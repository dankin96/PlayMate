package com.technostart.playmate.core.cv.field_detector;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.cv.settings.Cfg;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TableDetector extends FieldDetector {
    @Cfg
    static int sigmaColor = 25;
    @Cfg
    static int sigmaSpace = 25;
    @Cfg
    static int ksize = 5;
    @Cfg
    static Integer diameter = 5;
    private List<MatOfPoint> contours;
    private List<MatOfInt> hull;
    private List<MatOfPoint> hullmop;
    private List<MatOfPoint> approxContours;
    private Mat processingFrame;
    private Mat structeredElement;
    private int min_area;
    @Cfg
    private int threshold = 100;
    @Cfg
    private double approxCoef = 0.01;

    public TableDetector(Size frameSize) {
        super(frameSize);
        processingFrame = new Mat();
        structeredElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(ksize, ksize));
        contours = new ArrayList<MatOfPoint>();
        hull = new ArrayList<MatOfInt>();
        hullmop = new ArrayList<MatOfPoint>();
        approxContours = new ArrayList<MatOfPoint>();
        min_area = 0;
    }

    @Override
    public Mat getField(Mat frame) {
        // TODO: тут вся обработка без отрисовки.
        // возвращает маску стола
        return null;
    }

    public Mat getFrame(Mat inputFrame) {
        getField(inputFrame);
        min_area = inputFrame.height() * inputFrame.width() / 1000;
        //предварительная обработка изображения фильтрами
        frameFilter(inputFrame, threshold);
        //поиск контуров и их сортировка
        Imgproc.findContours(processingFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Collections.sort(this.contours, new Comparator<Mat>() {
            @Override
            public int compare(Mat o1, Mat o2) {
                double area_1 = Imgproc.contourArea(o1);
                double area_2 = Imgproc.contourArea(o2);
                if (area_1 < area_2) {
                    return 1;
                } else if (area_1 > area_2) {
                    return -1;
                }
                return 0;
            }
        });
        //фильтрация по площади
        int counter = 0;
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area < min_area) {
                counter = i + 1;
                break;
            }
        }
        counter = 3;
        System.out.println("Counter = " + counter);
        //построение нового изображения
        Mat cntImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC3);
        convexHull(counter);
        approximation(counter, approxCoef);
        print(cntImg, counter);
//        cntImg = lineSegmentDetect(cntImg);
//        Imgproc.resize(cntImg, cntImg, inputFrame.size());
//        добавление найденного контура к текущей картинке
        // Core.addWeighted(inputFrame, 0.5, cntImg, 0.5, 0, inputFrame);
        hull.clear();
        hullmop.clear();
        contours.clear();
        approxContours.clear();
        return cntImg;
    }

    private void convexHull(int counter) {
        for (int i = 0; i < counter; i++) {
            hullmop.add(Utils.convexHull(contours.get(i)));
        }
    }

    private void approximation(int counter, double approxCoef) {
        MatOfPoint2f approx = new MatOfPoint2f();
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        for (int i = 0; i < counter; i++) {
            MatOfPoint temp = new MatOfPoint();
            hullmop.get(i).convertTo(approx, CvType.CV_32FC2);
            double approxDistance = Imgproc.arcLength(approx, true) * approxCoef;
            Imgproc.approxPolyDP(approx, approxCurve, approxDistance, true);
            approxCurve.convertTo(temp, CvType.CV_32S);
            approxContours.add(temp);
        }
    }

    private void frameFilter(Mat inputFrame, int threshold) {
        Mat tempFrame = inputFrame;
        //обработка кадра различными фильтрами
        Imgproc.cvtColor(tempFrame, tempFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.bilateralFilter(tempFrame, processingFrame, diameter, sigmaColor, sigmaSpace); //фильтр лучше для краев
        Imgproc.Canny(processingFrame, processingFrame, threshold, threshold * 3, 3, false);
        Imgproc.GaussianBlur(processingFrame, processingFrame, new org.opencv.core.Size(ksize, ksize), 3);
        Imgproc.morphologyEx(processingFrame, processingFrame, Imgproc.MORPH_OPEN, structeredElement, new Point(-1, -1), 1);
    }

    private Mat lineSegmentDetect(Mat inputFrame) {
        Mat lines = new Mat();
        if (inputFrame.type() != CvType.CV_8UC1) {
            Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_BGR2GRAY);
        }
        int minLineLength = 20;
        int maxLineGap = 700;
        Imgproc.HoughLinesP(inputFrame, lines, 1, Math.PI / 720, 100, minLineLength, maxLineGap);
        Mat linesImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC3);
        for (int x = 0; x < lines.rows(); x++) {
            double[] vec = lines.get(x, 0);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            Imgproc.line(linesImg, start, end, Palette.getRandomColor(), 3);
        }
        System.out.println("lines = " + lines.rows());
        return linesImg;
    }

    private Mat print(Mat cntImg, int counter) {
        for (int i = 0; i < counter; i++) {
            Imgproc.drawContours(cntImg, approxContours, i, Palette.getNextColor(), -1);
            Imgproc.drawContours(cntImg, hullmop, i, Palette.WHITE, 3);
            Imgproc.drawContours(cntImg, contours, i, Palette.GREEN, 3);
        }
//        Imgproc.drawContours(cntImg, hullmop, 1, Palette.WHITE, 2);
        System.out.println("\nsize contours = " + contours.size());
        System.out.println("\nsize hull = " + hullmop.size());
        System.out.println("\nsize approxcontours = " + approxContours.size());
        return cntImg;
    }
}