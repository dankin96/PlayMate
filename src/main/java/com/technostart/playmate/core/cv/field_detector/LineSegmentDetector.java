package com.technostart.playmate.core.cv.field_detector;

import com.technostart.playmate.core.cv.Palette;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class LineSegmentDetector extends FieldDetector {
    private double cannyThreshold1 = 10;
    private double cannyThreshold2 = cannyThreshold1 * 3;
    private int houghLinesThreshold = 200;

    public void setCannyThreshold1(double cannyThreshold1) {
        this.cannyThreshold1 = cannyThreshold1;
    }

    public void setCannyThreshold2(double cannyThreshold2) {
        this.cannyThreshold2 = cannyThreshold2;
    }

    public void setHoughLinesThreshold(int houghLinesThreshold) {
        this.houghLinesThreshold = houghLinesThreshold;
    }

    public LineSegmentDetector(Size frameSize) {
        super(frameSize);
    }

    @Override
    public Mat getField(Mat inputFrame) {
        Mat procFrame = inputFrame.clone();
        Mat tmpFrame = new Mat();
        Imgproc.cvtColor(procFrame, procFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.bilateralFilter(procFrame, tmpFrame, 10, 25, 25);
        Imgproc.Canny(tmpFrame, tmpFrame, cannyThreshold1, cannyThreshold2);
        Mat lines = new Mat();
        Imgproc.HoughLinesP(tmpFrame, lines, 1, Math.PI / 180, 10, 120, 30);
        return lines;
    }

public Mat getFrame(Mat inputFrame) {
    Mat lines = getField(inputFrame);
    for (int x = 0; x < lines.rows(); x++) {
        double[] vec = lines.get(x, 0);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            Imgproc.line(inputFrame, start, end, Palette.getRandomColor(10), 3);
        }
        return inputFrame;
    }
}
