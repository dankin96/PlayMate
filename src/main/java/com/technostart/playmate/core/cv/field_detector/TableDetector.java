package com.technostart.playmate.core.cv.field_detector;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.settings.Cfg;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

public class TableDetector extends FieldDetector {
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
    private int threshold = 100;
    @Cfg
    static public double minRatio = 0.95;
    @Cfg
    static public double maxRatio = 1.05;

    private List<MatOfPoint> contours;
    private List<MatOfPoint> convexHull;
    private List<MatOfPoint> approxContours;
    private Mat processingFrame;
    private Mat structeredElement;
    private int min_area;

    public TableDetector(Size frameSize) {
        super(frameSize);
        processingFrame = new Mat();
        structeredElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(ksize, ksize));
        contours = new ArrayList<MatOfPoint>();
        convexHull = new ArrayList<MatOfPoint>();
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
        min_area = inputFrame.height() * inputFrame.width() / areaCoef;
        //предварительная обработка изображения фильтрами
        processingFrame = frameFilter(inputFrame, threshold);
        //поиск контуров на картинке
        Imgproc.findContours(processingFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        //фильтрация контуров
        contours = contourFilter(contours, min_area);
        //построение нового изображения
        Mat cntImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC3);
        convexHull = convexHull(contours);
        approxContours = Utils.findTwoMatchingShapes(approximateContours(convexHull, edgesNumber));
        print(cntImg);
        convexHull.clear();
        contours.clear();
        approxContours.clear();
        return cntImg;
    }

    private List<MatOfPoint> convexHull(List<MatOfPoint> contours) {
        List<MatOfPoint> hullmop = new ArrayList<MatOfPoint>();
        for (int i = 0; i < contours.size(); i++) {
            hullmop.add(Utils.convexHull(contours.get(i)));
        }
        return hullmop;
    }


    private List<MatOfPoint> approximateContours(List<MatOfPoint> convexHull, int edgesNumber) {
        List<MatOfPoint> approxContours = new ArrayList<MatOfPoint>();
        for (int i = 0; i < convexHull.size(); i++) {
            MatOfPoint temp = new MatOfPoint();
            convexHull.get(i).convertTo(temp, CvType.CV_32S);
            //если сторон больше нужного количества, то аппроксимируем
            if (temp.rows() <= edgesNumber) {
                approxContours.add(temp);
            } else {
                List<Point> listOfPoints = Utils.approximate(temp, edgesNumber);
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
        Imgproc.morphologyEx(processingFrame, processingFrame, Imgproc.MORPH_OPEN, structeredElement, new Point(-1, -1), 1);
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

    private Mat print(Mat cntImg) {
        for (int i = 0; i < approxContours.size(); i++) {
            Imgproc.drawContours(cntImg, approxContours, i, Palette.getNextColor(), -1);
        }
        for (int i = 0; i < convexHull.size(); i++) {
            Imgproc.drawContours(cntImg, convexHull, i, Palette.WHITE, 2);
        }
        /*for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(cntImg, contours, i, Palette.GREEN, 3);
        }*/
        return cntImg;
    }
}