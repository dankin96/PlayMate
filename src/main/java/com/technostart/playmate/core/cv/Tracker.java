package com.technostart.playmate.core.cv;

//import org.opencv.core.*;
import com.google.common.collect.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import sun.security.action.PutAllAction;

import java.util.*;

public class Tracker {
    // Параметры по умлочанию.
    public static final int DEFAULT_HISTORY_LENGTH = 5;
    public static final double DEFAULT_THRESHOLD = 20;
    public static final int DEFAULT_BUFFER_LENGTH = 30;
    public static final float DEFAULT_SHADOW_THRESHOLD = 0.5f;
    // TODO: должно зависеть от размеров кадра
    private static final double DIST_THRESHOLD = 2000;

    // Параметры выделения фона.
    private Mat bgMask;
    private BackgroundSubtractorMOG2 bgSubstractor;
    private int historyLength;

    // Буфер для фона.
    private int bufferLength;
    private List<Mat> maskBuffer;

    // Буфер контуров.
    private List<List<MatOfPoint>> contoursBuffer;

    //
//    Map<Group, List<Point>> groupList;
    ArrayList<Group> groups;

    public Tracker() {
        this(DEFAULT_HISTORY_LENGTH, DEFAULT_BUFFER_LENGTH, DEFAULT_SHADOW_THRESHOLD);
    }

    public Tracker(int historyLength, int bufferLength, float shadow_threshold) {
        this.historyLength = historyLength;
        this.bufferLength = bufferLength;

        this.bgMask = new Mat();
        this.bgSubstractor =
                Video.createBackgroundSubtractorMOG2(historyLength, DEFAULT_THRESHOLD, false);
        this.bgSubstractor.setShadowThreshold(shadow_threshold);
        // Находим тени но не отображаем их на маске.
        this.bgSubstractor.setShadowValue(0);
        this.maskBuffer = new ArrayList<>(bufferLength);

        this.contoursBuffer = new ArrayList<>(bufferLength);

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
            // Координаты.
            Point centroid = Utils.getCentroid(contour);
            lastCoord = centroid;
            track.add(centroid);

            idle = idle > 0 ? idle - 1: 0;
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
        // Resize.
        Mat frame = Utils.resizeIn(inputFrame);
        // TODO Правка геометрии
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
        // Сохранение контуров.
        /*if (contoursBuffer.size() >= bufferLength) {
            contoursBuffer.remove(0);
        }
        contoursBuffer.add(contours);*/

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

        // Поиск ближайших контуров.
        Map<Integer, List<Integer>> cntIdxToGroupIdx = new HashMap<>();

        // Поиск ближайшего к группе контура.
        for (int i = 0, groupsSize = groups.size(); i < groupsSize; i++) {
            Group group = groups.get(i);
            int contourIdx;
            contourIdx = getNearestContourIdx(group, contours);
            if (contourIdx >= 0) {
                List<Integer> groupsList = cntIdxToGroupIdx.get(contourIdx);
                if (groupsList == null) groupsList = new ArrayList<>();
                groupsList.add(i);
                cntIdxToGroupIdx.put(contourIdx, groupsList);
            } else {
                // Не нашлось контура для добавления.
            }
        }

        Set<Integer> addedContoursIdx = new HashSet<>();

        // Поиск ближайшей к контуру группы.
        for (Integer contourIdx : cntIdxToGroupIdx.keySet()) {
            int groupIdx;
            MatOfPoint contour = contours.get(contourIdx);
            List<Integer> groupsIdxList = cntIdxToGroupIdx.get(contourIdx);
            groupIdx = getNearestGroupIdx(contour, groups, groupsIdxList);
            if (groupIdx >= 0) {
                // Добавляем контур в группу.
                Group updatedGroup = groups.get(groupIdx);
                updatedGroup.add(contour);
                groups.set(groupIdx, updatedGroup);
                // Отмечаем добавленный контур.
                addedContoursIdx.add(contourIdx);
            } else {
                System.err.println("Find group index error!");
            }
        }

        // TODO Поиск контуров похожих по цвету

        // Создаем новые группы из оставшихся контуров.
        for (int i = 0, size = contours.size(); i < size; i++) {
            if (addedContoursIdx.contains(i)) {
                addedContoursIdx.remove(i);
            } else {
                MatOfPoint contour = contours.get(i);
                Group newGroup = new Group(contour);
                groups.add(newGroup);
            }
        }

        // TODO Восстановление траектории по контурам

        /**
         * Композиция исходного изображения с данными трекера.
         */
        // Конвертируем исходное изображение в BGR для отрисовки цветных контуров.
        if (inputFrame.type() != CvType.CV_8UC3) {
            Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_GRAY2BGR);
        }
        // Матрица для отрисовки контуров, треков и т.д.
        Mat dataImg = Mat.zeros(frame.size(), CvType.CV_8UC3);

        // Рисуем группы контуров разными цветами.
        for (Group group : groups) {
            List<MatOfPoint> contoursToDraw = group.getContourList();
            Imgproc.drawContours(dataImg, contoursToDraw, -1, Palette.getNextColor(), 2);
        }

        // Рисуем треки.
        for (Group group : groups) {
            // Utils.drawLine(group.getTrack(), dataImg, Palette.getNextColor(), 3);
        }
        Imgproc.resize(dataImg, dataImg, inputFrame.size());
        Core.addWeighted(inputFrame, 0.5, dataImg, 0.5, 0, inputFrame);
        return inputFrame;
    }

    private int getNearestGroupIdx(MatOfPoint contour, List<Group> groups, List<Integer> groupsIdx) {
        Point contourCoord = Utils.getCentroid(contour);
        double minDist = DIST_THRESHOLD;
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


    private int getNearestGroupIdx(MatOfPoint contour, List<Group> groups) {
        List<Integer> groupsIdx = ContiguousSet
                .create(com.google.common.collect.Range.closed(0, groups.size()), DiscreteDomain.integers()).asList();
        return getNearestGroupIdx(contour, groups, groupsIdx);
    }

    private int getNearestContourIdx(Group group, List<MatOfPoint> contours) {
        Point groupCoord = group.getLastCoord();
        double minDist = DIST_THRESHOLD;
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
