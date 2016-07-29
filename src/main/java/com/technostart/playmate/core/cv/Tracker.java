package com.technostart.playmate.core.cv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class Tracker {
    // Параметры по умлочанию.
    public static final int DEFAULT_HISTORY_LENGTH = 5;
    public static final double DEFAULT_BG_THRESHOLD = 20;
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
    private ArrayList<Group> groups;
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

        groups = new ArrayList<>();
    }

    private class Group {
        private static final int COLOR_NUMBER = 3;
        private Scalar medianColor;
        private Point lastCoord;

        // Кол-во итераций без добавления новых элементов.
        private int idle;
        // Максимальное кол-во итераций простоя.
        public static final int MAX_IDLE = 3;

        private List<Scalar> colors;
        private List<MatOfPoint> contours;
        private List<Point> track;

        //

        public Group(Scalar medianColor, Point lastCoord) {
            init();
            this.medianColor = medianColor;
            this.lastCoord = lastCoord;
        }

        public Group(MatOfPoint contour) {
            init();
            add(contour);
        }

        private void init() {
            track = new ArrayList<>();
            contours = new ArrayList<>();
        }

        public void add(MatOfPoint contour) {
            contours.add(contour);
            Point centroid = Utils.getCentroid(contour);
            add(centroid);
        }

        public void add(List<MatOfPoint> newContours) {
            int size = newContours.size();
            contours.addAll(newContours);
            Point centroid = null;
            if (size == 0) {
                return;
            } else if (size == 1) {
                add(newContours.get(0));
                return;
            } else if (size == 2) {
                Point c1 = Utils.getCentroid(newContours.get(0));
                Point c2 = Utils.getCentroid(newContours.get(1));
                centroid = new Point((c1.x + c2.x) / 2, (c1.y + c2.y) / 2);
            } else if (size > 2) {
                // TODO: Центроид с весами.
                centroid = Utils.getContoursCentroid(newContours);
            }

            if (centroid != null) {
                add(centroid);
            }
        }

        private void add(Point centroid) {
            lastCoord = centroid;
            track.add(centroid);

            idle = idle > 0 ? idle - 1 : 0;
        }

        public Point getLastCoord() {
            return lastCoord;
        }

        public MatOfPoint getTrackContour() {
            MatOfPoint contour = new MatOfPoint();
            contour.fromList(track);
            return contour;
        }

        public List<Point> getTrack() {
            return track;
        }

        public List<MatOfPoint> getContourList() {
            return contours;
        }

        public void idle() {
            idle++;
        }
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

        // Чистка групп.
        ArrayList<Group> updatedGroups = new ArrayList<>();
        for (int i = 0, size = groups.size(); i < size; i++) {
            Group group = groups.get(i);
            if (group.idle < Group.MAX_IDLE) {
                group.idle();
                groups.set(i, group);
                updatedGroups.add(group);
            }
        }
        groups = updatedGroups;

        // Структура для хранения весов Map<contourIdx, Map<groupIdx, weight>>
        Map<Integer, Map<Integer, Double>> contoursWeight = new HashMap<>();

        for (int cntIdx = 0, cntSize = contours.size(); cntIdx < cntSize; cntIdx++) {
            MatOfPoint curContour = contours.get(cntIdx);
            Map<Integer, Double> groupIdxToWeight = new HashMap<>();
            for (int groupIdx = 0, groupSize = groups.size(); groupIdx < groupSize; groupIdx++) {
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
            } else {
                restContours.add(cntIdx);
            }
        }

        // Заполняем группы.
        for (Map.Entry<Integer, List<MatOfPoint>> entry : groupIdxToCntList.entrySet()) {
            int groupIdx = entry.getKey();
            List<MatOfPoint> contoursList = entry.getValue();
            Group updatedGroup = groups.get(groupIdx);
            updatedGroup.add(contoursList);
            groups.set(groupIdx, updatedGroup);
        }

        // Создание новых групп из оставшихся контуров.
        // FIXME: Тут надо рассматривать каждый контур как группу и сразу объединить их
        // FIXME: можно создать группы из каждого контура и потом посчитать веса для них же.
        for (Integer cntIdx : restContours) {
            Group newGroup = new Group(contours.get(cntIdx));
            groups.add(newGroup);
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
        for (Group group : groups) {
            // Группы.
            List<MatOfPoint> contoursToDraw = group.getContourList();
            Imgproc.drawContours(dataImg, contoursToDraw, -1, Palette.getNextColor(), 1);
            // Треки.
            Utils.drawLine(group.getTrack(), dataImg, Palette.getNextColor(), 1);
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
