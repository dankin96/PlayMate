package com.technostart.playmate.core.cv.field_detector;

import com.technostart.playmate.core.cv.Palette;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class TableDetectorMock extends FieldDetector {
    private MatOfPoint tableContour;

    public TableDetectorMock(Size frameSize) {
        super(frameSize);
        tableContour = new MatOfPoint();
    }

    @Override
    public Mat getField(Mat inputFrame) {
        Mat cntImg = Mat.zeros(inputFrame.size(), CvType.CV_8UC3);
        Imgproc.fillConvexPoly(cntImg, tableContour, Palette.WHITE);
        return cntImg;
    }

    public void setPoints(MatOfPoint points) {
        this.tableContour = points;
    }
}
