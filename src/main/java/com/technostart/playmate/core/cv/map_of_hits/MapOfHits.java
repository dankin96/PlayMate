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
    private Mat perspectiveTransform;
    private List<Point> srcPointsTable;
    private List<Point> dstPointsTable;
    private Map<Point, Direction> ballPoints;
    private int curWidth;
    private int curHeight;
    private Mat tableImg;
    private Direction currentDirection;

    public MapOfHits() {
        srcPointsTable = new ArrayList<Point>();
        dstPointsTable = new ArrayList<Point>();
        ballPoints = new HashMap<Point, Direction>();
        perspectiveTransform = new Mat();
        curWidth = 0;
        curHeight = 0;
        currentDirection = Direction.UNDEFINED;
        tableImg = Imgcodecs.imread(System.getProperty("user.dir") + "/src/main/resources/com/technostart/playmate/gui/table.png", Imgcodecs.CV_LOAD_IMAGE_COLOR);
    }

    //для получения картинки карты попаданий
    public Mat getMap(Point ballCoords, Direction set) {
        ballPoints.put(getNewHomoCoords(ballCoords), set);
        Mat heatMap = new Mat(new Size(curWidth, curHeight), CvType.CV_8UC3);
        Imgproc.resize(tableImg, heatMap, new Size(curWidth, curHeight));
//        Imgproc.applyColorMap(heatMap, heatMap, Imgproc.COLORMAP_RAINBOW);
//        Core.addWeighted(heatMap, 0.5, tableImg, 1.0, 0, heatMap);
        Imgproc.line(heatMap, new Point(0, curHeight / 2), new Point(curWidth, curHeight / 2), Palette.WHITE, 4);
        Imgproc.line(heatMap, new Point(curWidth / 2, 0), new Point(curWidth / 2, curHeight), Palette.NET, 2);
        heatMap = printBall(heatMap, ballPoints, curWidth, currentDirection);
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
        Mat transformedPoints = new Mat();
        List<Point> newCoords = new ArrayList<Point>();
        Core.perspectiveTransform(Converters.vector_Point2f_to_Mat(oldCoords), transformedPoints, perspectiveTransform);
        Converters.Mat_to_vector_Point2f(transformedPoints, newCoords);
        return newCoords;
    }

    public Point getNewHomoCoords(Point oldCoord) {
        Mat transformedPoints = new Mat();
        List<Point> temp = new ArrayList<Point>();
        temp.add(oldCoord);
        Core.perspectiveTransform(Converters.vector_Point2f_to_Mat(temp), transformedPoints, perspectiveTransform);
        temp.clear();
        Converters.Mat_to_vector_Point2f(transformedPoints, temp);
        return temp.get(0);
    }

    public void setCurrentDirection(Direction direction) {
        currentDirection = direction;
    }

    private Mat printBall(Mat heatMap, Map<Point, Direction> center, int width, Direction currentDirection) {
        if (currentDirection == Direction.UNDEFINED) {
            for (Map.Entry<Point, Direction> entry : center.entrySet()) {
//                Imgproc.circle(heatMap, entry.getKey(), width / 90, Palette.WHITE, -1);
                center.forEach((key, value) -> {
                    if (value == Direction.LEFT_TO_RIGHT) {
                        Imgproc.circle(heatMap, key, width / 90, Palette.RED, -1);
                    }
                });
                center.forEach((key, value) -> {
                    if (value == Direction.RIGHT_TO_LEFT) {
                        Imgproc.circle(heatMap, key, width / 90, Palette.GREEN, -1);
                    }
                });
            }
        } else if (currentDirection == Direction.LEFT_TO_RIGHT) {
            center.forEach((key, value) -> {
                if (value == Direction.LEFT_TO_RIGHT) {
                    Imgproc.circle(heatMap, key, width / 90, Palette.WHITE, -1);
                }
            });
        } else {
            center.forEach((key, value) -> {
                if (value == Direction.RIGHT_TO_LEFT) {
                    Imgproc.circle(heatMap, key, width / 90, Palette.WHITE, -1);
                }
            });
        }
        return heatMap;
    }
}
