package com.technostart.playmate.core.cv.tracker;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

public class HitDetectorFilter {
    public static boolean check(Point hitPoint, Mat fieldMask) {
        double value = fieldMask.get((int) hitPoint.x, (int) hitPoint.y)[0];
        return value > 0;
    }

    public static boolean check(Point hitPoint, MatOfPoint contour) {
        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
        // It returns positive (inside), negative (outside), or zero (on an edge) value, correspondingly.
        // When measureDist=false , the return value is +1, -1, and 0, respectively.
        double testResult = Imgproc.pointPolygonTest(contour2f, hitPoint, false);
        return testResult >= 0;
    }
}
