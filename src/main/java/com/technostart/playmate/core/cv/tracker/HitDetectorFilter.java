package com.technostart.playmate.core.cv.tracker;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class HitDetectorFilter {
    public static boolean check(Point hitPoint, Mat fieldMask) {
        int x = (int) hitPoint.x;
        int y = (int) hitPoint.y;
        if (fieldMask.width() < x || x < 0) return false;
        if (fieldMask.height() < y || y < 0) return false;
        double[] valueArray = fieldMask.get(x, y);
        if (valueArray == null) return false;
        double value = valueArray[0];
        return value > 0;
    }

    public static boolean check(Point hitPoint, MatOfPoint contour) {
        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
        // It returns positive (inside), negative (outside), or zero (on an edge) value, correspondingly.
        // When measureDist=false , the return value is +1, -1, and 0, respectively.
        double testResult = Imgproc.pointPolygonTest(contour2f, hitPoint, false);
        return testResult >= 0;
    }

    public static boolean check(Point hitPoint, List<MatOfPoint> contours) {
        for (MatOfPoint contour : contours) {
            if (check(hitPoint, contour)) return true;
        }
        return false;
    }
}
