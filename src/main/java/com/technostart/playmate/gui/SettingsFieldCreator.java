package com.technostart.playmate.gui;

import com.technostart.playmate.core.cv.settings.Property;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.util.Map;

public class SettingsFieldCreator {
    public SettingsFieldCreator() {
    }

    public static void fill(Pane container, Map<String, Property> propertyMap) {
        for (Map.Entry<String, Property> entry : propertyMap.entrySet()) {
            String key = entry.getKey();
            Property property = entry.getValue();
            HBox propertyBox = new HBox();
            propertyBox.setSpacing(5);
            propertyBox.getChildren().add(new Label(key));
            String type = property.getType();
            if (type.equals(Property.BOOLEAN)) {
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected((boolean) property.getValue());
                propertyBox.getChildren().add(checkBox);
            } else {
                propertyBox.getChildren().add(new TextField(property.getValue().toString()));
            }
            container.getChildren().add(propertyBox);
        }
    }
}
