package com.technostart.playmate.core.cv;

import javafx.scene.image.Image;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class Utils {
    // Параметры по умолчанию

    // Чем больше значение тем меньше радиус.
    public static final int DEFAULT_KERNEL_RATE = 300;

    // Resize
    private static int resizeHeight = 480;
    private static int resizeWidth = 640;
    private static int perimeterHalf = resizeHeight + resizeWidth;
    public static int resizeInterpolation = Imgproc.INTER_LINEAR;

    // Параметры шумодава
    private static int kernelRate = DEFAULT_KERNEL_RATE;
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
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Центры масс.
    ///////////////////////////////////////////////////////////////////////////

    public static Point getCentroid(MatOfPoint contour) {
        Moments m = Imgproc.moments(contour);
        double cx = m.m10 / m.m00;
        double cy = m.m01 / m.m00;
        return new Point(cx, cy);
    }

    public static Point getCentroid(List<Point> points) {
        MatOfPoint contour = new MatOfPoint();
        contour.fromList(points);
        contour = convexHull(contour);
        return getCentroid(contour);
    }

    public static Point getWeightedCentroid(List<Point> points, List<Double> weights) {
        int pSize = points.size();
        int wSize = weights.size();
        assert pSize == wSize;

        // Рассчет центра
        double xNum = 0, yNum = 0, denum = 0;
        for (int i = 0; i < pSize; i++) {
            Point point = points.get(i);
            Double weight = weights.get(i);
            xNum += point.x * weight;
            yNum += point.y * weight;
            denum += weight;
        }
        double x = xNum / denum;
        double y = yNum / denum;
        return new Point(x, y);
    }

    public static Point getContoursCentroid(List<MatOfPoint> contours) {
        List<Point> centers = new ArrayList<>();
        for (MatOfPoint cnt : contours) {
            centers.add(Utils.getCentroid(cnt));
        }
        return getCentroid(centers);
    }

    public static Point getContoursCentroid(List<MatOfPoint> contours, List<Double> weights) {
        List<Point> centroids = new ArrayList<>();
        for (int i = 0, size = contours.size(); i < size; i++) {
            centroids.add(centroids.get(i));
        }
        return getWeightedCentroid(centroids, weights);
    }

    // Возвращает контур из центров тяжести входных контуров.
    public static MatOfPoint getEqualContour(List<MatOfPoint> contours) {
        List<Point> centers = new ArrayList<>();
        for (MatOfPoint cnt : contours) {
            centers.add(Utils.getCentroid(cnt));
        }
        MatOfPoint newContour = new MatOfPoint();
        newContour.fromList(centers);
        return newContour;
    }
    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

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
        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(contour);
        Mat mask = MatOfByte.zeros(size, CvType.CV_8U);
        Imgproc.drawContours(mask, contours, -1, new Scalar(255), -1);
        return mask;
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

    public static Scalar getMedianColor(MatOfPoint contour, Mat img) {
        return Core.mean(img, getContourMask(contour, img.size()));
    }

    public static Scalar scalarMean(List<Scalar> scalars) {
        double[] val = new double[scalars.get(0).val.length];
        Scalar mean = new Scalar(val);
        for (Scalar s : scalars) {
            for (int i = 0; i < s.val.length; i++) {
                mean.val[i] += s.val[i];
            }
        }

        for (int i = 0; i < mean.val.length; i++) {
            mean.val[i] /= scalars.size();
        }
        return mean;
    }

    public static MatOfPoint convexHull(MatOfPoint inputContour) {
        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(inputContour, hull);
        List<Point> hullPoints = new ArrayList<>();
        List<Point> contourPoints = inputContour.toList();
        for (int idx : hull.toList()) {
            Point point = contourPoints.get(idx);
            hullPoints.add(point);
        }
        MatOfPoint hullContour = new MatOfPoint();
        hullContour.fromList(hullPoints);
        return hullContour;
    }


    static public Image mat2Image(Mat frame) {
        int[] params = new int[2];
        params[0] = Imgcodecs.IMWRITE_JPEG_QUALITY;
        params[1] = 70;
        MatOfInt matOfParams = new MatOfInt();
        matOfParams.fromArray(params);
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, buffer, matOfParams);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Методы отрисовки.
    ///////////////////////////////////////////////////////////////////////////

    public static void drawLine(List<Point> points, Mat img, Scalar color, int thickness) {
        for (int i = 0; i < points.size() - 1; i++) {
            Imgproc.line(img, points.get(i), points.get(i + 1), color, thickness);
        }
    }


}