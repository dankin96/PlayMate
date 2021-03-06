package com.technostart.playmate.core.cv.field_detector;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.settings.Cfg;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class TableDetector2 extends FieldDetector {
    @Cfg
    int sigmaColor = 101;
    @Cfg
    int sigmaSpace = 101;
    @Cfg
    double structElementSizeRate = 0.01;
    @Cfg
    int edgesNumber = 4;
    @Cfg
    int diameter = 5;
    @Cfg
    int threshold = 80;
    @Cfg
    public double minRatio = 0.85;
    @Cfg
    public double maxRatio = 1.15;
    @Cfg
    double minAngle = -15.0;
    @Cfg
    double maxAngle = 15.0;
    @Cfg
    double approxAngleThreshold = 200;
    @Cfg
    double minContourAreaRate = 0.1;
    @Cfg
    double maxContourAreaRate = 0.75;

    private double frameArea;

    private Mat processingFrame;
    private Mat structuredElement;
    private List<MatOfPoint> approxContours;
    private Boolean isDetected = false;

    public TableDetector2(Size frameSize) {
        super(frameSize);
        processingFrame = new Mat();
        updateStructuredElement();
        frameArea = frameSize.width * frameSize.height;
    }

    @Override
    public Mat getField(Mat inputFrame) {
        Mat cntImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC1);
        List<MatOfPoint> convexHullList = getContours(inputFrame);
        if (convexHullList != null) {
            approxContours = approximateContours(convexHullList, edgesNumber);
            print(cntImg, approxContours, -1, false);
            convexHullList.clear();
        }
        return cntImg;
    }

    @Override
    public List<MatOfPoint> getContours(Mat inputFrame) {
        List<MatOfPoint> contours = new ArrayList<>();
        processingFrame = frameFilter(inputFrame, threshold);
        Imgproc.findContours(processingFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        double minArea = frameArea * minContourAreaRate;
        double maxArea = frameArea * maxContourAreaRate;
        contours = filterContoursByArea(contours, minArea, maxArea);
        contours = filterContoursByAngle(contours, minAngle, maxAngle);
        contours = convexHull(contours);
        return approximateContours(contours, edgesNumber);
    }

    public Mat getFrame(Mat inputFrame) {
        List<MatOfPoint> contours = getContours(inputFrame);
        Mat newFrame = new Mat();
        Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_GRAY2BGR);
//        Core.addWeighted(inputFrame, 0.5, fieldMask, 0.5, 1, newFrame);
        Imgproc.drawContours(inputFrame, contours, -1, Palette.GREEN, 1);
        return inputFrame;
    }

    private List<MatOfPoint> convexHull(List<MatOfPoint> contours) {
        List<MatOfPoint> newContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            newContours.add(Utils.convexHull(contour));
        }
        return newContours;
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
        Mat processingFrame = new Mat();
        Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.bilateralFilter(inputFrame, processingFrame, diameter, sigmaColor, sigmaSpace);
        Imgproc.Canny(processingFrame, processingFrame, threshold, threshold * 3, 3, false);
        double size = structElementSizeRate * frameSize.height;
        Imgproc.GaussianBlur(processingFrame, processingFrame, new Size(5, 5), 3);
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
    private Mat print(Mat cntImg, List<MatOfPoint> contours, int thickness, Boolean isRandomColor) {
        Scalar color = isRandomColor ? Palette.getNextColor() : Palette.WHITE;
        Imgproc.drawContours(cntImg, contours, -1, color, thickness);
        return cntImg;
    }

    public void updateStructuredElement() {
        double size = frameSize.width * structElementSizeRate;
        Size structElementSize = new Size(size, size);
        structuredElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, structElementSize);
    }
}