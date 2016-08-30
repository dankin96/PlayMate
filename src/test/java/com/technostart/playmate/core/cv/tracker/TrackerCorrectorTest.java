package com.technostart.playmate.core.cv.tracker;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TrackerCorrectorTest {
    TrackerCorrector trackerCorrector;
    List<Point> correctedPoints;

    @Before
    public void setUp() throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        correctedPoints = new ArrayList<>();
        trackerCorrector = new TrackerCorrector(newPoint -> correctedPoints.add(newPoint));
    }

    @Test
    public void addNewPoint() throws Exception {
        Point p1 = new Point(-3, 3);
        Point p2 = new Point(-2, 2);
        Point p3 = new Point(-1, 1);
        Point p4 = new Point(0, 0);
        Point p5 = new Point(1, 1);
        Point p6 = new Point(2, 2);
        Point p7 = new Point(3, 3);
        Point p8 = new Point(4, 2);
        Point newP = new Point(5, 1);
        Point p9 = new Point(6, 2);
        Point p10 = new Point(7, 3);
        Point p11 = new Point(8, 4);

        trackerCorrector.addNewPoint(p1);
        trackerCorrector.addNewPoint(p2);
        trackerCorrector.addNewPoint(p3);
        trackerCorrector.addNewPoint(p4); //
        trackerCorrector.addNewPoint(p5);
        trackerCorrector.addNewPoint(p6);
        trackerCorrector.addNewPoint(p7);
        trackerCorrector.addNewPoint(p8);
        trackerCorrector.addNewPoint(p9);
        trackerCorrector.addNewPoint(p10);
        trackerCorrector.addNewPoint(p11);

        List<Point> expectedPoints = Arrays.asList(p2, p3, p4, p5, p6, p7, p8, newP, p9);
        assertEquals(expectedPoints, correctedPoints);


    }

}