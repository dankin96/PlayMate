/*package com.technostart.playmate.core.cv.tracker;

import com.technostart.playmate.core.cv.tracker.Hit.Direction;
import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class HitDetectorTest {
    HitDetector hitDetector;
    List<Point> hitList;
    List<Direction> directionList;

    @Before
    public void setUp() throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        hitDetector = new HitDetector();
        hitList = new ArrayList<>();
        directionList = new ArrayList<>();

        hitDetector.setHitDetectorListener((hitPoint, direction) -> {
            hitList.add(hitPoint);
            directionList.add(direction);
        });
    }

    @Test
    public void testAddNewPoint() {
        hitDetector.addNewPoint(new Point(-2, 2));
        hitDetector.addNewPoint(new Point(-1, 1));
        hitDetector.addNewPoint(new Point(0, 0)); // hit
        hitDetector.addNewPoint(new Point(1, 1));
        hitDetector.addNewPoint(new Point(2, 2));
        hitDetector.addNewPoint(new Point(3, 1)); // hit
        hitDetector.addNewPoint(new Point(4, 2));
        hitDetector.addNewPoint(new Point(3, 3));
        hitDetector.addNewPoint(new Point(2, 2)); // hit
        hitDetector.addNewPoint(new Point(1, 3));
        hitDetector.addNewPoint(new Point(1, 3));

        List<Point> expectedPoints = Arrays.asList(new Point(0, 0), new Point(3, 1), new Point(2, 2));
        List<Direction> expectedDirections = Arrays.asList(Direction.LEFT_TO_RIGHT, Direction.LEFT_TO_RIGHT, Direction.RIGHT_TO_LEFT);
        assertEquals(expectedPoints, hitList);
        assertEquals(expectedDirections, directionList);
    }
}*/