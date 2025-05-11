package ui;

import utils.Constants;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SplashScreen extends JWindow {
    private Timer displayTimer;
    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel versionLabel;
    private JPanel contentPanel;
    private MainFrame mainFrame;

    public SplashScreen() {
        initializeComponents();
        setupLayout();
        setSize(400, 300);
        setLocationRelativeTo(null);
        setBackground(new Color(255, 255, 255, 255));
    }

    private void initializeComponents() {
        contentPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                
                int arc = 0;
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                super.paintComponent(g);
            }
        };
        contentPanel.setOpaque(false);

        // 创建图标标签
        iconLabel = new JLabel();
        try {
            BufferedImage icon = UIUtils.loadIcon(Constants.ICON_PATH);
            Image scaledIcon = icon.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaledIcon));
        } catch (Exception e) {
            System.err.println("无法加载启动界面图标: " + e.getMessage());
        }

        // 创建软件名称标签
        nameLabel = new JLabel("Imget");
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 28));
        nameLabel.setForeground(Color.BLACK);
        
        // 创建版本号标签
        versionLabel = new JLabel("v1.6e by Github@landeyucc");
        versionLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        versionLabel.setForeground(Color.BLACK);
    }

    private void setupLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 添加图标
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(iconLabel, gbc);

        // 添加软件名称
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        contentPanel.add(nameLabel, gbc);
        
        // 添加版本号
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(versionLabel, gbc);

        add(contentPanel);
    }

    public void showSplash() {
        setOpacity(1.0f);
        setVisible(true);
        startDisplayTimer();
    }

    private void startDisplayTimer() {
        displayTimer = new Timer(3000, e -> {
            displayTimer.stop();
            dispose();
            showMainFrame();
        });
        displayTimer.setRepeats(false);
        displayTimer.start();
    }

    private void showMainFrame() {
        mainFrame = new MainFrame();
        mainFrame.setVisible(true);
    }
}