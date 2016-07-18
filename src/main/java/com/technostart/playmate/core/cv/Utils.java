package com.technostart.playmate.core.cv;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.List;

public class Utils {
    // Параметры по умолчанию
    public static final int DEFAULT_KERNEL_RATE = 250;

    // Resize
    private static int resizeHeight = 480;
    private static int resizeWidth = 640;
    private static int perimeterHalf = resizeHeight + resizeWidth;
    public static int resizeInterpolation = Imgproc.INTER_LINEAR;

    // Параметры шумодава
    private static int kernelRate = 250;
    private static int kernelRadius;
    public static Mat kernelShape;

    static {
        kernelRadius = perimeterHalf / kernelRate;
        kernelShape = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,
                new Size(kernelRadius, kernelRadius));
    }

    private static int updatePerimeter() {
        perimeterHalf = resizeHeight + resizeWidth;
        updateKernelShape();
        return perimeterHalf;
    }

    private static int updateKernelShape() {
        kernelRadius = perimeterHalf / kernelRate;
        kernelShape = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,
                new Size(kernelRadius, kernelRadius));
        return kernelRadius;
    }

    public static void setResizeHeight(int value) {
        resizeHeight = value;
        updatePerimeter();
    }

    public static void setResizeWidth(int value) {
        resizeHeight = value;
        updatePerimeter();
    }

    public static void setKernelRate(int value) {
        kernelRate = value;
        updateKernelShape();
    }

    public static int getResizeHeight() {
        return resizeHeight;
    }

    public static int getResizeWidth() {
        return resizeWidth;
    }

    public static int getKernelRate() {
        return kernelRate;
    }

    public static int getKernelRadius() {
        return kernelRadius;
    }

    public static Mat resizeIn(Mat src) {
        if (src.height() < resizeHeight && src.width() < resizeWidth) {
            return src;
        }
        Mat dst = new Mat();
        Imgproc.resize(src, dst, new Size(resizeWidth, resizeHeight), 0, 0, resizeInterpolation);
        return dst;
    }

    public static Mat resizeOut(Mat src) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static Mat filterNoise(Mat src) {
        Mat dst = new Mat();
        Imgproc.morphologyEx(src, dst, Imgproc.MORPH_OPEN, kernelShape);
        return dst;
    }

    public static double getDistance(Point p1, Point p2) {
        return Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2);
    }

    public static Point getCentroid(MatOfPoint contour) {
        Moments m = Imgproc.moments(contour);
        double cx = m.m10 / m.m00;
        double cy = m.m01 / m.m00;
        return new Point(cx, cy);
    }

    public static MatOfPoint findNearestContour(MatOfPoint contour, List<MatOfPoint> contours) {
        MatOfPoint firstContour = null;
        for (MatOfPoint cnt : contours) {
            if (contour != cnt) {
                firstContour = cnt;
                break;
            }
        }
        if (firstContour == null) return null;
        MatOfPoint nearestCnt = firstContour;
        Point c1 = getCentroid(contour);
        Point c2 = getCentroid(firstContour);
        double minDistance = Utils.getDistance(c1, c2);
        for (MatOfPoint cnt : contours) {
            if (contour == cnt) continue;
            Point centroid = getCentroid(cnt);
            double dist = Utils.getDistance(c1, centroid);
            if (dist < minDistance) {
                minDistance = dist;
                nearestCnt = cnt;
            }
        }
        return nearestCnt;
    }

    public static Mat getContourMask(MatOfPoint contour, Size size) {
        return MatOfByte.zeros(size, CvType.CV_8U);
    }

    public static double scalarDiff(Scalar scalar1, Scalar scalar2) {
        return (Math.abs(scalar1.val[0] - scalar2.val[0]) +
                Math.abs(scalar1.val[1] - scalar2.val[1]) +
                Math.abs(scalar1.val[2] - scalar2.val[2])) / 3;
    }

    public static MatOfPoint findSimilarByColor(MatOfPoint contour, List<MatOfPoint> contours, Mat img) {
        MatOfPoint firstContour = null;
        for (MatOfPoint cnt : contours) {
            if (contour != cnt) {
                firstContour = cnt;
                break;
            }
        }
        if (firstContour == null) return null;
        Scalar mean = Core.mean(img, getContourMask(contour, img.size()));
        Scalar mean1 = Core.mean(img, getContourMask(firstContour, img.size()));
        MatOfPoint similarContour = contours.get(0);
        double minDiff = scalarDiff(mean, mean1);
        for (MatOfPoint cnt : contours) {
            if (contour == cnt) continue;
            Scalar curMean = Core.mean(img, getContourMask(cnt, img.size()));
            double curDiff = scalarDiff(mean, curMean);
            if (curDiff < minDiff) {
                minDiff = curDiff;
                similarContour = cnt;
            }
        }
        return similarContour;
    }

    public static Scalar get() {
        Scalar scalar = new Scalar;
        return scalar;
    }


}
