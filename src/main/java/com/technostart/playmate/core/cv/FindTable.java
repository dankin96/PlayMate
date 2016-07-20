package com.technostart.playmate.core.cv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FindTable {

    private List<MatOfPoint> contours;
    Mat detectedEdges;
    Mat structeredElement;
    int contourId;

    public FindTable() {
        detectedEdges = new Mat();
        structeredElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(5, 5));
    }

    public Mat getTable(Mat inputFrame, int threshold) {
        contours = new ArrayList<MatOfPoint>();
        //обработка
        Imgproc.cvtColor(inputFrame, detectedEdges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(detectedEdges, detectedEdges, new Size(3, 3));
        Imgproc.Canny(detectedEdges, detectedEdges, threshold, threshold * 3, 3, false);
        Imgproc.GaussianBlur(detectedEdges, detectedEdges, new org.opencv.core.Size(5, 5), 5);
        Imgproc.morphologyEx(detectedEdges, detectedEdges, Imgproc.MORPH_OPEN, structeredElement, new Point(-1, -1), 1);
        //поиск контуров
        Imgproc.findContours(detectedEdges, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        this.contourId = findMax();
        //построение нового изображения
        Mat cntImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC3);
        Imgproc.drawContours(cntImg, contours, this.contourId, new Scalar(0, 0, 255), 1);
        Imgproc.resize(cntImg, cntImg, inputFrame.size());
        //добавление найденного контура к текущей картинке
        Core.addWeighted(inputFrame, 0.5, cntImg, 0.5, 0, inputFrame);
        return inputFrame;
    }

    private int findMax() {
        double maxArea = -1;
        int maxAreaIdx = -1;
        for (int idx = 0; idx < contours.size(); idx++) {
            Mat contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(contour);
            if (contourarea > maxArea) {
                maxArea = contourarea;
                maxAreaIdx = idx;
            }
        }
        return maxAreaIdx;
    }
}


