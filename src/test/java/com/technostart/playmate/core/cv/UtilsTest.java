package com.technostart.playmate.core.cv;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Point;

import static org.junit.Assert.*;

public class UtilsTest {

    @Before
    public void setUp() throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Test
    public void testGetDistance() throws Exception {
        Point p1 = new Point(0, 0);
        Point p2 = new Point(0, 1);
        Point p3 = new Point(0, 2);
        Point p4 = new Point(2, 0);
        assertEquals(0, Utils.getDistance(p1, p1), 0);
        assertEquals(1, Utils.getDistance(p1, p2), 0);
        assertEquals(4, Utils.getDistance(p1, p3), 0);
        assertEquals(4, Utils.getDistance(p1, p4), 0);
    }
}