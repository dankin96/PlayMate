package com.technostart.playmate.core.cv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

public class Tracker {
    // Параметры по умлочанию.
    public static final int DEFAULT_HISTORY_LENGTH = 5;
    public static final double DEFAULT_THRESHOLD = 20;
    public static final int DEFAULT_BUFFER_LENGTH = 30;
    public static final float DEFAULT_SHADOW_THRESHOLD = 0.5f;
    private static final double DIST_THRESHOLD = 10;

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

    public class Group {
        private static final int COLOR_NUMBER = 3;
        private Scalar medianColor;
        private Point lastCoord;

        private List<Scalar> colors;
        private List<Scalar> contours;
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
            // Координаты.
            Point centroid = Utils.getCentroid(contour);
            lastCoord = centroid;
            track.add(centroid);
        }

        public MatOfPoint getTrackContour() {
            MatOfPoint contour = new MatOfPoint();
            contour.fromList(track);
            return contour;
        }

        public List<Point> getTrack() {
            return track;
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
        // Выделение и сохранение контуров.
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(bgMask.clone(), contours, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contoursBuffer.size() >= bufferLength) {
            contoursBuffer.remove(0);
        }
        contoursBuffer.add(contours);

        // TODO Чистка групп.
        // TODO Поиск ближайших контуров
        for (MatOfPoint contour : contours) {
            // findGroup(contour);
        }
        // TODO Поиск контуров похожих по цвету

        // TODO Восстановление траектории по контурам

        // Композиция исходного изображения с данными трекера.
        if (inputFrame.type() != CvType.CV_8UC3) {
            Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_GRAY2BGR);
        }
        Mat dataImg = Mat.zeros(frame.size(), CvType.CV_8UC3);
        List<MatOfPoint> cnts = new ArrayList<>();
        for (List<MatOfPoint> curCnts : contoursBuffer) {
            cnts.addAll(curCnts);
        }
        Imgproc.drawContours(dataImg, cnts, -1, new Scalar(0, 255, 0), 1);
        // TODO Рисуем треки.
        Imgproc.resize(dataImg, dataImg, inputFrame.size());
        Core.addWeighted(inputFrame, 0.5, dataImg, 0.5, 0, inputFrame);
        return inputFrame;
    }

    private void findSimilarContours(Mat inputFrame) {
        int bufferSize = contoursBuffer.size();
        if (bufferSize > 1) {
            List<MatOfPoint> prevContours = contoursBuffer.get(bufferSize - 1);
            for (MatOfPoint cnt : prevContours) {
                MatOfPoint curCnt = Utils.findSimilarByColor(cnt, prevContours, inputFrame);
                Point p1 = Utils.getCentroid(cnt);
                Point p2 = Utils.getCentroid(curCnt);
                Imgproc.line(inputFrame, p1, p2, new Scalar(0, 0, 255));
            }
        }
    }

    private void findGroup(MatOfPoint contour) {
        Point center = Utils.getCentroid(contour);
        int groupId = 0;
        boolean isInit = false;
        double minDist = DIST_THRESHOLD;

        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);
            double curDist = Utils.getDistance(center, group.lastCoord);
            if (curDist >= DIST_THRESHOLD) continue;
            if (!isInit) {
                minDist = curDist;
                isInit = true;
            }

            if (curDist < minDist) {
                minDist = curDist;
                groupId = i;
            }
        }

        if (isInit) {
            // Добавляем контур в группу.
            Group updatedGroup = groups.get(groupId);
            updatedGroup.add(contour);
            groups.set(groupId, updatedGroup);
        } else {
            Group newGroup = new Group(contour);
            groups.add(newGroup);
        }
    }


}
