package com.technostart.playmate.core.cv;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

public class Tracker {
    // Параметры по умлочанию.
    public static final int DEFAULT_HISTORY_LENGTH = 5;
    public static final double DEFAULT_THRESHOLD = 0;
    public static final int DEFAULT_BUFFER_LENGTH = 30;
    public static final float DEFAULT_SHADOW_THRESHOLD = 0.5f;

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
    private List<Point> track;

    public Tracker() {
        this(DEFAULT_HISTORY_LENGTH, DEFAULT_BUFFER_LENGTH, DEFAULT_SHADOW_THRESHOLD);
    }

    public Tracker(int historyLength, int bufferLength, float shadow_threshold) {
        this.historyLength = historyLength;
        this.bufferLength = bufferLength;

        this.bgMask = new Mat();
        this.bgSubstractor =
                Video.createBackgroundSubtractorMOG2(historyLength, DEFAULT_THRESHOLD, true);
        this.bgSubstractor.setShadowThreshold(shadow_threshold);
        // Находим тени но не отображаем их на маске.
        this.bgSubstractor.setShadowValue(0);
        this.maskBuffer = new ArrayList<>(bufferLength);

        this.contoursBuffer = new ArrayList<>(bufferLength);

        this.track = new ArrayList<>();
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

        // TODO Поиск ближайших контуров
        // TODO Поиск контуров похожих по цвету

        // TODO Восстановление траектории по контурам

        // Композиция исходного изображения с данными трекера.
        Mat cntImg = Mat.zeros(frame.size(), frame.type());
        List<MatOfPoint> cnts = new ArrayList<>();
        for (List<MatOfPoint> curCnts : contoursBuffer) {
            cnts.addAll(curCnts);
        }
        Imgproc.drawContours(cntImg, cnts, -1, new Scalar(0, 255, 0), 1);
        Imgproc.resize(cntImg, cntImg, inputFrame.size());
        Core.addWeighted(inputFrame, 0.5, cntImg, 0.5, 0, inputFrame);
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


}
