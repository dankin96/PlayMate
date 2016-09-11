package com.technostart.playmate.core.cv.field_detector;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.settings.Cfg;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

@SuppressWarnings("Duplicates")
public class TableDetector extends FieldDetector {
    @Cfg
    static int sigmaColor = 101;
    @Cfg
    static int sigmaSpace = 101;
    @Cfg
    int ksize = 5;
    @Cfg
    int areaCoef = 250;
    @Cfg
    int edgesNumber = 10;
    @Cfg
    int diameter = 5;
    @Cfg
    int threshold = 80;
    @Cfg
    double minRatio = 0.85;
    @Cfg
    double maxRatio = 1.15;
    @Cfg
    double minAngle = -15.0;
    @Cfg
    double maxAngle = 15.0;
    @Cfg
    static int blurSigmaX = 3;
    @Cfg
    static int multiplierThreshold = 3;

    static int cannyApertureSize = 3;
    private double approxAngleThreshold = 200;

    private Mat processingFrame;
    private Mat structuredElement;
    private int minArea;
    private Boolean isDetected = false;

    public TableDetector(Size frameSize) {
        super(frameSize);
        processingFrame = new Mat();
        structuredElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(ksize, ksize));
        minArea = 0;
    }

    @Override
    public Mat getField(Mat inputFrame) {

        List<MatOfPoint> convexHull = getContours(inputFrame);
        Mat mask = Mat.zeros(inputFrame.size(), CvType.CV_8UC1);
        if (convexHull != null) {
            Imgproc.drawContours(mask, convexHull, -1, Palette.WHITE, -1);
        }
        return mask;
    }

    @Override
    public List<MatOfPoint> getContours(Mat inputFrame) {
        minArea = inputFrame.height() * inputFrame.width() / areaCoef;
        //предварительная обработка изображения фильтрами
        processingFrame = frameFilter(inputFrame, threshold);
        //поиск контуров на картинке
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(processingFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        //фильтрация контуров
        contours = filterContoursByArea(contours, minArea, Double.MAX_VALUE);
        contours = filterContoursByAngle(contours, minAngle, maxAngle);
        //построение нового изображения
        List<MatOfPoint> convexHull = convexHull(contours);
        convexHull = findTwoMatchingShapes(convexHull);

        if (convexHull != null) {
            convexHull = approximateContours(convexHull, edgesNumber);
        }
        return convexHull;
    }


    public Mat getFrame(Mat inputFrame) {
        // todo
        return inputFrame;
    }

   /* public List<MatOfPoint> getPointsOfTable() {
        return approxContours;
    }*/

    private List<MatOfPoint> convexHull(List<MatOfPoint> contours) {
        List<MatOfPoint> convexHulls = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            convexHulls.add(Utils.convexHull(contour));
        }
        return convexHulls;
    }


    private List<MatOfPoint> approximateContours(List<MatOfPoint> convexHull, int edgesNumber) {
        List<MatOfPoint> approxContours = new ArrayList<>();
        for (int i = 0; i < convexHull.size(); i++) {
            MatOfPoint temp = new MatOfPoint();
            convexHull.get(i).convertTo(temp, CvType.CV_32S);
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
        Mat processingFrame = new Mat();
        //обработка кадра различными фильтрами
        Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_BGR2GRAY);
        //bilateral фильтр лучше для краев
        Imgproc.bilateralFilter(inputFrame, processingFrame, diameter, sigmaColor, sigmaSpace);
        Imgproc.Canny(processingFrame, processingFrame, threshold, threshold * 3, 3, false);
        Imgproc.GaussianBlur(processingFrame, processingFrame, new org.opencv.core.Size(ksize, ksize), 3);
        Imgproc.morphologyEx(processingFrame, processingFrame, Imgproc.MORPH_OPEN, structuredElement, new Point(-1, -1), 1);
        return processingFrame;
    }

    private List<MatOfPoint> filterContoursByAngle(List<MatOfPoint> contours, double minAngle, double maxAngle) {
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
            if (tempAngle > maxAngle || tempAngle < minAngle) {
                contours.remove(i);
                i--;
            }
        }
        return contours;
    }

    private List<MatOfPoint> filterContoursByArea(List<MatOfPoint> contours, double minArea, double maxArea) {
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area < minArea || area > maxArea) {
                contours.remove(i);
                i--;
            }
        }
        return contours;
    }

    private List<MatOfPoint> findTwoMatchingShapes(List<MatOfPoint> contours) {
        double matchingRatio = Double.MAX_VALUE;
        //поиск похожих половин стола, с помощью отношения площади и функции opencv
        int indexOfFirstTableContour = -1;
        int indexOfSecondTableContour = -1;
        int counter = 0;
        for (int i = 0; i < contours.size() - 1 && counter == 0; i++) {
            for (int j = i + 1; j < contours.size(); j++) {
                double curMatchingRatio = Imgproc.matchShapes(contours.get(i), contours.get(j), 1, 0.0);
                if (curMatchingRatio < matchingRatio) {
                    double areaRatio = Math.abs(Imgproc.contourArea(contours.get(i)) / Imgproc.contourArea(contours.get(j)));
                    if (areaRatio > minRatio && areaRatio < maxRatio) {
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
        } else {
            return null;
        }
    }

    public Boolean getIsDetected() {
        return isDetected;
    }

    private Mat print(Mat cntImg, List<MatOfPoint> contours, int thickness, Boolean isRandomColor) {
        Scalar color = isRandomColor ? Palette.getNextColor() : Palette.WHITE;
        Imgproc.drawContours(cntImg, contours, -1, color, thickness);
        return cntImg;
    }
}