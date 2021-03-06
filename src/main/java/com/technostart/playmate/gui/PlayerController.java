package com.technostart.playmate.gui;


import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.cv.background_subtractor.BackgroundExtractor;
import com.technostart.playmate.core.cv.background_subtractor.BgSubtractorFactory;
import com.technostart.playmate.core.cv.field_detector.FieldDetector;
import com.technostart.playmate.core.cv.field_detector.TableDetector;
import com.technostart.playmate.core.cv.tracker.*;
import com.technostart.playmate.core.model.field.Table;
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
import org.opencv.utils.Converters;
import org.opencv.video.Video;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class PlayerController implements Initializable, RawTrackerInterface, HitDetectorInterface {

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
    private OpticalFlow opticalFlow = new OpticalFlow();

    private Mat firstFrame;
    private Mat lastFrame;

    private List<Hit> innerHitList = new ArrayList<>();
    private List<Hit> outerHitList = new ArrayList<>();
    private Mat lastFieldMask;
    private List<MatOfPoint> lastFieldContours = new ArrayList<>();
    private Mat hitMap;
    private Mat outerHitMap;
    private Hit lastHit;
    private double speed;
    private List<Point> homoPoint = new ArrayList<>();
    private Mat perspectiveTransform;

    private List<Point> tablePointList = new ArrayList<>();

    private Map<Integer, List<List<MatOfPoint>>> contourGropus;
    private Mat contourGroupsMat;

    private List<Long> timestampList = new ArrayList<>();
    private long lastTimestamp;

    private double polygonTestDistance = 5;

    private volatile boolean isFrameButtonEnable = true;

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
        boolean isDrawingHitsEnable = true;
        @Cfg
        int hitRadius = 5;
        @Cfg
        boolean isDrawAllHitsEnable = false;
        @Cfg
        boolean isDrawTableContourEnable = false;
        @Cfg
        boolean isPrintSpeedEnable = false;
        @Cfg
        int trackLength = 5;
        @Cfg
        boolean isMockTimeEnable = false;
        @Cfg
        boolean isOpticalFlowEnable = true;


        @Override
        public Image process(Mat inputFrame) {
            if (isMockTimeEnable) {
                lastTimestamp += 33.333;
            } else {
                lastTimestamp = System.currentTimeMillis();
            }
            timestampList.add(lastTimestamp);
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
            }
            if (isDrawTableContourEnable) {
                if (lastFieldContours != null) {
                    Imgproc.drawContours(newFrame, lastFieldContours, -1, Palette.GREEN, 2);
                }
            }
            if (isOpticalFlowEnable) {
                newFrame = opticalFlow.getFrame(newFrame);
            }
            if (isTrackerEnable) {
                int lastIdx = timestampList.size();
                int diff = lastIdx - trackLength;
                int fromIdx = diff >= 0 ? diff : 0;
                newFrame = tracker.getFrame(lastTimestamp, newFrame, timestampList.subList(fromIdx, lastIdx));
            }
            if (isDrawingHitsEnable) {
                for (Hit hit : innerHitList) {
                    Scalar color = (hit.direction == Hit.Direction.LEFT_TO_RIGHT) ? Palette.RED : Palette.GREEN;
                    Imgproc.circle(newFrame, hit.point, hitRadius, color, -1);
                }
                if (isDrawAllHitsEnable) {
                    for (Hit hit : outerHitList) {
                        Scalar color = (hit.direction == Hit.Direction.LEFT_TO_RIGHT) ? Palette.RED : Palette.GREEN;
                        Imgproc.circle(newFrame, hit.point, hitRadius, color, 1);
                    }
                }
            }
            if (isPrintSpeedEnable) {
                Imgproc.putText(newFrame, String.format("Speed: %.2f m/s", speed), new Point(10, 20), Core.FONT_HERSHEY_PLAIN, 1.5, Palette.WHITE);
            }
            Image img = GuiUtils.mat2Image(newFrame, jpgQuality);
            if (img == null) {
                new Image("com/technostart/playmate/gui/video.png", true);
            }
            lastFrame = newFrame.clone();
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
//        bgSubstr = new SimpleBackgroundSubtractor();
        // TODO: добавить новые объекты если будут.
        updateSettingsFromObjects(Arrays.asList(frameHandler));
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
        firstFrame = cvReader.read();
        Mat2ImgReader mat2ImgReader = new Mat2ImgReader(cvReader, frameHandler);
//        capture = mat2ImgReader;
        capture = new BufferedFrameReader<>(mat2ImgReader);

        initDetectors();

        showFrame(capture.read());

        positionLabel.textProperty().setValue("na");
        // Обновление слайдера.
        frameSlider.setMax(capture.getFramesNumber());

        // Обновляем поля с настройками.
        // TODO: дописать новые объекты если будут.
        updateSettingsFromObjects(Arrays.asList(capture, tracker, tableDetector, new Utils()));
    }

    @Override
    public void onHitDetect(Hit hit) {
        if (lastFieldContours == null) return;
        if (HitDetectorFilter.check(hit.point, lastFieldContours, polygonTestDistance)) {
            innerHitList.add(hit);
            // Считаем скорость.
            if (lastHit != null) {
                if (lastHit.direction == hit.direction) {
                    double newSpeed = calcSpeed(lastHit, hit);
                    speed = newSpeed > 0 ? newSpeed : speed;
                    System.out.println(String.format("speed: %.2f м/c", speed));
                }
            }
            lastHit = hit;
        } else {
            outerHitList.add(hit);
        }
    }

    @FXML
    private void initDetectors() {
        int threshold = settingsManager.getInt("bgThreshold", 300);
        bgSubstr = BgSubtractorFactory.createMOG2(3, threshold, true);
//        bgSubstr = new ColorBackgroundSubtractor();
        tracker = new Tracker(firstFrame.size(), bgSubstr);
        tracker.setHitDetectorListener(this);
        tableDetector = new TableDetector(firstFrame.size());
        opticalFlow = new OpticalFlow();
        opticalFlow.setBgSubstr(bgSubstr);
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
            tracker = settingsManager.fromSettings(tracker);
            int history = settingsManager.getInt("bgHistoryLength", 5);
            int threshold = settingsManager.getInt("bgThreshold", 300);
            bgSubstr = BgSubtractorFactory.createMOG2(history, threshold, true);
            // ColorBgExtr
/*            String lowerBString = settingsManager.getString("lowerColor", "0, 0, 0");
            String upperBString = settingsManager.getString("upperColor", "255, 255, 255");
            Scalar lowerB = GuiUtils.str2scalar(lowerBString);
            Scalar upperB = GuiUtils.str2scalar(upperBString);
            bgSubstr = new ColorBackgroundSubtractor(lowerB, upperB);*/

            tracker.setBgSubstr(bgSubstr);
            opticalFlow.setBgSubstr(bgSubstr);
            Utils.setKernelRate(settingsManager.getInt("kernelRate", Utils.DEFAULT_KERNEL_RATE));
            polygonTestDistance = settingsManager.getDouble("polygonTestDistance", 5);

        } catch (IllegalAccessException e) {
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
            settingsManager.putInt("bgHistoryLength", 5);
            settingsManager.putInt("bgThreshold", 300);
            settingsManager.putDouble("polygonTestDistance", 5);
            // ColorBg
            settingsManager.putString("lowerColor", "0, 0, 0");
            settingsManager.putString("upperColor", "255, 255, 255");
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

    @FXML
    private void clearMap() {
        innerHitList.clear();
        outerHitList.clear();
    }

    @FXML
    private void recordTablePoints() {
        processedFrameView.setOnMouseClicked(event -> {
            double x = (event.getX() / processedFrameView.getFitWidth()) * lastFrame.width();
            double y = (event.getY() / processedFrameView.getFitHeight()) * lastFrame.height();

            tablePointList.add(new Point(x, y));
            System.out.println(String.format("x: %.2f y: %.2f", x, y));
            if (tablePointList.size() == 8) {
                lastFieldContours.clear();
                MatOfPoint leftContour = new MatOfPoint();
                leftContour.fromList(tablePointList.subList(0, 4));
                MatOfPoint rightContour = new MatOfPoint();
                rightContour.fromList(tablePointList.subList(4, 8));
                lastFieldContours.add(leftContour);
                lastFieldContours.add(rightContour);
                homoPoint.add(tablePointList.get(0));
                homoPoint.add(tablePointList.get(1));
                homoPoint.add(tablePointList.get(6));
                homoPoint.add(tablePointList.get(7));

                perspectiveTransform = Imgproc.getPerspectiveTransform(Converters.vector_Point2f_to_Mat(homoPoint),
                        Converters.vector_Point2f_to_Mat(Table.getBorderPoint()));
            }
        });
    }

    @FXML
    private void clearTablePoints() {
        tablePointList.clear();
    }

    @Override
    public void onTrackContour(int groupId, List<MatOfPoint> newContours) {
        List<List<MatOfPoint>> contours;
        if (contourGropus.containsKey(groupId)) {
            contours = contourGropus.get(groupId);
            contours.add(newContours);
        } else {
            contours = new ArrayList<>();
        }
        contourGropus.put(groupId, contours);
    }

    private double calcSpeed(Hit hit1, Hit hit2) {
        List<Point> points = new ArrayList<>();
        points.add(hit1.point);
        points.add(hit2.point);
        List<Point> homoPoints = getHomoPoints(points);
        double distance = Utils.getDistance(homoPoints.get(0), homoPoints.get(1));
        double timeDiff = hit2.timestamp - hit1.timestamp;
        return Math.abs(1000 * distance / timeDiff);
    }

    public List<Point> getHomoPoints(List<Point> inputPoints) {
        Mat transformedPoints = new Mat();
        List<Point> newCoords = new ArrayList<>();
        Core.perspectiveTransform(Converters.vector_Point2f_to_Mat(inputPoints), transformedPoints, perspectiveTransform);
        Converters.Mat_to_vector_Point2f(transformedPoints, newCoords);
        return newCoords;
    }

    @FXML
    private void clearSpeed(ActionEvent actionEvent) {
        speed = 0;
        lastHit = null;
    }
}