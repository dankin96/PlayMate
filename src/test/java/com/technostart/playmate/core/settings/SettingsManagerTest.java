package com.technostart.playmate.core.settings;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SettingsManagerTest {
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void intFromJson() throws Exception {
        String json = "{\"i1\":{\"type\":\"Integer\",\"value\":1},\"i2\":{\"type\":\"Integer\",\"value\":2}}";
        SettingsManager manager = new SettingsManager();
        manager.fromJson(json);
        assertEquals(manager.getInt("i1"), 1);
        assertEquals(manager.getInt("i2"), 2);
    }

    @Test
    public void boolFromJson() throws Exception {
        String json = "{\"b1\":{\"type\":\"Boolean\",\"value\":true}}";
        SettingsManager manager = new SettingsManager();
        manager.fromJson(json);
        assertEquals(manager.getBoolean("b1"), true);
    }

    @Test
    public void stringFromJson() throws Exception {
        String json = "{\"s1\":{\"type\":\"String\",\"value\":\"hello\"}}";
        SettingsManager manager = new SettingsManager();
        manager.fromJson(json);
        assertEquals(manager.getString("s1"), "hello");
    }

    @Test
    public void toJson() throws Exception {
        SettingsManager manager = new SettingsManager();
        manager.mockProperty();
        String json = manager.toJson();
        System.out.println(json);
    }
}