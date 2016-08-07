package com.technostart.playmate.gui;

import com.technostart.playmate.core.cv.settings.Property;
import com.technostart.playmate.core.cv.settings.SettingsManager;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class SettingsFieldCreator {
    public SettingsFieldCreator() {
    }

    public static void bind(Pane container, SettingsManager settingsManager) {
        Map<String, Property> propertyMap = settingsManager.getProperties();
        for (Map.Entry<String, Property> entry : propertyMap.entrySet()) {
            String key = entry.getKey();
            Property property = entry.getValue();

            HBox propertyBox = new HBox();
            propertyBox.setSpacing(5);
            propertyBox.getChildren().add(new Label(key));
            String type = property.getType();
            // Создание вьюшки для редактирования в зависимости от типа.
            switch (type) {
                case Property.BOOLEAN:
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected((boolean) property.getValue());
                    checkBox.setOnAction(event -> settingsManager.putBoolean(key, checkBox.isSelected()));
                    propertyBox.getChildren().add(checkBox);
                    break;
                case Property.STRING: {
                    TextField textField = new TextField(property.getValue().toString());
                    textField.setOnKeyReleased(event -> settingsManager.putString(key, textField.getText()));
                    propertyBox.getChildren().add(textField);
                    break;
                }
                case Property.DOUBLE: {
                    TextField textField = new TextField(property.getValue().toString());
                    textField.setOnKeyReleased(event
                            -> settingsManager.putDouble(key, Double.parseDouble(textField.getText())));
                    propertyBox.getChildren().add(textField);
                    break;
                }
                case Property.INTEGER: {
                    TextField textField = new TextField(property.getValue().toString());
                    textField.setOnKeyReleased(event
                            -> settingsManager.putInt(key, Integer.parseInt(textField.getText())));
                    propertyBox.getChildren().add(textField);
                    break;
                }
            }
            container.getChildren().add(propertyBox);
        }
    }
}
