package com.technostart.playmate.gui;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.background_subtractor.BackgroundExtractor;
import com.technostart.playmate.core.cv.background_subtractor.BgSubtractorFactory;
import com.technostart.playmate.core.cv.field_detector.FieldDetector;
import com.technostart.playmate.core.cv.field_detector.TableDetector;
import com.technostart.playmate.core.cv.tracker.Hit;
import com.technostart.playmate.core.cv.tracker.HitDetectorFilter;
import com.technostart.playmate.core.cv.tracker.Tracker;
import com.technostart.playmate.core.sessions.Session;
import com.technostart.playmate.core.settings.Cfg;
import com.technostart.playmate.core.settings.SettingsManager;
import com.technostart.playmate.frame_reader.BufferedFrameReader;
import com.technostart.playmate.frame_reader.CvFrameReader;
import com.technostart.playmate.frame_reader.FrameHandler;
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
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

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

    private BufferedFrameReader<Image> capture;
    private String videoFileName;
    private int frameNumberToShow;

    private Tracker tracker;
    private FieldDetector tableDetector;
    private BackgroundExtractor bgSubstr;
    private Session hitMapSession;

    private Hit lastHit;
    private Mat lastFieldMask;
    private List<MatOfPoint> lastFieldContours;
    private Mat hitMap;

    private volatile boolean isFrameButtonEnable = true;

    private FrameHandler<Image, Mat> frameHandler = new FrameHandler<Image, Mat>() {
        @Cfg
        int jpgQuality = 100;
        @Cfg(name = "bgrToGrayConversion")
        boolean isGray = false;
        @Cfg
        double resizeRate = 0.6;
        @Cfg
        boolean isTrackerEnable = false;
        @Cfg
        boolean isFieldDetectorEnable = false;
        @Cfg
        boolean isHitMapSessionEnable = false;
        @Cfg
        boolean isDrawingHitsEnable = false;

        @Override
        public Image process(Mat inputFrame) {
            Mat newFrame = inputFrame.clone();
            if (isGray) {
                Imgproc.cvtColor(newFrame, newFrame, Imgproc.COLOR_BGR2GRAY);
            }
            Imgproc.resize(newFrame, newFrame, new Size(), resizeRate, resizeRate, Imgproc.INTER_LINEAR);
            if (isFieldDetectorEnable) {
                List<MatOfPoint> fieldContours = tableDetector.getContours(newFrame.clone());
                if (fieldContours != null) {
                    lastFieldContours = new ArrayList(fieldContours);
                }
                Imgproc.drawContours(newFrame, lastFieldContours, -1, Palette.GREEN, 2);
            }
            if (isTrackerEnable) {
                newFrame = tracker.getFrame(newFrame);
            }
            if (isHitMapSessionEnable) {
                hitMapSession.update(newFrame.clone());
            }
            if (isDrawingHitsEnable && lastHit != null) {
                Imgproc.circle(newFrame, lastHit.point, 5, Palette.RED);
            }
            if (isDrawingHitsEnable) {
                if (hitMap == null) {
                    hitMap = Mat.zeros(newFrame.size(), newFrame.type());
                }
                Core.addWeighted(newFrame, 0.5, hitMap, 0.5, 0, newFrame);
            }
            return GuiUtils.mat2Image(newFrame, jpgQuality);
        }
    };

    private Observable<Image> createFrameObservable(Supplier<Image> imageSupplier) {
        return Observable.create(subscriber -> {
            if (capture != null) {
                subscriber.onNext(imageSupplier.get());
            }
            subscriber.onCompleted();
        });
    }

    private void createFrameSubscription(Supplier<Image> imageSupplier) {
        if (!isFrameButtonEnable) return;
        disableFrameButtons();
        Executor executor = Executors.newSingleThreadExecutor();
        createFrameObservable(imageSupplier)
                .subscribeOn(Schedulers.from(executor))
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(
                        image -> {
                            showFrame(image);
                            frameSlider.setValue(capture.getCurrentFrameNumber());
                        },
                        throwable -> {
                            System.out.println("Error while processing frame!");
                            throwable.printStackTrace();
                            processedFrameView.requestFocus();
                            enableFrameButtons();
                        },
                        () -> {
                            processedFrameView.requestFocus();
                            enableFrameButtons();
                        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Менеджер настроек.
        settingsManager = new SettingsManager();

        videoFileName = "";
        Image imageToShow = new Image("com/technostart/playmate/gui/video.png", true);
        processedFrameView.setImage(imageToShow);

        // Инициализация слайдера.
        frameSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (capture != null) {
                int frameNumber = capture.getFramesNumber();
                double pos = frameSlider.getValue();
                pos = pos < 0 ? 0 : pos;
                pos = frameNumber <= pos ? frameNumber - 2 : pos;
                frameNumberToShow = (int) pos;
            }
        });
//        bgSubstr = new SimpleBackgroundSubtractor();
        bgSubstr = BgSubtractorFactory.createMOG2(3, 9, false);
        // TODO: добавить новые объекты если будут.
        updateSettingsFromObjects(Arrays.asList(frameHandler, bgSubstr));
    }

    private void showFrame(Image imageToShow) {
        positionLabel.textProperty().setValue(String.valueOf(capture.getCurrentFrameNumber()));
        processedFrameView.setImage(imageToShow);
        processedFrameView.requestFocus();
    }

    @FXML
    protected void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File videoFile = fileChooser.showOpenDialog(null);
        videoFileName = videoFile.getAbsolutePath();
        // Очистка буфера и карты попаданий.
        if (capture != null) capture.close();
        hitMap = null;

        // Инициализация ридера.
        CvFrameReader cvReader = new CvFrameReader(videoFileName);
        Mat firstFrame = cvReader.read();
        tracker = new Tracker(firstFrame.size(), bgSubstr);
        tableDetector = new TableDetector(firstFrame.size());

        Mat2ImgReader mat2ImgReader = new Mat2ImgReader(cvReader, frameHandler);
//        capture = mat2ImgReader;
        capture = new BufferedFrameReader<>(mat2ImgReader);

        // Новая сессия.
        hitMapSession = new Session(firstFrame.size());
        hitMapSession.setHitDetectorListener((hitPoint, direction) -> {
            // TODO: действие при новом попадании.
            lastHit = new Hit(hitPoint, direction);

            int lineType = 1;
            if (lastFieldContours != null && HitDetectorFilter.check(hitPoint, lastFieldContours)) {
                lineType = -1;
            }
            Scalar color = direction == Hit.Direction.LEFT_TO_RIGHT ? Palette.RED : Palette.GREEN;
            Imgproc.circle(hitMap, hitPoint, 5, color, lineType);
//            Imgproc.drawContours(newFrame, fieldContours, -1, Palette.GREEN, 2);
        });

//        hitMap = new Mat(firstFrame.size(), firstFrame.type());

        showFrame(capture.read());

        positionLabel.textProperty().setValue("na");
        // Обновление слайдера.
        frameSlider.setMax(capture.getFramesNumber());

        // Обновляем поля с настройками.
        // TODO: дописать новые объекты если будут.
        updateSettingsFromObjects(Arrays.asList(capture, tracker, tableDetector));
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

        if (event.getCode() == KeyCode.ENTER) {
            processedFrameView.requestFocus();
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

    @FXML
    private void clearBuffer(ActionEvent actionEvent) {
        capture.clear();
    }

    @FXML
    private void reloadFrame() {
        capture.clear();
        createFrameSubscription(() -> capture.get(capture.getCurrentFrameNumber()));
    }

    private void disableFrameButtons() {
        nextFrameBtn.setDisable(true);
        previousFrameBtn.setDisable(true);
        frameSlider.setDisable(true);
        isFrameButtonEnable = false;
    }

    private void enableFrameButtons() {
        nextFrameBtn.setDisable(false);
        previousFrameBtn.setDisable(false);
        frameSlider.setDisable(false);
        isFrameButtonEnable = true;
    }

    /**
     * Применяет настройки.
     */
    @FXML
    private void applySettings() {
        try {
            // TODO: дописать новые объекты если будут.
            tableDetector = settingsManager.fromSettings(tableDetector);
//            tableDetector.updateStructuredElement();
            capture = settingsManager.fromSettings(capture);
            bgSubstr = settingsManager.fromSettings(bgSubstr);
            frameHandler = settingsManager.fromSettings(frameHandler);
        } catch (IllegalAccessException e) {
            // TODO: вывести ошибку.
            System.out.println("Ошибка парсера настроек");
            e.printStackTrace();
        }
    }

    private void reloadTracker() {
        // TODO
    }

    /**
     * Загружает настройки из перечисленных объектов.
     */
    private void updateSettingsFromObjects(List<Object> objects) {
        for (Object object : objects) {
            settingsManager.toSettings(object);
        }
        updateSettingsFields();
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
        SettingsFieldCreator fieldCreator = new SettingsFieldCreator();
        fieldCreator.setOnUpdateListener(this::applySettings);
        fieldCreator.bind(settingsBox, settingsManager);
    }

    private void saveImage(Image image) {

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
            string = new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return string;
    }


}