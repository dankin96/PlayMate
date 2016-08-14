package com.technostart.playmate.core.cv;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

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

    @Test
    public void getWeightedCentroid() throws Exception {
        Point p0 = new Point(0, 0);
        Point p1 = new Point(1, 1);
        Point p2 = new Point(-1, 1);
        Point p3 = new Point(-1, -1);
        Point p4 = new Point(1, -1);
        Point p5 = new Point(0, -1);
        Point p6 = new Point(0, 5);
        Point p7 = new Point(3, 0);
        Point p8 = new Point(3, 5);

        Point rectCent = new Point(1.5, 2.5);

        Point p11 = new Point(0, 1);
        Point p12 = new Point(0, 2);
        Point p13 = new Point(0, 3);
        Point p14 = new Point(0, 4);
        Point p15 = new Point(0, 5);

        List<Point> squarePoints = Arrays.asList(p1, p2, p3, p4);
        List<Point> rectPoints = Arrays.asList(p0, p6, p7, p8);
        List<Point> linePoints = Arrays.asList(p11, p12, p13, p14, p15);

        List<Double> weights1 = Arrays.asList(1.0, 1.0, 1.0, 1.0);
        List<Double> weights01 = Arrays.asList(0.0, 0.0, 1.0, 1.0);
        List<Double> weights10 = Arrays.asList(10.0, 10.0, 10.0, 10.0);
        List<Double> weights10_2 = Arrays.asList(0.0, 0.0, 10.0, 10.0);

        // Центр квадрата.
        assertEquals(p0, Utils.getWeightedCentroid(squarePoints, weights1));
        // Центр прямоугольника.
        assertEquals(rectCent, Utils.getWeightedCentroid(rectPoints, weights1));
        // Центр нижней стороны квадрата.
        assertEquals(p5, Utils.getWeightedCentroid(squarePoints, weights01));
        // Проверка весов больше 1.
        assertEquals(p0, Utils.getWeightedCentroid(squarePoints, weights10));
        assertEquals(p5, Utils.getWeightedCentroid(squarePoints, weights10_2));
        // TODO: Проверка точек на пямой линии.
    }

    @Test
    public void approximate() throws Exception {
        Point p0 = new Point(0, 0);
        Point p1 = new Point(-2, 2);
        Point p2 = new Point(2, 2);
        Point p3 = new Point(2, -2);
        Point p4 = new Point(-2, -2);
        Point p5 = new Point(2, -4);
        Point p6 = new Point(-2, -4);

        Point p7 = new Point(0, 3);
        Point p8 = new Point(4, 3);
        Point p9 = new Point(5, 2);
        Point p10 = new Point(5, 0);
        Point p11 = new Point(5, 3);

        // Квадрат.
        List<Point> squarePoints = Arrays.asList(p1, p2, p3, p4);
        // Прямоугольник.
        List<Point> rectPoints = Arrays.asList(p1, p2, p5, p6);
        // Прямоугольник со срезанным правым верхним углом.
        List<Point> cutRect1 = Arrays.asList(p0, p7, p8, p9, p10);
        List<Point> cutRectApprox = Arrays.asList(p0, p7, p11, p10);

        assertEquals(squarePoints, Utils.approximate(squarePoints, 4));
        assertEquals(rectPoints, Utils.approximate(rectPoints, 4));
        assertNotEquals(rectPoints, Utils.approximate(squarePoints, 4));
        assertEquals(cutRectApprox, Utils.approximate(cutRect1, 4));
    }
}