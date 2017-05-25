package com.technostart.playmate.gui;

import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.cv.background_subtractor.BackgroundExtractor;
import com.technostart.playmate.core.cv.background_subtractor.SimpleBackgroundSubtractor;
import com.technostart.playmate.core.cv.field_detector.TableDetector;
import com.technostart.playmate.core.cv.map_of_hits.MapOfHits;
import com.technostart.playmate.core.cv.tracker.Tracker;
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
import rx.Subscription;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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
//    private Mat2ImgReader capture;
    private String videoFileName;
    private int frameNumberToShow;
    private List<Point> pointsForTesting;

    private Tracker tracker;
    private TableDetector tableDetector;
    private BackgroundExtractor bgSubstr;
    private MapOfHits map;
    private int counter = 0;

    private FrameHandler<Image, Mat> frameHandler = new FrameHandler<Image, Mat>() {
        @Cfg
        int jpgQuality = 100;
        @Cfg(name = "bgrToGrayConversion")
        boolean isGray = false;
        @Cfg
        double resizeRate = 0.6;
        @Cfg
        boolean isTrackerEnable = true;
        @Cfg
        boolean isFieldDetectorEnable = false;
        @Cfg
        boolean isWarpPerspectiveEnable = false;
        @Cfg
        boolean isJsonCreateEnable = false;
        @Cfg
        boolean isMapOfHitsEnable = false;

        @Override
        public Image process(Mat inputFrame) {
            Mat newFrame = inputFrame.clone();
            if (isGray) {
                Imgproc.cvtColor(newFrame, newFrame, Imgproc.COLOR_BGR2GRAY);
            }
            Imgproc.resize(newFrame, newFrame, new Size(), resizeRate, resizeRate, Imgproc.INTER_LINEAR);
            if (isFieldDetectorEnable) {
                Mat originalFrame = newFrame.clone();
                newFrame = tableDetector.getField(newFrame);
                Core.addWeighted(newFrame, 0.5, originalFrame, 0.5, 0, newFrame);
            }
            if (isTrackerEnable) {
                tracker.getFrame(newFrame);
            }
            if (isMapOfHitsEnable) {
                Mat originalFrame = newFrame.clone();
                if (originalFrame != null && tableDetector.getIsDetected() != true) {
                    originalFrame = tableDetector.getField(originalFrame);
                    map.setField(tableDetector.getPointsOfTable(), originalFrame);
                }
                if (Math.random() > 0.5) {
                    newFrame = map.getMap(new Point(200 + 150 * Math.random(), 250 + 50 * Math.random()), MapOfHits.Direction.LEFT_TO_RIGHT);
                }
                else
                    newFrame = map.getMap(new Point(500 + 150 * Math.random(), 250 + 40 * Math.random()), MapOfHits.Direction.RIGHT_TO_LEFT);
                counter++;
            }
            if (isJsonCreateEnable) {
                processedFrameView.setOnMouseClicked(e -> {
                    //считывает по 8 координат с ImageView и записывает их в Json с ресурсах
                    if (pointsForTesting.size() <= 8)
                        pointsForTesting.add(new Point(e.getX() / processedFrameView.getFitWidth(), e.getY() / processedFrameView.getFitHeight()));
                    if (pointsForTesting.size() == 8) {
                        GuiUtils.createJsonTestFile(pointsForTesting, videoFileName);
                        pointsForTesting.clear();
                    }
                });
            }
            if (isWarpPerspectiveEnable) {
                List<Point> srcPoints = new ArrayList<Point>();
                //координаты правой половины стола 1 дубля захордкоженные
                Point srcP1 = new Point(403.50083892617465, 241.15520134228194);
                Point srcP2 = new Point(590.7818181818185, 244.85454545454544);
                Point srcP3 = new Point(398.76898763595807, 305.8238356419075);
                Point srcP4 = new Point(693.163064833006, 311.442043222004);
                srcPoints.add(srcP1);
                srcPoints.add(srcP2);
                srcPoints.add(srcP3);
                srcPoints.add(srcP4);
                List<Point> dstPoints = new ArrayList<Point>();
                Point dstP1 = new Point(0, 0);
                Point dstP2 = new Point(newFrame.width() - 1, 0);
                Point dstP3 = new Point(0, newFrame.height() - 1);
                Point dstP4 = new Point(newFrame.width() - 1, newFrame.height() - 1);
                dstPoints.add(dstP1);
                dstPoints.add(dstP2);
                dstPoints.add(dstP3);
                dstPoints.add(dstP4);

                newFrame = Utils.createHomography(newFrame, srcPoints, dstPoints);
            }
            return GuiUtils.mat2Image(newFrame, jpgQuality);
        }
    };


    @FunctionalInterface
    interface Command<T> {
        T execute();
    }

    private Observable<Image> createFrameObservable(Command<Image> command) {
        return Observable.create(subscriber -> {
            if (capture != null) {
                subscriber.onNext(command.execute());
                frameSlider.setValue(capture.getCurrentFrameNumber());
                capture.getCurrentFrameNumber();
            }
            subscriber.onCompleted();
        });
    }

    private Subscription createFrameSubscription(Command<Image> command) {
        return createFrameObservable(command)
                .subscribe(this::showFrame);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Менеджер настроек.
        settingsManager = new SettingsManager();
        videoFileName = "";
        Image imageToShow = new Image("com/technostart/playmate/gui/video.png", true);
        pointsForTesting = new ArrayList<Point>();
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
        bgSubstr = new SimpleBackgroundSubtractor();
        // TODO: добавить новые объекты если будут.
        updateSettingsFromObjects(Arrays.asList(frameHandler, bgSubstr));
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
        tracker = new Tracker(firstFrame.size(), bgSubstr);
        tableDetector = new TableDetector(firstFrame.size());
        map = new MapOfHits();

        Mat2ImgReader mat2ImgReader = new Mat2ImgReader(cvReader, frameHandler);
//        capture = mat2ImgReader;
        capture = new BufferedFrameReader<>(mat2ImgReader);

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
            nextFrameBtn.requestFocus();

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

    /**
     * Применяет настройки.
     */
    @FXML
    private void applySettings() {
        try {
            // TODO: дописать новые объекты если будут.
            tableDetector = settingsManager.fromSettings(tableDetector);
            capture = settingsManager.fromSettings(capture);
            frameHandler = settingsManager.fromSettings(frameHandler);
            bgSubstr = settingsManager.fromSettings(bgSubstr);
        } catch (IllegalAccessException e) {
            // TODO: вывести ошибку.
            System.out.println("Ошибка парсера настроек");
            e.printStackTrace();
        }
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