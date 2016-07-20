package com.technostart.playmate.gui;

import com.technostart.playmate.core.cv.CvFrameReader;
import com.technostart.playmate.core.cv.FrameReader;
import com.technostart.playmate.core.cv.Tracker;
import com.technostart.playmate.core.cv.BufferedFrameReader;
import com.technostart.playmate.core.cv.CvFrameReader;
import com.technostart.playmate.core.cv.FrameReader;
import com.technostart.playmate.core.cv.Tracker;
import com.technostart.playmate.core.cv.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.opencv.core.Mat;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class PlayerController implements Initializable {
    @FXML
    Button openVideoButton;
    @FXML
    Pane pane;
    @FXML
    Button nextFrame;
    @FXML
    Button previousFrame;
    @FXML
    Slider sliderFrame;
    @FXML
    Slider threshold;
    @FXML
    CheckBox checkBoxCanny;
    @FXML
    Label position;
    @FXML
    Label thresholdLabel;
    @FXML
    ImageView currentFrameView;
    @FXML
    ImageView processedFrameView;

    private FrameReader<Mat> capture;
    private String videoFileName;
    private int frameNumberToShow;

    private Tracker tracker;
    private TableDetector table;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        videoFileName = "";
        Image imageToShow = new Image("com/technostart/playmate/gui/video.png", true);
        currentFrameView.setImage(imageToShow);
        processedFrameView.setImage(imageToShow);

        tracker = new Tracker(5, 5, 0.5f);
        table = new TableDetector();

        // Инициализация слайдера.
        sliderFrame.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                                Number oldValue, Number newValue) {
                if (capture != null) {
                    System.out.print("\nFrame\n");
                    int frameNumber = capture.getFrameNumber();
                    double pos = sliderFrame.getValue() * frameNumber / 1000;
                    pos = pos < 0 ? 0 : pos;
                    pos = frameNumber <= pos ? frameNumber - 2 : pos;
                    frameNumberToShow = (int) pos;
                    System.out.print(pos + "\n");
                    System.out.print("Slider Value Changed (newValue: " + newValue.intValue() + ")\n");
                }
            }
        });

        // Инициализация порога для checkBoxCanny.
        threshold.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                                Number oldValue, Number newValue) {
                double pos = threshold.getValue();
                pos = pos < 0 ? 0 : pos;
                pos = threshold.getMax() <= pos ? threshold.getMax() : pos;
                thresholdLabel.textProperty().setValue("threshold - " + String.valueOf((int) pos));
                System.out.print(pos + "\n");
                System.out.print("Threshold Value Changed (newValue: " + newValue.intValue() + ")\n");
            }

        });

    }

    private void showFrame(Mat inputFrame) {
        position.textProperty().setValue(String.valueOf(capture.getCurrentFrameNumber()));
        Image imageToShow = Utils.mat2Image(inputFrame);
        currentFrameView.setImage(imageToShow);
        if (this.checkBoxCanny.isSelected()) {
            inputFrame = processFrame(inputFrame);
        }
        imageToShow = Utils.mat2Image(inputFrame);
        processedFrameView.setImage(imageToShow);
    }

    @FXML
    protected void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File videoFile = fileChooser.showOpenDialog(null);
        videoFileName = videoFile.getAbsolutePath();
        capture = new CvFrameReader(videoFileName);
        System.out.print("\nname" + videoFileName);
        showFrame(capture.read());
        position.textProperty().setValue("1");
        System.out.print("FrameNumber - " + capture.getFrameNumber() + "\n");
    }

    @FXML
    protected void cannySelected(ActionEvent event) {
        if (this.checkBoxCanny.isSelected()) {
            Mat frame = capture.get(capture.getCurrentFrameNumber());
            frame = table.getTable(frame, (int) this.threshold.getValue());
            Image imageToShow = Utils.mat2Image(frame);
            processedFrameView.setImage(imageToShow);
        }
    }

    // Переключает кадры с клавиатуры на < и >
    @FXML
    protected void changeFrame(KeyEvent event) {
        if (event.getCode() == KeyCode.PERIOD) {
            showNextFrame();
        }

        if (event.getCode() == KeyCode.COMMA) {
            showPreviousFrame();
        }
    }

    @FXML
    protected void showNextFrame() {
        showFrame(capture.next());
    }

    @FXML
    protected void showPreviousFrame() {
        showFrame(capture.prev());
    }

    @FXML
    public void showCurrentFrame() {
        showFrame(capture.get(frameNumberToShow));
    }

    private Mat processFrame(Mat frame) {
        return tracker.getFrame(table.getTable(frame, (int) this.threshold.getValue()));
    }
}