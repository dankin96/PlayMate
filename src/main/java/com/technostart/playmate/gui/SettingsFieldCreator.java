package com.technostart.playmate.gui;

import com.technostart.playmate.core.settings.Property;
import com.technostart.playmate.core.settings.SettingsManager;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class SettingsFieldCreator {
    public SettingsFieldCreator() {
    }

    private OnUpdateListener mListener = new OnUpdateListener() {
        @Override
        public void onUpdate() {

        }
    };

    public void setOnUpdateListener(OnUpdateListener listener) {
        mListener = listener;
    }

    public void bind(Pane container, SettingsManager settingsManager) throws NumberFormatException {
        Map<String, Property> propertyMap = settingsManager.getProperties();
        for (Map.Entry<String, Property> entry : propertyMap.entrySet()) {
            String key = entry.getKey();
            Property property = entry.getValue();

            VBox propertyBox = new VBox();
            propertyBox.setSpacing(5);
            String labelText = String.format("%s (%s)", key, property.getType());
            propertyBox.getChildren().add(new Label(labelText));
            String type = property.getType();
            // Создание вьюшки для редактирования в зависимости от типа.
            switch (type) {
                case Property.BOOLEAN:
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected((boolean) property.getValue());
                    checkBox.setOnAction(event -> {
                        settingsManager.putBoolean(key, checkBox.isSelected());
                        mListener.onUpdate();
                    });
                    propertyBox.getChildren().add(checkBox);
                    break;
                case Property.STRING: {
                    String value = (String) property.getValue();
                    TextField textField = new TextField(value);
                    textField.setOnKeyReleased(event -> {
                        settingsManager.putString(key, textField.getText());
                        mListener.onUpdate();
                    });
                    propertyBox.getChildren().add(textField);
                    break;
                }
                case Property.DOUBLE: {
                    TextField textField = new TextField(property.getValue().toString());
                    textField.setOnKeyReleased(event -> {
                        String text = textField.getText();
                        try {
                            settingsManager.putDouble(key, Double.parseDouble(text));
                            mListener.onUpdate();
                        } catch (NumberFormatException e) {
                            System.out.println(String.format("Невалидное значение в поле %s: \"%s\"", key, text));
                        }
                    });
                    propertyBox.getChildren().add(textField);
                    break;
                }
                case Property.INTEGER: {
                    TextField textField = new TextField(property.getValue().toString());
                    textField.setOnKeyReleased(event -> {
                        String text = textField.getText();
                        try {
                            settingsManager.putInt(key, Integer.parseInt(text));
                            mListener.onUpdate();
                        } catch (NumberFormatException e) {
                            System.out.println(String.format("Невалидное значение в поле %s: \"%s\"", key, text));
                        }
                    });
                    propertyBox.getChildren().add(textField);
                    break;
                }
            }
            container.getChildren().add(propertyBox);
        }
    }

    interface OnUpdateListener {
        void onUpdate();
    }

}
