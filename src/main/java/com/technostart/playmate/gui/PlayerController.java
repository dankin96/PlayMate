package com.technostart.playmate.gui;

import com.technostart.playmate.core.cv.BufferedFrameReader;
import com.technostart.playmate.core.cv.CvFrameReader;
import com.technostart.playmate.core.cv.FrameReader;
import com.technostart.playmate.core.cv.Tracker;
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
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
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
    Slider slider;
    @FXML
    ImageView currentFrame;
    @FXML
    ImageView processedFrame;

    private FrameReader<Mat> capture;
    private String videoFileName;
    private int currentFrameNumber;

    private Tracker tracker;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        videoFileName = "";
        Image imageToShow = new Image("com/technostart/playmate/gui/video.png", true);
        currentFrame.setImage(imageToShow);
//        processedFrame.setImage(imageToShow);

        tracker = new Tracker(5, 5, 0.5f);

        // Инициализация слайдера.
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                                Number oldValue, Number newValue) {
                if (capture != null) {
                    System.out.print("\nFrame\n");
                    int frameNumber = capture.getFrameNumber();
                    double pos = slider.getValue() * frameNumber / 100;
                    pos = pos < 0 ? 0 : pos;
                    pos = frameNumber <= pos ? frameNumber - 2 : pos;
                    currentFrameNumber = (int) pos;
                    System.out.print(pos + "\n");
                    System.out.print("Slider Value Changed (newValue: " + newValue.intValue() + ")\n");
                }
            }
        });

    }

    private void showFrame(Mat inputFame) {
        Mat frame = processFrame(inputFame);
        Image imageToShow = mat2Image(frame);
        currentFrame.setImage(imageToShow);
    }

    @FXML
    protected void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File videoFile = fileChooser.showOpenDialog(null);
        videoFileName = videoFile.getAbsolutePath();
        capture = new CvFrameReader(videoFileName);
//        CvFrameReader fReader = new CvFrameReader(videoFileName);
//        capture = new BufferedFrameReader<>(fReader, 30, 120);
        System.out.print("\nname" + videoFileName);
        showFrame(capture.read());
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
        showFrame(capture.get(currentFrameNumber));
    }

    private Mat processFrame(Mat frame) {
//        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.resize(frame, frame, new Size(), 0.7, 0.7, Imgproc.INTER_LINEAR);
        return tracker.getFrame(frame);
    }

    private Image mat2Image(Mat frame) {
        int[] params = new int[2];
        params[0] = Imgcodecs.IMWRITE_JPEG_QUALITY;
        params[1] = 70;
        MatOfInt matOfParams = new MatOfInt();
        matOfParams.fromArray(params);
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, buffer, matOfParams);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}