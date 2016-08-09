package com.technostart.playmate.core.cv.settings;

import com.technostart.playmate.core.cv.field_detector.TableDetector;
import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;

public class SettingsParserTest {
    @Before
    public void setUp() throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Test
    public void toSettings() throws Exception {
        SettingsParser parser = new SettingsParser();
        SettingsManager manager = new SettingsManager();
        parser.toSettings(TableDetector.class, manager);
    }

}