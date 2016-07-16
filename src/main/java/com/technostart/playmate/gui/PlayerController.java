package com.technostart.playmate.gui;

import com.technostart.playmate.core.cv.Tracker;
import com.technostart.playmate.core.cv.Utils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class PlayerController implements Initializable {
    private static final double RESIZE_RATE = 0.3;

    @FXML
    Button openVideoButton;
    @FXML
    Pane pane;
    @FXML
    Button nextFrame;
    @FXML
    Button previousFrame;
    @FXML
    Slider slider;
    @FXML
    ImageView currentFrame;
    @FXML
    ImageView processedFrame;

    private VideoCapture capture;
    private Image imageToShow;
    private String videoFileName;
    private double frameCount;
    private int currentFrameNumber;

    private Tracker tracker;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        videoFileName = "";
        imageToShow = new Image("com/technostart/playmate/gui/video.png", true);
        currentFrame.setImage(imageToShow);
        processedFrame.setImage(imageToShow);

        tracker = new Tracker();

        // Инициализация слайдера.
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                                Number oldValue, Number newValue) {
                if (capture != null) {
                    System.out.print("\nFrame\n");
                    double pos = slider.getValue() * frameCount / 100;
                    pos = pos < 0 ? 0 : pos;
                    pos = frameCount <= pos ? frameCount - 2 : pos;
                    currentFrameNumber = (int) pos;
                    System.out.print(pos + "\n");
                    System.out.print("Slider Value Changed (newValue: " + newValue.intValue() + ")\n");
                }
            }
        });

    }

    private void showFrame() {
        imageToShow = grabFrame();
//        currentFrame.setImage(imageToShow);
        processedFrame.setImage(imageToShow);
    }

    @FXML
    protected void openFile(ActionEvent event) {
        if (capture != null) {
            capture.release();
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File videoFile = fileChooser.showOpenDialog(null);
        videoFileName = videoFile.getAbsolutePath();
        capture = new VideoCapture(videoFileName);
        System.out.print("\nname" + videoFileName);
        frameCount = capture.get(7);
        showFrame();
        System.out.print("\npos" + frameCount);
    }

    // Переключает кадры с клавиатуры на < и >
    @FXML
    protected void changeFrame(KeyEvent event) {
        if (capture != null && capture.isOpened()) {
            if (event.getCode() == KeyCode.PERIOD) {
                showFrame();
            }

            if (event.getCode() == KeyCode.COMMA) {
                double curFrameNumb = capture.get(1);
                if (curFrameNumb == 0) return;
                capture.set(1, curFrameNumb - 2);
                showFrame();
            }
        } else {
            System.out.print("\nVideoStream doesn't opened");
        }
    }

    @FXML
    protected void nextFrame(ActionEvent event) {
        showFrame();
    }

    @FXML
    protected void previousFrame(ActionEvent event) {
        int newFrameNumber = (int) capture.get(1) - 2;
        if (setCaptureFrame(newFrameNumber)) showFrame();
    }

    @FXML
    public void showCaptureCurrentFrame() {
        setCaptureCurrentFrame();
        showFrame();
    }

    private boolean setCaptureFrame(int value) {
        if (value < 0 || frameCount < value) return false;
        if (capture == null || !capture.isOpened()) return false;
        currentFrameNumber = value;
        capture.set(1, currentFrameNumber);
        return true;
    }

    private boolean setCaptureCurrentFrame() {
        return setCaptureFrame(currentFrameNumber);
    }


    private Image grabFrame() {
        Image imageToShow = null;
        Mat frame = new Mat();
        if (capture.isOpened()) {
            capture.read(frame);
            if (!frame.empty()) {
                frame = Utils.resizeIn(frame);
                frame = tracker.getFrame(frame);
                imageToShow = mat2Image(frame);
            }
        }
        return imageToShow;
    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}