package com.technostart.playmate.gui;

import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.cv.field_detector.LineSegmentDetector;
import com.technostart.playmate.core.cv.settings.SettingsManager;
import com.technostart.playmate.core.cv.tracker.Tracker;
import com.technostart.playmate.frame_reader.CvFrameReader;
import com.technostart.playmate.frame_reader.FrameHandler;
import com.technostart.playmate.frame_reader.FrameReader;
import com.technostart.playmate.frame_reader.Mat2ImgReader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class PlayerController implements Initializable {

    SettingsManager settingsManager;
    @FXML
    public MenuBar menuBar;
    @FXML
    public VBox settingsBox;
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
    Label position;
    @FXML
    ImageView processedFrameView;

    private FrameReader<Image> capture;
    private String videoFileName;
    private int frameNumberToShow;

    private Tracker tracker;
    private LineSegmentDetector table;

    private FrameHandler<Image, Mat> frameHandler = new FrameHandler<Image, Mat>() {
        @Override
        public Image process(Mat inputFrame) {
            Mat newFrame = inputFrame.clone();
//            Imgproc.cvtColor(newFrame, newFrame, Imgproc.COLOR_BGR2GRAY);
            Imgproc.resize(newFrame, newFrame, new Size(), 0.6, 0.6, Imgproc.INTER_LINEAR);
            newFrame = table.getFrame(newFrame);
            return Utils.mat2Image(newFrame);
        }
    };

    @FunctionalInterface
    interface Command<T> {
        T execute();
    }

    private Observable<Image> createFrameObservable(Command<Image> command) {
        return Observable.create(subscriber -> {
            subscriber.onNext(command.execute());
            subscriber.onCompleted();
        });
    }

    private Subscription createFrameSubscription(Command<Image> command) {
        return createFrameObservable(command)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(this::showFrame);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Менеджер настроек.
        settingsManager = new SettingsManager();
        updateSettingsFields();

        videoFileName = "";
        Image imageToShow = new Image("com/technostart/playmate/gui/video.png", true);
        processedFrameView.setImage(imageToShow);

        // Инициализация слайдера.
        sliderFrame.valueProperty().addListener((observable, oldValue, newValue) -> {
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
        // Очистка буфера.
        if (capture != null) capture.close();
        // Инициализация ридера.
        CvFrameReader cvReader = new CvFrameReader(videoFileName);
        Mat firstFrame = cvReader.read();
        tracker = new Tracker(firstFrame.size(), 5, 5, 0.5f);
        table = new LineSegmentDetector(firstFrame.size());

        Mat2ImgReader mat2ImgReader = new Mat2ImgReader(cvReader, frameHandler);
        capture = mat2ImgReader;
//        capture = new BufferedFrameReader<>(mat2ImgReader, 10, 400);

        showFrame(capture.read());

        position.textProperty().setValue("1");
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
        createFrameSubscription(() -> capture.next());
    }

    @FXML
    protected void showPreviousFrame() {
        createFrameSubscription(() -> capture.prev());
    }

    @FXML
    private void showCurrentFrame() {
        createFrameSubscription(() -> capture.get(frameNumberToShow));
    }

    @FXML
    private void openSettings(ActionEvent actionEvent) {
        loadSettings();
    }

    @FXML
    private void saveSettings(ActionEvent actionEvent) {
        writeSettings();
    }

    private void loadSettings() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Settings File");
        String settingsFileName = fileChooser.showOpenDialog(null).getAbsolutePath();
        String json = readFile(settingsFileName);
        settingsManager.fromJson(json);
        updateSettingsFields();
    }

    private void writeSettings() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Settings to File");
        File settingsFile = fileChooser.showOpenDialog(null);
        saveTextFile(settingsFile, settingsManager.toJson());
    }

    private void updateSettingsFields() {
        settingsBox.getChildren().clear();
        SettingsFieldCreator.bind(settingsBox, settingsManager);
    }

    private void saveTextFile(File file, String content) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readFile(String path) {
        String string = null;
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            string =  new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return string;
    }
}