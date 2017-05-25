package com.technostart.playmate.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
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

    //имя файла без абсолютного пути
    public static String getNameOfFile(String absolutePath) {
        int i = absolutePath.lastIndexOf("/");
        int j = absolutePath.lastIndexOf(".");
        return absolutePath.substring(i, j);
    }

    public static void createJsonTestFile(List<Point> points, String fileNameOfObject) {
        try {
            //создание нового объекта json
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String fileNameWithJson = (System.getProperty("user.dir") + "/src/main/resources/com/technostart/playmate/table_tests/" + getNameOfFile(fileNameOfObject) + ".json");
            File file = new File(fileNameWithJson);
            if (file.exists() != true) {
                file.createNewFile();
            }
            String stringPoints = gson.toJson(points);
            FileWriter fileWriter = new FileWriter(fileNameWithJson);
            fileWriter.write(stringPoints);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException ex) {
        }
    }
}
