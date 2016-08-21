package com.technostart.playmate.core.cv.field_detector;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.settings.Cfg;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

@SuppressWarnings("Duplicates")
public class TableDetector2 extends FieldDetector {
    @Cfg
    static int sigmaColor = 25;
    @Cfg
    static int sigmaSpace = 25;
    @Cfg
    static int ksize = 5;
    @Cfg
    static int areaCoef = 250;
    @Cfg
    static int edgesNumber = 4;
    @Cfg
    static int diameter = 5;
    @Cfg
    private int threshold = 80;
    @Cfg
    static public double minRatio = 0.85;
    @Cfg
    static public double maxRatio = 1.15;
    @Cfg
    static public double minAngle = -15.0;
    @Cfg
    static public double maxAngle = 15.0;
    @Cfg
    private double approxAngleThreshold = 200;

//    private List<MatOfPoint> contours;
//    private List<MatOfPoint> convexHullList;
//    private List<MatOfPoint> approxContours;
    private Mat processingFrame;
    private Mat structuredElement;
    private int minArea;

    public TableDetector2(Size frameSize) {
        super(frameSize);
        processingFrame = new Mat();
        structuredElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(ksize, ksize));
//        convexHullList = new ArrayList<>();
//        approxContours = new ArrayList<>();
        minArea = 0;
    }

    @Override
    public Mat getField(Mat inputFrame) {
        List<MatOfPoint> contours = new ArrayList<>();
        minArea = inputFrame.height() * inputFrame.width() / areaCoef;
        //предварительная обработка изображения фильтрами
        processingFrame = frameFilter(inputFrame, threshold);
        //поиск контуров на картинке
        Imgproc.findContours(processingFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        //фильтрация контуров
        contours = contourFilter(contours, minArea);
        //построение нового изображения
        Mat cntImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC1);
        List<MatOfPoint> convexHullList = convexHull(contours);
//        convexHullList = findTwoMatchingShapes(convexHullList);
        if (convexHullList != null) {
            List<MatOfPoint> approxContours = approximateContours(convexHullList, edgesNumber);
            print(cntImg, approxContours, -1, false);
//            print(cntImg, convexHullList, 3, false);
            convexHullList.clear();
        }
        return cntImg;
    }

    public Mat getFrame(Mat inputFrame) {
        Mat fieldMask = getField(inputFrame);
        Mat newFrame = new Mat();
        Core.addWeighted(inputFrame, 0.5, fieldMask, 0.5, 1, newFrame);
        return newFrame;
    }

    private List<MatOfPoint> convexHull(List<MatOfPoint> contours) {
        List<MatOfPoint> hullmop = new ArrayList<MatOfPoint>();
        for (MatOfPoint contour : contours) {
            hullmop.add(Utils.convexHull(contour));
        }
        return hullmop;
    }


    private List<MatOfPoint> approximateContours(List<MatOfPoint> convexHullList, int edgesNumber) {
        List<MatOfPoint> approxContours = new ArrayList<>();
        for (MatOfPoint convexHull : convexHullList) {
            MatOfPoint temp = new MatOfPoint();
            convexHull.convertTo(temp, CvType.CV_32S);
            //если сторон больше нужного количества, то аппроксимируем
            if (temp.rows() <= edgesNumber) {
                approxContours.add(temp);
            } else {
                List<Point> listOfPoints = Utils.approximate(temp, edgesNumber, approxAngleThreshold);
                temp.fromList(listOfPoints);
                approxContours.add(temp);
            }
        }
        return approxContours;
    }

    private Mat frameFilter(Mat inputFrame, int threshold) {
        Mat tempFrame = inputFrame;
        Mat processingFrame = new Mat();
        //обработка кадра различными фильтрами
        Imgproc.cvtColor(tempFrame, tempFrame, Imgproc.COLOR_BGR2GRAY);
        //bilateral фильтр лучше для краев
        Imgproc.bilateralFilter(tempFrame, processingFrame, diameter, sigmaColor, sigmaSpace);
        Imgproc.Canny(processingFrame, processingFrame, threshold, threshold * 3, 3, false);
        Imgproc.GaussianBlur(processingFrame, processingFrame, new org.opencv.core.Size(ksize, ksize), 3);
        Imgproc.morphologyEx(processingFrame, processingFrame, Imgproc.MORPH_OPEN, structuredElement, new Point(-1, -1), 1);
        return processingFrame;
    }

    private List<MatOfPoint> contourFilter(List<MatOfPoint> contours, int min_area) {
        Collections.sort(contours, new Comparator<Mat>() {
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
        int temp = contours.size();
        for (int i = 0; i < temp - counter; i++) {
            contours.remove(temp - 1 - i);
        }
        return contours;
    }

    private List<MatOfPoint> findTwoMatchingShapes(List<MatOfPoint> contours) {
        double matchingRatio = Double.MAX_VALUE;
        //отсев по аппроксимируемым контурам с помощью линий, которые должны быть близки к параллельности
        for (int i = 0; i < contours.size(); i++) {
            Mat line = new Mat();
            double[] angle = new double[2];
            Imgproc.fitLine(contours.get(i), line, 1, 0.0, 0.1, 0.1);
            for (int counter = 0; counter < line.rows(); counter++) {
                double[] array = line.get(counter, 0);
                if (counter < 2)
                    angle[counter] = array[0];
            }
            double tempAngle = Math.toDegrees(Math.atan(angle[1] / angle[0]));
            if (tempAngle > TableDetector.maxAngle || tempAngle < TableDetector.minAngle) {
                contours.remove(i);
                i--;
            }
        }
        //поиск похожих половин стола, с помощью отношения площади и функции opencv
        int indexOfFirstTableContour = -1;
        int indexOfSecondTableContour = -1;
        int counter = 0;
        for (int i = 0; i < contours.size() - 1 && counter == 0; i++) {
            for (int j = i + 1; j < contours.size(); j++) {
                double curMatchingRatio = Imgproc.matchShapes(contours.get(i), contours.get(j), 1, 0.0);
                if (curMatchingRatio < matchingRatio) {
                    double areaRatio = Math.abs(Imgproc.contourArea(contours.get(i)) / Imgproc.contourArea(contours.get(j)));
                    if (areaRatio > TableDetector.minRatio && areaRatio < TableDetector.maxRatio) {
                        matchingRatio = curMatchingRatio;
                        indexOfFirstTableContour = i;
                        indexOfSecondTableContour = j;
                        counter++;
                        break;
                    }
                }
            }
        }
        List<MatOfPoint> matchedContours = new LinkedList<>();
        if (indexOfFirstTableContour != -1 && indexOfSecondTableContour != -1) {
            matchedContours.add(contours.get(indexOfFirstTableContour));
            matchedContours.add(contours.get(indexOfSecondTableContour));
            return matchedContours;
        } else
            return null;
    }


    private Mat print(Mat cntImg, List<MatOfPoint> contours, int thickness, Boolean isRandomColor) {
        Scalar color = isRandomColor ? Palette.getNextColor() : Palette.WHITE;
        Imgproc.drawContours(cntImg, contours, -1 , color, thickness);
        return cntImg;
    }
}