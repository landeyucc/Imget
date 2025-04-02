package src.utils;

import java.awt.Color;
import java.awt.Font;

public class Constants {
    public static final String ICON_PATH = "/src/resources/Imget.png";

    // 定义颜色主题
    private static Color backgroundColor;
    private static Color componentBgColor;
    private static Color textColor;
    private static Color accentColor;
    private static Color progressColor;
    private static Color hoverColor;
    
    // 定义全局字体
    private static Font normalFont;
    private static Font boldFont;
    private static Font smallFont;
    
    public static Color BACKGROUND_COLOR() {
        if (backgroundColor == null) {
            backgroundColor = new Color(245, 246, 247);
        }
        return backgroundColor;
    }
    
    public static Color COMPONENT_BG_COLOR() {
        if (componentBgColor == null) {
            componentBgColor = new Color(255, 255, 255);
        }
        return componentBgColor;
    }
    
    public static Color TEXT_COLOR() {
        if (textColor == null) {
            textColor = new Color(33, 37, 41);
        }
        return textColor;
    }
    
    public static Color ACCENT_COLOR() {
        if (accentColor == null) {
            accentColor = new Color(206, 206, 255);
        }
        return accentColor;
    }
    
    public static Color PROGRESS_COLOR() {
        if (progressColor == null) {
            progressColor = new Color(40, 167, 69);
        }
        return progressColor;
    }
    
    public static Color HOVER_COLOR() {
        if (hoverColor == null) {
            hoverColor = new Color(135, 206, 235);
        }
        return hoverColor;
    }
    
    public static Font NORMAL_FONT() {
        if (normalFont == null) {
            normalFont = new Font("微软雅黑", Font.PLAIN, 14);
        }
        return normalFont;
    }
    
    public static Font BOLD_FONT() {
        if (boldFont == null) {
            boldFont = new Font("微软雅黑", Font.BOLD, 14);
        }
        return boldFont;
    }
    
    public static Font SMALL_FONT() {
        if (smallFont == null) {
            smallFont = new Font("微软雅黑", Font.PLAIN, 12);
        }
        return smallFont;
    }

    // 版本信息
    public static final String VERSION = "1.1.6(25040216)";
    public static final String AUTHOR = "Lande Yu";
    public static final String WEBSITE = "https://Imget.coldsea.vip";
    public static final String GITHUB_URL = "https://github.com/landeyucc/Imget";
}