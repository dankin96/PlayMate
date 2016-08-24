package com.technostart.playmate.core.cv.map_of_hits;

import com.technostart.playmate.core.cv.Palette;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.*;

public class MapOfHits {
    public enum Direction {UNDEFINED, LEFT_TO_RIGHT, RIGHT_TO_LEFT}

    ;
    private static Mat perspectiveTransform;
    private static List<Point> srcPointsTable;
    private static List<Point> dstPointsTable;
    private static List<Point> ballPoints;
    private static int curWidth;
    private static int curHeight;
    private static Mat sponsorImg;
    private static Mat heatMap;
    private static Direction currentDirection = Direction.UNDEFINED;

    public MapOfHits() {
        srcPointsTable = new ArrayList<Point>();
        dstPointsTable = new ArrayList<Point>();
        ballPoints = new ArrayList<Point>();
        perspectiveTransform = new Mat();
        curWidth = 0;
        curHeight = 0;
        heatMap = new Mat();
        sponsorImg = Imgcodecs.imread(System.getProperty("user.dir") + "/src/main/resources/com/technostart/playmate/gui/sponsorimg.jpg", Imgcodecs.CV_LOAD_IMAGE_COLOR);
    }

    //для получения картинки карты попаданий
    public Mat getMap(Mat inputFrame, Point ballCoords, Direction set) {
        currentDirection = set;
        //гомография стола
        Mat homographyImgTable = new Mat();
        if (heatMap.empty()) {
            heatMap = Mat.zeros(inputFrame.size(), CvType.CV_8UC3);
//            Imgproc.cvtColor(heatMap, heatMap, Imgproc.COLOR_BGR2HSV);
        }
        Imgproc.warpPerspective(inputFrame, homographyImgTable, perspectiveTransform, new Size(inputFrame.width(), inputFrame.height()));
        Imgproc.resize(sponsorImg, sponsorImg, new Size(homographyImgTable.width(), homographyImgTable.height()));
        Core.addWeighted(homographyImgTable, 0.4, sponsorImg, 0.5, 0, homographyImgTable);
        heatMap = printBall(heatMap, currentDirection, getNewHomoCoords(ballCoords), inputFrame.width());
        Core.addWeighted(homographyImgTable, 0.6, heatMap, 0.5, 0, homographyImgTable);
        Imgproc.circle(homographyImgTable, getNewHomoCoords(ballCoords), inputFrame.width() / 90, Palette.WHITE, -1);
//        return homographyImgTable;
        ballPoints.add(ballCoords);
        return heatMap;
    }

    //для разовой настройки стола и вызова при изменении этого стола на картинке
    public void setField(List<MatOfPoint> contours, Mat inputFrame) {
        if (contours.size() == 2) {
            //проверка стороны стола
            List<Point> srcPointsLeftTable;
            List<Point> srcPointsRightTable;
            if (contours.get(0).toList().get(0).x > contours.get(1).toList().get(0).x) {
                srcPointsLeftTable = contours.get(1).toList();
                srcPointsRightTable = contours.get(0).toList();
            } else {
                srcPointsLeftTable = contours.get(0).toList();
                srcPointsRightTable = contours.get(1).toList();
            }
            //сортировка обоих половин по x
            Comparator pointsComporator = new Comparator<Point>() {
                @Override
                public int compare(Point p1, Point p2) {
                    if (p1.x > p2.x) {
                        return 1;
                    } else if (p1.x < p2.x) {
                        return -1;
                    }
                    return 0;
                }
            };
            Collections.sort(srcPointsLeftTable, pointsComporator);
            Collections.sort(srcPointsRightTable, pointsComporator);
            for (int i = 0; i < srcPointsLeftTable.size(); i++) {
                System.out.println("x left - " + srcPointsLeftTable.get(i).x + " y left - " + srcPointsLeftTable.get(i).y);
            }
            for (int i = 0; i < srcPointsRightTable.size(); i++) {
                System.out.println("x right - " + srcPointsRightTable.get(i).x + " y right - " + srcPointsRightTable.get(i).y);
            }
            //кладем финальные 4 точки в массив исходного стола
            srcPointsTable.clear();
            srcPointsTable.add(srcPointsLeftTable.get(0));
            srcPointsTable.add(srcPointsLeftTable.get(1));
            srcPointsTable.add(srcPointsRightTable.get(2));
            srcPointsTable.add(srcPointsRightTable.get(3));
            for (int i = 0; i < srcPointsRightTable.size(); i++) {
                System.out.println("x src - " + srcPointsTable.get(i).x + " y src - " + srcPointsTable.get(i).y);
            }
            //4 точки в массив выходного стола, при другом размере картинки
            if (curHeight != inputFrame.height() || curWidth != inputFrame.width()) {
                curWidth = inputFrame.width();
                curHeight = inputFrame.height();
                dstPointsTable.clear();
                Point dstP1 = new Point(0, curHeight - 1);
                Point dstP2 = new Point(0, 0);
                Point dstP3 = new Point(curWidth - 1, 0);
                Point dstP4 = new Point(curWidth - 1, curHeight - 1);
                dstPointsTable.add(dstP1);
                dstPointsTable.add(dstP2);
                dstPointsTable.add(dstP3);
                dstPointsTable.add(dstP4);
            }
            perspectiveTransform = Imgproc.getPerspectiveTransform(Converters.vector_Point2f_to_Mat(srcPointsTable), Converters.vector_Point2f_to_Mat(dstPointsTable));
        }
    }

    //получить преобразования точек с известной матрицей преобразования
    public List<Point> getNewHomoCoords(List<Point> oldCoords) {
        Mat transformed = new Mat();
        List<Point> newCoords = new ArrayList<Point>();
        Core.perspectiveTransform(Converters.vector_Point2f_to_Mat(oldCoords), transformed, perspectiveTransform);
        Converters.Mat_to_vector_Point2f(transformed, newCoords);
        return newCoords;
    }

    public Point getNewHomoCoords(Point oldCoord) {
        Mat transformed = new Mat();
        List<Point> temp = new ArrayList<Point>();
        temp.add(oldCoord);
        Core.perspectiveTransform(Converters.vector_Point2f_to_Mat(temp), transformed, perspectiveTransform);
        temp.clear();
        Converters.Mat_to_vector_Point2f(transformed, temp);
        return temp.get(0);
    }

    private Mat printBall(Mat img, Direction set, Point center, int width) {
        if (currentDirection == Direction.LEFT_TO_RIGHT) {
            Imgproc.circle(heatMap, center, width / 90, Palette.YELLOW, -1);
        } else if (currentDirection == Direction.RIGHT_TO_LEFT) {
            Imgproc.circle(heatMap, center, width / 90, Palette.RED, -1);
        } else if (currentDirection == Direction.UNDEFINED) {
            Imgproc.circle(heatMap, center, width / 90, Palette.WHITE, -1);
        }
        return heatMap;
    }
}
