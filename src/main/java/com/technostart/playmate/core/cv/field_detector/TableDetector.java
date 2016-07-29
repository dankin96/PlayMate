package com.technostart.playmate.core.cv.field_detector;

import org.opencv.core.Mat;
import org.opencv.core.Size;

public class TableDetector extends FieldDetector{
    public TableDetector(Size frameSize) {
        super(frameSize);
    }

    @Override
    public Mat getField(Mat frame) {
        // TODO: тут вся обработка без отрисовки.
        // возвращает маску стола
        return null;
    }

    public Mat getFrame(Mat frame) {
        getField(frame);
        // Тут можно сделать отриcовку.
        return frame;
    }
}
