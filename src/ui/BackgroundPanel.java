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
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return loadBackgroundImage();
            }
            
            @Override
            protected void done() {
                try {
                    backgroundImage = get();
                    repaint();
                } catch (Exception e) {
                    System.out.println("无法加载背景图片: " + e.getMessage());
                }
            }
        }.execute();
    }
    
    private BufferedImage loadBackgroundImage() throws IOException {
        // 尝试从文件加载
        File imageFile = new File("bg.jpg");
        if (imageFile.exists()) {
            return ImageIO.read(imageFile);
        } else {
            // 尝试从类路径加载
            InputStream is = getClass().getResourceAsStream("/bg.jpg");
            if (is != null) {
                try {
                    return ImageIO.read(is);
                } finally {
                    is.close();
                }
            }
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