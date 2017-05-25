package com.technostart.playmate;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class Sandbox {

    @Before
    public void setUp() throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Test
    public void perspectiveTest() {
        Point p0 = new Point(0, 0);
        Point p1 = new Point(8, 0);
        Point p2 = new Point(6, 2);
        Point p3 = new Point(2, 2);

        Point p4 = new Point(2, 0);
        Point p5 = new Point(0, 2);

        Point p6 = new Point(4, 0);
        Point p7 = new Point(0, 2);

        Point[] srcPoints = {p0, p1, p2, p3};
        Point[] dstPoints = {p0, p4, p3, p5};
        MatOfPoint2f srcMat = new MatOfPoint2f(srcPoints);
        MatOfPoint2f dstMat = new MatOfPoint2f(dstPoints);

        MatOfPoint2f input = new MatOfPoint2f(p0, p1, p2, p3, p6);
        MatOfPoint2f output = new MatOfPoint2f();

        Mat mat = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Core.perspectiveTransform(input, output, mat);

        input.toList().forEach(System.out::println);
        System.out.println("---");
        output.toList().forEach(System.out::println);
    }
}
