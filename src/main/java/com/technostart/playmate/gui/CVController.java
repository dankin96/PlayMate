package com.technostart.playmate.gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class CVController implements Initializable {
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
    private double currentFrameNumber;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        videoFileName = "";
        imageToShow = new Image("com/technostart/playmate/gui/video.png", true);
        currentFrame.setImage(imageToShow);
        processedFrame.setImage(imageToShow);
        //инициализация слайдера
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                                Number oldValue, Number newValue) {
                if (capture != null) {
                    System.out.print("\nFrame\n");
                    double pos = slider.getValue() * frameCount / 100;
                    if (pos < 0) {
                        pos = 0;
                    }
                    if (pos >= frameCount) {
                        pos = frameCount - 2;
                    }
                    currentFrameNumber = Math.round(pos);
                    System.out.print(pos + "\n");
                    System.out.print("Slider Value Changed (newValue: " + newValue.intValue() + ")\n");
                }
            }
        });

    }

    protected void print() {
        imageToShow = grabFrame();
        currentFrame.setImage(imageToShow);
        processedFrame.setImage(imageToShow);
    }

    @FXML
    protected void openFile(ActionEvent event) {
        if (this.capture != null) {
            this.capture.release();
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File videoFile = fileChooser.showOpenDialog(null);
        videoFileName = videoFile.getAbsolutePath();
        capture = new VideoCapture(videoFileName);
        Mat matOrig = new Mat();
        System.out.print("\nname" + videoFileName);
        frameCount = this.capture.get(7);
        print();
        System.out.print("\npos" + this.capture.get(7));
    }

    //менять кадры с клавиатуры на < и >
    @FXML
    protected void changeFrame(KeyEvent event) {
        if (this.capture != null && this.capture.isOpened()) {
            if (event.getCode() == KeyCode.PERIOD && this.capture.get(1) != frameCount) {
                System.out.print("\nFrame\n");
                print();
            }

            if (event.getCode() == KeyCode.COMMA && this.capture.get(1) != 0) {
                this.capture.set(1, this.capture.get(1) - 2);
                System.out.print("\nFrame\n");
                print();
            }
        } else {
            System.out.print("\nVideoStream doesn't opened");
        }
    }

    @FXML
    protected void nextFrame(ActionEvent event) {
        event.getEventType();
        if (this.capture != null && this.capture.isOpened()) {
            if (this.capture.get(1) != frameCount) {
                System.out.print("\nFrame\n");
                print();
            }
        } else {
            System.out.print("\nVideoStream doesn't opened");
        }
    }

    @FXML
    protected void previousFrame(ActionEvent event) {
        if (this.capture != null && this.capture.isOpened()) {
            if (this.capture.get(1) != 0) {
                this.capture.set(1, this.capture.get(1) - 2);
                System.out.print("\nFrame\n");
                print();
            }
        } else {
            System.out.print("\nVideoStream doesn't opened");
        }
    }

    private Image grabFrame() {
        Image imageToShow = null;
        Mat frame = new Mat();
        if (this.capture.isOpened()) {
            try {
                this.capture.read(frame);
                if (!frame.empty()) {
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                    imageToShow = mat2Image(frame);
                }
            } catch (Exception e) {
                System.err.println("Exception during the image elaboration: " + e);
            }
        }
        return imageToShow;
    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    @FXML
    public void setFrame() {
        print();
    }
}