package com.technostart.playmate.core.cv;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        assertEquals(2, Utils.getDistance(p1, p3), 0);
        assertEquals(2, Utils.getDistance(p1, p4), 0);
    }

    @Test
    public void testScalarMean() throws Exception {
        Scalar scalar1 = new Scalar(0, 0, 0);
        Scalar scalar2 = new Scalar(1, 1, 1);
        Scalar scalar3 = new Scalar(255, 255, 255);
        Scalar scalar4 = new Scalar(8, 9, 3);
        Scalar scalar5 = new Scalar(8, 3, 0);
        Scalar scalar6 = new Scalar(8, 0, 0);
        Scalar scalar7 = new Scalar(8, 4, 1);
        Scalar scalar8 = new Scalar(255);

        List<Scalar> scalars0 = Arrays.asList(scalar2);
        List<Scalar> scalars1 = Arrays.asList(scalar1, scalar1, scalar1);
        List<Scalar> scalars2 = Arrays.asList(scalar2, scalar2, scalar2);
        List<Scalar> scalars3 = Arrays.asList(scalar3, scalar3, scalar3);
        List<Scalar> scalars4 = Arrays.asList(scalar4, scalar5, scalar6);
        List<Scalar> scalars8 = Arrays.asList(scalar8, scalar8, scalar8);

        assertEquals(scalar2, Utils.scalarMean(scalars0));
        assertEquals(scalar1, Utils.scalarMean(scalars1));
        assertEquals(scalar2, Utils.scalarMean(scalars2));
        assertEquals(scalar3, Utils.scalarMean(scalars3));
        assertEquals(scalar7, Utils.scalarMean(scalars4));
        assertEquals(scalar8, Utils.scalarMean(scalars8));
    }
}