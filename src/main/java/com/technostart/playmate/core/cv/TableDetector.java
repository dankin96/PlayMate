package com.technostart.playmate.core.cv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TableDetector {
    static final int sigmaColor = 25;
    static final int sigmaSpace = 25;
    static final int ksize = 5;
    static final int diameter = 5;
    private List<MatOfPoint> contours;
    private List<MatOfInt> hull;
    private List<MatOfPoint> hullmop;
    private List<MatOfPoint> approxContours;
    private Mat processingFrame;
    private Mat structeredElement;
    private int min_area;

    public TableDetector() {
        processingFrame = new Mat();
        structeredElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(ksize, ksize));
        contours = new ArrayList<MatOfPoint>();
        hull = new ArrayList<MatOfInt>();
        hullmop = new ArrayList<MatOfPoint>();
        approxContours = new ArrayList<MatOfPoint>();
        min_area = 0;
    }

    public Mat getTable(Mat inputFrame, int threshold, double approxCoef) {
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
        System.out.println("Counter = " + counter);
        //построение нового изображения
        Mat cntImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC3);
        convexHull(counter);
        approximation(counter, approxCoef);
        print(cntImg);
        Imgproc.resize(cntImg, cntImg, inputFrame.size());
        //добавление найденного контура к текущей картинке
        // Core.addWeighted(inputFrame, 0.5, cntImg, 0.5, 0, inputFrame);
        hull.clear();
        hullmop.clear();
        contours.clear();
        approxContours.clear();
        return cntImg;
//        return processingFrame;
    }

    private void convexHull(int counter) {
        for (int i = 0; i < counter; i++) {
            hull.add(new MatOfInt());
        }
        for (int i = 0; i < counter; i++) {
            Imgproc.convexHull(contours.get(i), hull.get(i));
        }
        List<Point[]> hullpoints = new ArrayList<Point[]>();
        for (int i = 0; i < hull.size(); i++) {
            Point[] points = new Point[hull.get(i).rows()];
            for (int j = 0; j < hull.get(i).rows(); j++) {
                int index = (int) hull.get(i).get(j, 0)[0];
                points[j] = new Point(contours.get(i).get(index, 0)[0], contours.get(i).get(index, 0)[1]);
            }
            hullpoints.add(points);
        }
        for (int i = 0; i < hullpoints.size(); i++) {
            MatOfPoint mop = new MatOfPoint();
            mop.fromArray(hullpoints.get(i));
            hullmop.add(mop);
        }
    }

    private void approximation(int counter, double approxCoef) {
        MatOfPoint2f approx = new MatOfPoint2f();
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        approxContours = hullmop;
        for (int i = 0; i < counter; i++) {
            hullmop.get(i).convertTo(approx, CvType.CV_32FC2);
            double approxDistance = Imgproc.arcLength(approx, true) * approxCoef;
            Imgproc.approxPolyDP(approx, approxCurve, approxDistance, true);
            approxCurve.convertTo(approxContours.get(i), CvType.CV_32S);
        }
    }

    private void frameFilter(Mat inputFrame, int threshold) {
        Mat tempFrame = inputFrame;
        //обработка кадра различными фильтрами
        Imgproc.cvtColor(tempFrame, tempFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.bilateralFilter(tempFrame, processingFrame, diameter, sigmaColor, sigmaSpace); //фильтр лучше для краев
        Utils.filterNoise(processingFrame);
        Imgproc.Canny(processingFrame, processingFrame, threshold, threshold * 3, 3, false);
        Imgproc.GaussianBlur(processingFrame, processingFrame, new org.opencv.core.Size(ksize, ksize), 3);
        Imgproc.morphologyEx(processingFrame, processingFrame, Imgproc.MORPH_OPEN, structeredElement, new Point(-1, -1), 1);
    }

    private Mat print(Mat cntImg) {
        for (int i = 0; i < approxContours.size(); i++) {
            Imgproc.drawContours(cntImg, approxContours, i, Palette.getNextColor(), -1);
            Imgproc.drawContours(cntImg, hullmop, i, Palette.BLACK, 1);
//            Imgproc.drawContours(cntImg, contours, i, Palette.BLACK, 1);
        }
        return cntImg;
    }
}
