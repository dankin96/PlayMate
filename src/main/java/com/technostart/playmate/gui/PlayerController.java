package com.technostart.playmate.gui;

import com.technostart.playmate.frame_reader.*;
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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

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
    Slider sliderApproxCoef;
    @FXML
    CheckBox checkBoxCanny;
    @FXML
    Label position;
    @FXML
    Label thresholdLabel;
    @FXML
<<<<<<< HEAD
    Label approxLabel;
    @FXML
    ImageView currentFrameView;
    @FXML
=======
>>>>>>> 5091e808b10d71644b104b0590f9f537a1ecaf25
    ImageView processedFrameView;

    private FrameReader<Image> capture;
    private String videoFileName;
    private int frameNumberToShow;

    private Tracker tracker;
    private TableDetectorImpl table;

    private FrameHandler<Image, Mat> frameHandler = new FrameHandler<Image, Mat>() {
        @Override
        public Image process(Mat inputFrame) {
            Mat newFrame = inputFrame.clone();
            Imgproc.cvtColor(newFrame, newFrame, Imgproc.COLOR_BGR2GRAY);
            Imgproc.resize(newFrame, newFrame, new Size(), 0.6, 0.6, Imgproc.INTER_LINEAR);
            newFrame = tracker.getFrame(newFrame);
            return Utils.mat2Image(newFrame);
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        videoFileName = "";
        Image imageToShow = new Image("com/technostart/playmate/gui/video.png", true);
        processedFrameView.setImage(imageToShow);

<<<<<<< HEAD
//        tracker = new Tracker(5, 5, 0.5f);

=======
>>>>>>> 5091e808b10d71644b104b0590f9f537a1ecaf25
        // Инициализация слайдера.
        sliderFrame.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                                Number oldValue, Number newValue) {
                if (capture != null) {
                    System.out.print("\nFrame\n");
                    int frameNumber = capture.getFramesNumber();
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
                table.setThreshold((int) pos);
            }

        });

        // слайдер коэффициента аппроксимации
        sliderApproxCoef.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                                Number oldValue, Number newValue) {
                double pos = sliderApproxCoef.getValue();
                pos = pos < 0 ? 0 : pos;
                pos = sliderApproxCoef.getMax() <= pos ? sliderApproxCoef.getMax() : pos;
                approxLabel.textProperty().setValue(String.format("approx coef - %(.4f" ,pos));
                table.setApproxCoef(pos);
            }

        });

    }

    private void showFrame(Image imageToShow) {
        position.textProperty().setValue(String.valueOf(capture.getCurrentFrameNumber()));
//        Image imageToShow = Utils.mat2Image(inputFrame);
//        currentFrameView.setImage(imageToShow);
        processedFrameView.setImage(imageToShow);
    }

    @FXML
    protected void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File videoFile = fileChooser.showOpenDialog(null);
        videoFileName = videoFile.getAbsolutePath();
<<<<<<< HEAD
        capture = new CvFrameReader(videoFileName);
        table = new TableDetectorImpl((int) this.threshold.getValue(), this.sliderApproxCoef.getValue());
        System.out.print("\nname" + videoFileName);
=======
        // Очистка буфера.
        if (capture != null) capture.close();
        // Инициализация ридера.
        CvFrameReader cvReader = new CvFrameReader(videoFileName);
        Mat firstFrame = cvReader.read();
        tracker = new Tracker(firstFrame.size(), 5, 5, 0.5f);
        table = new TableDetector();

        Mat2ImgReader mat2ImgReader = new Mat2ImgReader(cvReader, frameHandler);
        capture = mat2ImgReader;
//        capture = new BufferedFrameReader<>(mat2ImgReader, 10, 400);
        
>>>>>>> 5091e808b10d71644b104b0590f9f537a1ecaf25
        showFrame(capture.read());

        position.textProperty().setValue("1");
    }

    @FXML
    protected void cannySelected(ActionEvent event) {
        if (this.checkBoxCanny.isSelected()) {
<<<<<<< HEAD
            Mat frame = capture.get(capture.getCurrentFrameNumber());
            frame = table.getTable(frame);
            Image imageToShow = Utils.mat2Image(frame);
            processedFrameView.setImage(imageToShow);
=======
//            Mat frame = capture.get(capture.getCurrentFrameNumber());
//            frame = table.getTable(frame, (int) this.threshold.getValue());
//            Image imageToShow = Utils.mat2Image(frame);
//            processedFrameView.setImage(imageToShow);
>>>>>>> 5091e808b10d71644b104b0590f9f537a1ecaf25
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

<<<<<<< HEAD
    private Mat processFrame(Mat frame) {
        return table.getTable(frame);
    }
=======
>>>>>>> 5091e808b10d71644b104b0590f9f537a1ecaf25
}