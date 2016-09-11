package com.technostart.playmate.core.cv.tracker;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.cv.background_subtractor.BackgroundExtractor;
import com.technostart.playmate.core.cv.background_subtractor.BgSubtractorFactory;
import com.technostart.playmate.core.settings.Cfg;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("WeakerAccess")
public class Tracker {
    // Параметры по умлочанию.
    public static final double DEFAULT_WEIGHT_THRESHOLD = 0.9;

    Size frameSize;

    // Параметры выделения фона.
    private Mat bgMask;
    private BackgroundExtractor bgSubtractor;

    //
    AtomicInteger groupId = new AtomicInteger();
    private Map<Integer, Group> groups;

    // Listeners.
    private HitDetectorInterface hitDetectorListener = (hitPoint, direction) -> {};
    private RawTrackerInterface contourListener = (groupId, contours) -> {};

    //
    // Максимальное кол-во контуров которые можно добавить в группу за раз.
    private int maxContourNumber;
    private double maxDist;

    ///////////////////////////////////////////////////////////////////////////
    // Пороги весов.
    ///////////////////////////////////////////////////////////////////////////

    @Cfg
    private double weightThreshold;
    @Cfg
    // Максимальное расстояния между последней точкой трека и текущим контуром
    // больше которого нельзя добавлять контуры в группу
    // (доля от максимального расстояния 0..1).
    private double distThreshold = 0.05;
    @Cfg
    // Максимальное кол-во раз в которое может отличаться текущее расстояние между
    // контуром и последней точкой трека. При большем значении
    // контур не добавляется в группу.
    // Принимает значения больше 1 включительно.
    private double maxAvgDistRate = 2;

    @Cfg
    // Максимальное кол-во раз в которое может отличаться площадь текущего контура
    // от средней по группе. При большем значении контур не добавляется в группу.
    // Принимает значения больше 1 включительно.
    private double maxAreaRate = 2.5;

    public Tracker(Size frameSize) {
        this(frameSize, BgSubtractorFactory.createSimpleBS());
    }

    public Tracker(Size frameSize, BackgroundExtractor bgSubtractor) {
        this.frameSize = frameSize;

        // Вычисляем значения для нормализации весов по расстоянию.
        Point leftUp = new Point(0, 0);
        Point rightBottom = new Point(frameSize.width, frameSize.height);
        maxDist = Utils.getDistance(leftUp, rightBottom);
        weightThreshold = DEFAULT_WEIGHT_THRESHOLD;

        // Выделение фона.
        bgMask = new Mat();
        this.bgSubtractor = bgSubtractor;

        groups = new HashMap<>();
    }

    public void setHitDetectorListener(HitDetectorInterface hitDetectorListener) {
        this.hitDetectorListener = hitDetectorListener;
    }

    public void setContourListener(RawTrackerInterface contourListener) {
        this.contourListener = contourListener;
    }


    public void process(long timestamp, Mat inputFrame) {
        Utils.setResizeHeight((int) inputFrame.size().height);
        Utils.setResizeWidth((int) inputFrame.size().width);
        // Выделение фона.
        bgSubtractor.apply(inputFrame, bgMask);
        // Шумодав.
        bgMask = Utils.filterNoise(bgMask);

        // Выделение контуров.
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(bgMask.clone(), contours, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Находим группы для удаления.
        List<Integer> groupIdToRemove = new ArrayList<>();
        for (Integer groupIdx : groups.keySet()) {
            Group group = groups.get(groupIdx);
            if (group.getIdle() > Group.MAX_IDLE) {
                groupIdToRemove.add(groupIdx);
            }
            group.idle();
        }

        // Удаляем группы.
        for (int id : groupIdToRemove) {
            groups.remove(id);
        }

        // Структура для хранения весов Map<contourIdx, Map<groupIdx, weight>>
        Map<Integer, Map<Integer, Double>> contoursWeight = new HashMap<>();

        for (int cntIdx = 0, cntSize = contours.size(); cntIdx < cntSize; cntIdx++) {
            MatOfPoint curContour = contours.get(cntIdx);
            Map<Integer, Double> groupIdxToWeight = new HashMap<>();
            for (int groupIdx : groups.keySet()) {
                // TODO: Вычисление веса по расстоянию.
                double distWeight = 0;
                Group curGroup = groups.get(groupIdx);
                Point groupPoint = curGroup.getLastCoord();
                Point cntPoint = Utils.getCentroid(curContour);
                double dist = Utils.getDistance(groupPoint, cntPoint);
                double normalDist = dist / maxDist;
                if (normalDist > distThreshold) continue;
                // TODO: Вычисление веса по среднему расстоянию.
                double curGroupAvgDist = curGroup.getAvgDist();
                if (curGroupAvgDist != 0) {
                    double avgDistRate = curGroupAvgDist / dist;
                    if (avgDistRate < 1) avgDistRate = 1 / avgDistRate;
                    if (avgDistRate > maxAvgDistRate) continue;
                }
                // TODO: Вычисление веса по площади.
                double areaRate = curGroup.getAvgArea() / Imgproc.contourArea(curContour);
                if (areaRate < 1) areaRate = 1 / areaRate;
                if (areaRate > maxAreaRate) continue;
                // TODO: Нормализация и суммирование.
                distWeight = 1 - normalDist;
                double weight = distWeight;
                // Сохранение веса.
                groupIdxToWeight.put(groupIdx, weight);
            }
            contoursWeight.put(cntIdx, groupIdxToWeight);
        }

        // Выбор групп по весам.
        Map<Integer, List<MatOfPoint>> groupIdxToCntList = new HashMap<>();
        Map<Integer, List<Double>> groupIdxToWeightList = new HashMap<>();
        List<Integer> restContours = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, Double>> entry : contoursWeight.entrySet()) {
            Integer cntIdx = entry.getKey();
            Map<Integer, Double> groupIdxToWeight = entry.getValue();
            // Поиск индекса группы с максимальным весом.
            int groupIdx = -1;
            double maxWeight = 0;
            for (Map.Entry<Integer, Double> groupIdxToWeightEntry : groupIdxToWeight.entrySet()) {
                int curGroupIdx = groupIdxToWeightEntry.getKey();
                double curWeight = groupIdxToWeightEntry.getValue();
                if (maxWeight < curWeight) {
                    maxWeight = curWeight;
                    groupIdx = curGroupIdx;
                }
            }
            // Отсев контуров по порогу.
            if (weightThreshold <= maxWeight && 0 < groupIdx) {
                // Добавляем контур в список группы.
                List<MatOfPoint> cntList = groupIdxToCntList.get(groupIdx);
                if (cntList == null) {
                    cntList = new ArrayList<>();
                }
                cntList.add(contours.get(cntIdx));
                groupIdxToCntList.put(groupIdx, cntList);
                // Добавляем вес в список группы.
                List<Double> weightList = groupIdxToWeightList.get(groupIdx);
                if (weightList == null) {
                    weightList = new ArrayList<>();
                }
                weightList.add(maxWeight);
                groupIdxToWeightList.put(groupIdx, weightList);
            } else {
                restContours.add(cntIdx);
            }
        }

        // Заполняем группы.
        for (Map.Entry<Integer, List<MatOfPoint>> entry : groupIdxToCntList.entrySet()) {
            int groupIdx = entry.getKey();
            List<MatOfPoint> contoursList = entry.getValue();
            List<Double> weightList = groupIdxToWeightList.get(groupIdx);
            Group updatedGroup = groups.get(groupIdx);
            updatedGroup.add(timestamp, contoursList, weightList);
            groups.put(groupIdx, updatedGroup);
            // Сообщаем о новых данных.
            contourListener.onTrackContour(groupIdx, contoursList);
        }

        // Создание новых групп из оставшихся контуров.
        // FIXME: Тут надо рассматривать каждый контур как группу и сразу объединить их
        // FIXME: можно создать группы из каждого контура и потом посчитать веса для них же.
        for (Integer cntIdx : restContours) {
            MatOfPoint contour = contours.get(cntIdx);
            Group newGroup = new Group(timestamp, contour, hitDetectorListener);
            int newGroupId = groupId.incrementAndGet();
            groups.put(newGroupId, newGroup);
            // Сообщаем о новых данных.
            contourListener.onTrackContour(newGroupId, Arrays.asList(contour));
        }
    }

    public Mat getFrame(long timestamp, Mat inputFrame, List<Long> timestamps) {
        process(timestamp, inputFrame);
        /**
         * Композиция исходного изображения с данными трекера.
         */
        // Конвертируем исходное изображение в BGR для отрисовки цветных контуров.
        if (inputFrame.type() == CvType.CV_8UC1) {
            Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_GRAY2BGR);
        }
        // Матрица для отрисовки контуров, треков и т.д.
        Mat dataImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC3);

        // Рисуем группы контуров и треки разными цветами.
        for (Group group : groups.values()) {
            if (group.getSize() <= 2) continue;
            // Контуры.
            List<MatOfPoint> contoursToDraw = group.getContoursByTimestamp(timestamps);
            Imgproc.drawContours(dataImg, contoursToDraw, -1, Palette.getRandomColor(10), 1);
            // Треки.
            List<Point> trackPoints = group.getTrackPointsByTimestamp(timestamps);
            Utils.drawLine(trackPoints, dataImg, Palette.getRandomColor(10), 1);
        }

        Core.addWeighted(inputFrame, 0.5, dataImg, 0.5, 0.5, inputFrame);
        return inputFrame;
    }

    public void setBgSubstr(BackgroundExtractor newBgExtr) {
        bgSubtractor = newBgExtr;
    }


}
