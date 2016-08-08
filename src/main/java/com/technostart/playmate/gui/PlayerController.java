package com.technostart.playmate.gui;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.cv.field_detector.LineSegmentDetector;
import com.technostart.playmate.core.cv.settings.SettingsManager;
import com.technostart.playmate.core.cv.tracker.Tracker;
import com.technostart.playmate.frame_reader.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class PlayerController implements Initializable {

    private SettingsManager settingsManager;
    @FXML
    public MenuBar menuBar;
    @FXML
    public VBox settingsBox;
    @FXML
    Pane pane;
    @FXML
    Button nextFrameBtn;
    @FXML
    Button previousFrameBtn;
    @FXML
    Slider frameSlider;
    @FXML
    Label positionLabel;
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
            Imgproc.cvtColor(newFrame, newFrame, Imgproc.COLOR_BGR2GRAY);
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
//                .subscribeOn(Schedulers.computation())
//                .observeOn(Schedulers.io())
                .subscribe(this::showFrame);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // FIXME: перенести в fxml
        settingsBox.setPadding(new Insets(10, 10, 0, 10));
        settingsBox.setSpacing(10);

        // Менеджер настроек.
        settingsManager = new SettingsManager();
        // Загрузка настроек из ресурсов.
        URL url = Resources.getResource("com/technostart/playmate/settings/settings.json");
        try {
            String jsonSettings = Resources.toString(url, Charsets.UTF_8);
            settingsManager.fromJson(jsonSettings);
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateSettingsFields();

        videoFileName = "";
        Image imageToShow = new Image("com/technostart/playmate/gui/video.png", true);
        processedFrameView.setImage(imageToShow);

        // Инициализация слайдера.
        frameSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (capture != null) {
                System.out.print("\nFrame\n");
                int frameNumber = capture.getFramesNumber();
                double pos = frameSlider.getValue() * frameNumber / 1000;
                pos = pos < 0 ? 0 : pos;
                pos = frameNumber <= pos ? frameNumber - 2 : pos;
                frameNumberToShow = (int) pos;
                System.out.print(pos + "\n");
                System.out.print("Slider Value Changed (newValue: " + newValue.intValue() + ")\n");
            }
        });
    }

    private void showFrame(Image imageToShow) {
        positionLabel.textProperty().setValue(String.valueOf(capture.getCurrentFrameNumber()));
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
        capture = new BufferedFrameReader<>(mat2ImgReader, 10, 400);

        showFrame(capture.read());

        positionLabel.textProperty().setValue("1");
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