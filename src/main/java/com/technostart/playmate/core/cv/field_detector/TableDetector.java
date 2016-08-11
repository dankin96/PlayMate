package com.technostart.playmate.core.cv.field_detector;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.cv.settings.Cfg;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

public class TableDetector extends FieldDetector {
    @Cfg
    static int sigmaColor = 25;
    @Cfg
    static int sigmaSpace = 25;
    @Cfg
    static int ksize = 5;
    @Cfg
    static int diameter = 5;
    @Cfg
    private int threshold = 100;

    private List<MatOfPoint> contours;
    private List<MatOfPoint> hullmop;
    private List<MatOfPoint> approxContours;
    private Mat processingFrame;
    private Mat structeredElement;
    private int min_area;

    public TableDetector(Size frameSize) {
        super(frameSize);
        processingFrame = new Mat();
        structeredElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(ksize, ksize));
        contours = new ArrayList<MatOfPoint>();
        hullmop = new ArrayList<MatOfPoint>();
        approxContours = new ArrayList<MatOfPoint>();
        min_area = 0;
    }

    @Override
    public Mat getField(Mat frame) {
        // TODO: тут вся обработка без отрисовки.
        // возвращает маску стола
        return null;
    }

    public Mat getFrame(Mat inputFrame) {
        getField(inputFrame);
        min_area = inputFrame.height() * inputFrame.width() / 250;
        //предварительная обработка изображения фильтрами
        processingFrame = frameFilter(inputFrame, threshold);
        //поиск контуров на картинке
        Imgproc.findContours(processingFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        //фильтрация контуров
        contours = contourFilter(contours, min_area);
        System.out.println("Counter = " + contours.size());
        //построение нового изображения
        Mat cntImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC3);
        hullmop = convexHull(contours);
        approxContours = approximation(hullmop, 4);
        print(cntImg);
        hullmop.clear();
        contours.clear();
        approxContours.clear();
        return cntImg;
    }

    private List<MatOfPoint> convexHull(List<MatOfPoint> contours) {
        List<MatOfPoint> hullmop = new ArrayList<MatOfPoint>();
        for (int i = 0; i < contours.size(); i++) {
            hullmop.add(Utils.convexHull(contours.get(i)));
        }
        return hullmop;
    }

    private List<MatOfPoint> approximation(List<MatOfPoint> hullmop, int edges) {
        List<MatOfPoint> approxContours = new ArrayList<MatOfPoint>();
        MatOfPoint2f approx = new MatOfPoint2f();
        for (int i = 0; i < hullmop.size(); i++) {
            MatOfPoint temp = new MatOfPoint();
            hullmop.get(i).convertTo(approx, CvType.CV_32FC2);
            approx.convertTo(temp, CvType.CV_32S);
            System.out.println("size = " + temp.size());
            //если сторон больше нужного количества, то аппроксимируем
            if (temp.rows() <= edges) {
                approxContours.add(temp);
            } else {
                List<Point> listOfPoints = temp.toList();
                listOfPoints = new LinkedList<>(listOfPoints);
                // убираем итеративно стороны
                while (listOfPoints.size() != edges) {
                    double min_distance = Double.MAX_VALUE;
                    int min_index = -1;
                    for (int j = 0; j < listOfPoints.size(); j++) {
                        Point beginPoint = listOfPoints.get(j);
                        Point endPoint = new Point();
                        if (j != listOfPoints.size() - 1) {
                            endPoint = listOfPoints.get(j + 1);
                        } else {
                            endPoint = listOfPoints.get(0);
                        }
                        System.out.println("x = " + beginPoint.x);
                        System.out.println("y = " + beginPoint.y);
                        double distance = (endPoint.x - beginPoint.x) * (endPoint.x - beginPoint.x) + (endPoint.y - beginPoint.y) * (endPoint.y - beginPoint.y);
                        //ищем индекс начальной точки отрезка с минимальной длиной
                        if (distance < min_distance) {
                            min_distance = distance;
                            min_index = j;
                        }
                    }
                    //выделяем точки необходимые для нахождения пересечения, с учетом граничных случаев
                    int[] index = new int[4];
                    for (int j = 0; j < index.length; j++) {
                        index[j] = min_index + j - 1;
                    }
                    if (index[0] < 0) {
                        index[0] = listOfPoints.size() - 1;
                    } else if (index[2] > listOfPoints.size() - 1) {
                        index[2] = 0;
                        index[3] = 1;
                    } else if (index[3] > listOfPoints.size() - 1) {
                        index[3] = 0;
                    }
                    Point newPoint = Utils.intersection(listOfPoints.get(index[0]), listOfPoints.get(index[1]), listOfPoints.get(index[2]), listOfPoints.get(index[3]));
                    //точка пересечения не лежит на одной прямой с двумя другими точками, иначе ее можно просто удалить
                    if (newPoint != null) {
                        //сохраняем нужный порядок удаления точек
                        if (index[2] > index[1]) {
                            listOfPoints.remove(index[2]);
                            listOfPoints.remove(index[1]);
                        } else {
                            listOfPoints.remove(index[1]);
                            listOfPoints.remove(index[2]);
                        }
                        if (min_index < listOfPoints.size())
                            listOfPoints.add(min_index, newPoint);
                        else
                            listOfPoints.add(newPoint);
                    } else
                        listOfPoints.remove(min_index);
                }
                temp.fromList(listOfPoints);
                approxContours.add(temp);
            }
        }
        return approxContours;
    }

    private Mat frameFilter(Mat inputFrame, int threshold) {
        Mat tempFrame = inputFrame;
        Mat processingFrame = new Mat();
        //обработка кадра различными фильтрами
        Imgproc.cvtColor(tempFrame, tempFrame, Imgproc.COLOR_BGR2GRAY);
        //bilateral фильтр лучше для краев
        Imgproc.bilateralFilter(tempFrame, processingFrame, diameter, sigmaColor, sigmaSpace);
        Imgproc.Canny(processingFrame, processingFrame, threshold, threshold * 3, 3, false);
        Imgproc.GaussianBlur(processingFrame, processingFrame, new org.opencv.core.Size(ksize, ksize), 3);
        Imgproc.morphologyEx(processingFrame, processingFrame, Imgproc.MORPH_OPEN, structeredElement, new Point(-1, -1), 1);
        return processingFrame;
    }

    private List<MatOfPoint> contourFilter(List<MatOfPoint> contours, int min_area) {
        Collections.sort(contours, new Comparator<Mat>() {
            @Override
            public int compare(Mat o1, Mat o2) {
                double area_1 = Imgproc.contourArea(o1);
                double area_2 = Imgproc.contourArea(o2);
                if (area_1 < area_2) {
                    return 1;
                } else if (area_1 > area_2) {
                    return -1;
                }
                return 0;
            }
        });
        //фильтрация по площади
        int counter = 0;
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area < min_area) {
                counter = i + 1;
                break;
            }
        }
        int temp = contours.size();
        for (int i = 0; i < temp - counter; i++) {
            contours.remove(temp - 1 - i);
        }
        return contours;
    }

    private Mat print(Mat cntImg) {
        for (int i = 0; i < approxContours.size(); i++) {
            Imgproc.drawContours(cntImg, approxContours, i, Palette.getNextColor(), -1);
            System.out.println("size = " + approxContours.get(i).size());
        }
        for (int i = 0; i < hullmop.size(); i++) {
            Imgproc.drawContours(cntImg, hullmop, i, Palette.getNextColor(), 3);
        }
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(cntImg, contours, i, Palette.GREEN, 3);
        }
        System.out.println("\nsize contours = " + contours.size());
        System.out.println("\nsize hull = " + hullmop.size());
        System.out.println("\nsize approxcontours = " + approxContours.size());
        return cntImg;
    }
}