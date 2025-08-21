package com.pixiv.crawler.util;

import java.io.*;
import java.util.Properties;

/**
 * 简单的设置管理器，用于保存和加载背景设置
 */
public class SettingsManager {
    
    private static final String SETTINGS_FILE = "gui_settings.properties";
    private static final Properties settings = new Properties();
    
    // 设置键名
    public static final String KEY_BACKGROUND_TYPE = "background.type";
    public static final String KEY_BACKGROUND_PATH = "background.path";
    public static final String KEY_BACKGROUND_THEME = "background.theme";
    
    /**
     * 加载设置
     */
    public static void loadSettings() {
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                settings.load(fis);
            } catch (IOException e) {
                System.err.println("加载设置失败: " + e.getMessage());
            }
        } else {
            // 设置默认值
            setDefaultSettings();
        }
    }
    
    /**
     * 保存设置
     */
    public static void saveSettings() {
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            settings.store(fos, "Pixiv Crawler GUI Background Settings");
        } catch (IOException e) {
            System.err.println("保存设置失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置默认值
     */
    private static void setDefaultSettings() {
        settings.setProperty(KEY_BACKGROUND_TYPE, "gradient");
        settings.setProperty(KEY_BACKGROUND_THEME, "default");
        settings.setProperty(KEY_BACKGROUND_PATH, "");
    }
    
    /**
     * 获取字符串设置
     */
    public static String getString(String key, String defaultValue) {
        return settings.getProperty(key, defaultValue);
    }
    
    /**
     * 设置字符串值
     */
    public static void setString(String key, String value) {
        loadSettings();
        settings.setProperty(key, value);
        saveSettings();
    }
    
    /**
     * 获取背景类型
     */
    public static String getBackgroundType() {
        loadSettings();
        return getString(KEY_BACKGROUND_TYPE, "gradient");
    }
    
    /**
     * 设置背景类型
     */
    public static void setBackgroundType(String type) {
        setString(KEY_BACKGROUND_TYPE, type);
    }
    
    /**
     * 获取背景主题
     */
    public static String getBackgroundTheme() {
        loadSettings();
        return getString(KEY_BACKGROUND_THEME, "default");
    }
    
    /**
     * 设置背景主题
     */
    public static void setBackgroundTheme(String theme) {
        setString(KEY_BACKGROUND_THEME, theme);
    }
    
    /**
     * 获取背景图片路径
     */
    public static String getBackgroundPath() {
        loadSettings();
        return getString(KEY_BACKGROUND_PATH, "");
    }
    
    /**
     * 设置背景图片路径
     */
    public static void setBackgroundPath(String path) {
        setString(KEY_BACKGROUND_PATH, path);
    }
}
