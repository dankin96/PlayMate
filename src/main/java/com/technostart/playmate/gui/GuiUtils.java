package com.technostart.playmate.gui;

import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class GuiUtils {
    public static Scalar str2scalar(String str) {
        double[] vals;
        vals = Arrays.stream(str.split(",")).map(String::trim).mapToDouble(Integer::parseInt).toArray();
        return new Scalar(vals);
    }

    public static Image mat2Image(Mat frame, int jpgQuality) {
        int[] params = new int[2];
        params[0] = Imgcodecs.IMWRITE_JPEG_QUALITY;
        params[1] = jpgQuality;
        MatOfInt matOfParams = new MatOfInt();
        matOfParams.fromArray(params);
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, buffer, matOfParams);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}
