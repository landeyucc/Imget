package utils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

public class UIUtils {
    public static JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Constants.TEXT_COLOR());
        label.setFont(Constants.NORMAL_FONT());
        return label;
    }

    public static Border createStyledBorder() {
        return BorderFactory.createEmptyBorder(5, 10, 5, 10);
    }

    public static JTextField createStyledTextField(int width, int height) {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(width, height));
        field.setBackground(Constants.COMPONENT_BG_COLOR());
        field.setForeground(Constants.TEXT_COLOR());
        field.setCaretColor(Constants.TEXT_COLOR());
        field.setBorder(createStyledBorder());
        field.setFont(Constants.NORMAL_FONT());
        return field;
    }

    public static JProgressBar createStyledProgressBar() {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setPreferredSize(new Dimension(400, 25));
        bar.setStringPainted(true);
        bar.setBackground(Constants.COMPONENT_BG_COLOR());
        bar.setForeground(Constants.PROGRESS_COLOR());
        bar.setFont(Constants.SMALL_FONT());
        bar.setBorderPainted(false);
        bar.setBorder(createStyledBorder());
        return bar;
    }

    public static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(0, 0, 0, 0)); // 透明背景
        button.setForeground(Constants.TEXT_COLOR());
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Constants.ACCENT_COLOR(), 2),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        button.setFont(Constants.BOLD_FONT());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(Constants.HOVER_COLOR());
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Constants.HOVER_COLOR(), 2),
                        BorderFactory.createEmptyBorder(5, 15, 5, 15)
                    ));
                } else {
                    button.setBackground(Constants.COMPONENT_BG_COLOR());
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(Constants.ACCENT_COLOR());
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Constants.ACCENT_COLOR(), 2),
                        BorderFactory.createEmptyBorder(5, 15, 5, 15)
                    ));
                } else {
                    button.setBackground(Constants.COMPONENT_BG_COLOR());
                }            }
        });
        
        return button;
    }

    public static void setUIColors() {
        UIManager.put("Panel.background", Constants.BACKGROUND_COLOR());
        UIManager.put("ProgressBar.selectionBackground", Constants.TEXT_COLOR());
        UIManager.put("ProgressBar.selectionForeground", Constants.TEXT_COLOR());
        UIManager.put("TextField.selectionBackground", Constants.ACCENT_COLOR());
        UIManager.put("TextField.selectionForeground", Color.WHITE);
    }

    public static void setUIFont() {
        UIManager.put("Label.font", Constants.NORMAL_FONT());
        UIManager.put("TextField.font", Constants.NORMAL_FONT());
        UIManager.put("Button.font", Constants.BOLD_FONT());
        UIManager.put("ProgressBar.font", Constants.SMALL_FONT());
        UIManager.put("OptionPane.messageFont", Constants.NORMAL_FONT());
        UIManager.put("OptionPane.buttonFont", Constants.NORMAL_FONT());
        UIManager.put("FileChooser.font", Constants.NORMAL_FONT());
    }

    public static void showErrorMessage(String message) {
        // 设置错误对话框的样式
        UIManager.put("OptionPane.background", Constants.BACKGROUND_COLOR());
        UIManager.put("OptionPane.messageForeground", Constants.TEXT_COLOR());
        UIManager.put("Panel.background", Constants.BACKGROUND_COLOR());
        UIManager.put("Button.background", Constants.ACCENT_COLOR());
        UIManager.put("Button.foreground", Constants.TEXT_COLOR());
        UIManager.put("Button.select", Constants.HOVER_COLOR());
        UIManager.put("Button.focus", Constants.ACCENT_COLOR());
        UIManager.put("Button.border", BorderFactory.createEmptyBorder(5, 15, 5, 15));
        
        JOptionPane.showMessageDialog(null, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    private static BufferedImage cachedIcon = null;
    
    public static BufferedImage loadIcon(String iconPath) throws IOException {
        if (cachedIcon != null) {
            return cachedIcon;
        }
        
        BufferedImage image = null;
        java.net.URL iconUrl = UIUtils.class.getResource(iconPath);
        
        if (iconUrl != null) {
            try {
                image = ImageIO.read(iconUrl);
                System.out.println("Icon loading: " + iconPath);
            } catch (IOException ex) {
                System.err.println("Icon loading error: " + ex.getMessage());
                cachedIcon = null; // 重置缓存
                throw ex;
            }
        } else {
            System.err.println("Icon file not found: " + iconPath);
            throw new IOException("Icon loading error: " + iconPath + "not found");
        }
        
        cachedIcon = image;
        return image;
    }
}