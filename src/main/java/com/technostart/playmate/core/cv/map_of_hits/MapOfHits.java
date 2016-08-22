package com.technostart.playmate.core.cv.map_of_hits;

import com.technostart.playmate.core.cv.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapOfHits {
    private static Mat perspectiveTransform;
    private static List<Point> srcPointsLeftTable;
    private static List<Point> srcPointsRightTable;
    private static List<Point> dstPointsLeftTable;
    private static List<Point> dstPointsRightTable;
    private static int curWidth;
    private static int curHeight;

    public MapOfHits(List<MatOfPoint> contours) {
        //проверка стороны стола
        if (contours.get(0).toList().get(0).x > contours.get(1).toList().get(0).x) {
            srcPointsLeftTable = contours.get(1).toList();
            srcPointsRightTable = contours.get(0).toList();
        } else {
            srcPointsLeftTable = contours.get(0).toList();
            srcPointsRightTable = contours.get(1).toList();
        }
        perspectiveTransform = new Mat();
        dstPointsLeftTable = new ArrayList<Point>();
        dstPointsRightTable = new ArrayList<Point>();
        curWidth = 0;
        curHeight = 0;
    }

    public Mat getMap(Mat inputFrame, Point ballCoords) {
        //установка точек для одного размера кадра
        if (inputFrame.width() != curWidth || inputFrame.height() != curHeight) {
            setDstPoints(inputFrame);
        }
        //гомография левой стороный
        perspectiveTransform = Imgproc.getPerspectiveTransform(Converters.vector_Point2f_to_Mat(srcPointsLeftTable), Converters.vector_Point2f_to_Mat(dstPointsLeftTable));
        Mat homographyImgLeftTable = new Mat();
        Imgproc.warpPerspective(inputFrame, homographyImgLeftTable, perspectiveTransform, new Size(inputFrame.width(), inputFrame.height()));
        //гомография правой стороны
        perspectiveTransform = Imgproc.getPerspectiveTransform(Converters.vector_Point2f_to_Mat(srcPointsRightTable), Converters.vector_Point2f_to_Mat(dstPointsRightTable));
        Mat homographyImgRightTable = new Mat();
        Imgproc.warpPerspective(inputFrame, homographyImgRightTable, perspectiveTransform, new Size(inputFrame.width(), inputFrame.height()));
        //склейка двух гомографий
        Mat img = new Mat();
        List<Mat> src = Arrays.asList(homographyImgLeftTable, homographyImgRightTable);
        Core.hconcat(src, img);
        return img;
    }

    public void setField(List<MatOfPoint> contours) {
        //проверка стороны стола
        if (contours.get(0).toList().get(0).x > contours.get(1).toList().get(0).x) {
            srcPointsLeftTable = contours.get(1).toList();
            srcPointsRightTable = contours.get(0).toList();
        } else {
            srcPointsLeftTable = contours.get(0).toList();
            srcPointsRightTable = contours.get(1).toList();
        }
    }

    private void setDstPoints(Mat inputFrame) {
        curWidth = inputFrame.width();
        curHeight = inputFrame.height();
        Point dstP1 = new Point(0, 0);
        Point dstP2 = new Point(curWidth / 2 - 1, 0);
        Point dstP3 = new Point(0, curHeight / 2 - 1);
        Point dstP4 = new Point(curWidth / 2 - 1, curHeight / 2 - 1);
        dstPointsLeftTable.add(dstP1);
        dstPointsLeftTable.add(dstP2);
        dstPointsLeftTable.add(dstP3);
        dstPointsLeftTable.add(dstP4);
        dstP1 = new Point(curWidth / 2, curHeight / 2);
        dstP2 = new Point(curWidth - 1, curHeight / 2);
        dstP3 = new Point(curWidth / 2, curHeight - 1);
        dstP4 = new Point(curWidth - 1, curHeight - 1);
        dstPointsRightTable.add(dstP1);
        dstPointsRightTable.add(dstP2);
        dstPointsRightTable.add(dstP3);
        dstPointsRightTable.add(dstP4);
    }
}
