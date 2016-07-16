package com.technostart.playmate.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.opencv.core.Core;

public class Player extends Application {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Player.fxml"));
            Pane rootElement = (Pane) loader.load();
            Scene scene = new Scene(rootElement, 600, 700);
            scene.setFill(Color.BLACK);
            primaryStage.setWidth(1400);
            primaryStage.setHeight(1400);
            primaryStage.sizeToScene();
            primaryStage.setTitle("Player");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}