package src.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class BackgroundPanel extends JPanel {
    private BufferedImage backgroundImage;
    
    public BackgroundPanel() {
        setLayout(new GridBagLayout());
        try {
            backgroundImage = loadBackgroundImage();
        } catch (Exception e) {
            System.out.println("无法加载背景图片: " + e.getMessage());
        }
    }
    
    private BufferedImage loadBackgroundImage() {
        try (InputStream is = getClass().getResourceAsStream("/src/resources/bg.jpg")) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch (IOException e) {
            System.err.println("加载背景图片失败: " + e.getMessage() + "\n路径: " + getClass().getResource("/src/resources/bg.jpg"));
        }
        return null;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            // 计算缩放比例
            double panelWidth = getWidth();
            double panelHeight = getHeight();
            double imageWidth = backgroundImage.getWidth(this);
            double imageHeight = backgroundImage.getHeight(this);
            
            // 计算缩放比例，确保图片至少覆盖整个面板
            double scale = Math.max(panelWidth / imageWidth, panelHeight / imageHeight);
            
            // 计算缩放后的尺寸
            int scaledWidth = (int) (imageWidth * scale);
            int scaledHeight = (int) (imageHeight * scale);
            
            // 计算居中位置
            int x = (int) ((panelWidth - scaledWidth) / 2);
            int y = (int) ((panelHeight - scaledHeight) / 2);
            
            // 绘制图片
            g2d.drawImage(backgroundImage, x, y, scaledWidth, scaledHeight, this);
        }
    }
}