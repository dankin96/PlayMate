package com.technostart.playmate.core.cv.tracker;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class HitDetectorTest {
    HitDetector hitDetector;
    List<Point> hitList;

    @Before
    public void setUp() throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        hitDetector = new HitDetector();
        hitList = new ArrayList<>();
        hitDetector.setHitDetectorListener(hitPoint -> hitList.add(hitPoint));
    }

    @Test
    public void testAddNewPoint() throws Exception {
        hitDetector.addNewPoint(new Point(-2, 2));
        hitDetector.addNewPoint(new Point(-1, 1));
        hitDetector.addNewPoint(new Point(0, 0)); // hit
        hitDetector.addNewPoint(new Point(1, 1));
        hitDetector.addNewPoint(new Point(2, 2));
        hitDetector.addNewPoint(new Point(3, 1)); // hit
        hitDetector.addNewPoint(new Point(4, 2));
        hitDetector.addNewPoint(new Point(3, 3));
        hitDetector.addNewPoint(new Point(2, 2));

        List<Point> expected = Arrays.asList(new Point(0, 0), new Point(3, 1));
        assertEquals(expected, hitList);
    }
}