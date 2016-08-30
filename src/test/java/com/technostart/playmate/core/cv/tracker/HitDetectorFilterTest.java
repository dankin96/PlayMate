package com.technostart.playmate.core.cv.tracker;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.*;

import static org.junit.Assert.assertEquals;

public class HitDetectorFilterTest {
    @Before
    public void setUp() throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Test
    public void checkMaskTest() throws Exception {
        Mat mask = Mat.zeros(10, 10, CvType.CV_8UC1);
        Point inPoint = new Point(5, 5);
        Point outPoint = new Point(1, 1);
        mask.put((int) inPoint.x, (int) inPoint.y, 255);
        assertEquals(true, HitDetectorFilter.check(inPoint, mask));
        assertEquals(false, HitDetectorFilter.check(outPoint, mask));

    }

    @Test
    public void checkContourTest() throws Exception {
        Point[] contourPoints = {new Point(1, 1), new Point(1, -1), new Point(-1, -1), new Point(-1, 1)};
        MatOfPoint contour = new MatOfPoint();
        contour.fromArray(contourPoints);
        Point inPoint = new Point(0, 0);
        Point outPoint = new Point(2, 2);
        assertEquals(true, HitDetectorFilter.check(inPoint, contour));
        assertEquals(false, HitDetectorFilter.check(outPoint, contour));
    }

}