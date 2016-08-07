package com.technostart.playmate.core.cv.tracker;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("WeakerAccess")
public class Tracker {
    // Параметры по умлочанию.
    public static final int DEFAULT_HISTORY_LENGTH = 5;
    public static final double DEFAULT_BG_THRESHOLD = 10;
    public static final int DEFAULT_BUFFER_LENGTH = 30;
    public static final float DEFAULT_SHADOW_THRESHOLD = 0.5f;
    public static final double DEFAULT_WEIGHT_THRESHOLD = 0.9;
    // Должна зависеть от размеров кадра.
    private double distThreshold;

    Size frameSize;

    // Параметры выделения фона.
    private Mat bgMask;
    private BackgroundSubtractorMOG2 bgSubstractor;
    private int historyLength;

    // Буфер для фона.
    private int bufferLength;
    private List<Mat> maskBuffer;

    //
    AtomicInteger groupId = new AtomicInteger();
    private Map<Integer, Group> groups;
    private double weightThreshold;
    private double maxDist;

    public Tracker(Size frameSize) {
        this(frameSize, DEFAULT_HISTORY_LENGTH, DEFAULT_BUFFER_LENGTH, DEFAULT_SHADOW_THRESHOLD);
    }

    public Tracker(Size frameSize, int historyLength, int bufferLength, float shadow_threshold) {
        this.frameSize = frameSize;

        // Вычисляем значения для нормализации весов по расстоянию.
        Point leftUp = new Point(0, 0);
        Point rightBottom = new Point(frameSize.width, frameSize.height);
        maxDist = Utils.getDistance(leftUp, rightBottom);
        distThreshold = maxDist / 4;
        weightThreshold = DEFAULT_WEIGHT_THRESHOLD;

        this.historyLength = historyLength;
        this.bufferLength = bufferLength;

        // Выделение фона.
        bgMask = new Mat();
        bgSubstractor = Video.createBackgroundSubtractorMOG2(historyLength, DEFAULT_BG_THRESHOLD, false);
        bgSubstractor.setShadowThreshold(shadow_threshold);
        // Находим тени но не отображаем их на маске.
        bgSubstractor.setShadowValue(0);
        maskBuffer = new ArrayList<>(bufferLength);

        groups = new HashMap<>();
    }

    public Mat getFrame(Mat inputFrame) {
        Utils.setResizeHeight((int) inputFrame.size().height);
        Utils.setResizeWidth((int) inputFrame.size().width);
        Mat frame = inputFrame;
        // Выделение фона.
        bgSubstractor.apply(frame, bgMask);
        // Шумодав.
        bgMask = Utils.filterNoise(bgMask);
        // Сохранение маски.
        if (maskBuffer.size() >= bufferLength) {
            maskBuffer.remove(0);
        }
        maskBuffer.add(bgMask);
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
                // TODO: Вычисление веса по форме/площади.
                double shapeWeight = 0;
                // TODO: Нормализация и суммирование.
                distWeight = 1 - dist / maxDist;
                double weight = distWeight + shapeWeight;
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
            updatedGroup.add(contoursList, weightList);
            groups.put(groupIdx, updatedGroup);
        }

        // Создание новых групп из оставшихся контуров.
        // FIXME: Тут надо рассматривать каждый контур как группу и сразу объединить их
        // FIXME: можно создать группы из каждого контура и потом посчитать веса для них же.
        for (Integer cntIdx : restContours) {
            Group newGroup = new Group(contours.get(cntIdx));
            groups.put(groupId.incrementAndGet(), newGroup);
        }

        /**
         * Композиция исходного изображения с данными трекера.
         */
        // Конвертируем исходное изображение в BGR для отрисовки цветных контуров.
        if (inputFrame.type() != CvType.CV_8UC3) {
            Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_GRAY2BGR);
        }
        // Матрица для отрисовки контуров, треков и т.д.
        Mat dataImg = Mat.zeros(frame.size(), CvType.CV_8UC3);

        // Рисуем группы контуров и треки разными цветами.
        for (Group group : groups.values()) {
            // Группы.
            List<MatOfPoint> contoursToDraw = group.getContourList();
            Imgproc.drawContours(dataImg, contoursToDraw, -1, Palette.getRandomColor(10), 1);
            // Треки.
            Utils.drawLine(group.getTrack(), dataImg, Palette.getRandomColor(10), 1);
        }

        Core.addWeighted(inputFrame, 0.3, dataImg, 0.7, 0.5, inputFrame);
        return inputFrame;
    }

    private int getNearestGroupIdx(MatOfPoint contour, List<Group> groups, List<Integer> groupsIdx) {
        Point contourCoord = Utils.getCentroid(contour);
        double minDist = distThreshold;
        int idx = -1;

        for (int i : groupsIdx) {
            Group group = groups.get(i);
            Point lastCoord = group.getLastCoord();
            double curDist = Utils.getDistance(contourCoord, lastCoord);
            if (curDist < minDist) {
                minDist = curDist;
                idx = i;
            }
        }
        return idx;
    }

    private int getNearestContourIdx(Group group, List<MatOfPoint> contours) {
        Point groupCoord = group.getLastCoord();
        double minDist = distThreshold;
        int idx = -1;

        for (int i = 0, size = contours.size(); i < size; i++) {
            MatOfPoint contour = contours.get(i);
            Point centroid = Utils.getCentroid(contour);
            double curDist = Utils.getDistance(groupCoord, centroid);
            if (curDist < minDist) {
                minDist = curDist;
                idx = i;
            }
        }
        return idx;
    }


}