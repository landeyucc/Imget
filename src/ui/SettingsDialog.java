package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import utils.Constants;
import utils.UIUtils;

public class SettingsDialog extends JDialog {
    private static SettingsDialog instance;
    
    // 设置选项
    private JRadioButton imageModeButton;
    private JRadioButton videoModeButton;
    private JTextField requestDelayField;
    private ButtonGroup threadModeGroup;
    private JRadioButton normalModeButton;
    private JRadioButton fastModeButton;
    private JRadioButton extremeModeButton;
    
    // 当前设置值
    private int currentMode = 0; // 0=图片, 1=视频
    private int currentThreadMode = 0; // 0=默认, 1=高速, 2=极限
    private int currentDelay = 100;
    
    public static SettingsDialog getInstance() {
        if (instance == null) {
            instance = new SettingsDialog();
        }
        return instance;
    }
    
    public SettingsDialog() {
        super(MainFrame.getInstance(), "设置选项", true);
        instance = this;
        initializeDialog();
        createUI();
        loadSettings();
        pack();
        setLocationRelativeTo(getOwner());
    }
    
    private void initializeDialog() {
        setSize(450, 400);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Constants.BACKGROUND_COLOR());
    }
    
    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(true);
        mainPanel.setBackground(Constants.BACKGROUND_COLOR());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 创建标题
        JLabel titleLabel = UIUtils.createStyledLabel("下载设置");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 创建设置面板
        JPanel settingsPanel = createSettingsPanel();
        mainPanel.add(settingsPanel, BorderLayout.CENTER);
        
        // 创建按钮面板
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int row = 0;
        
        // 模式选择部分
        JLabel modeLabel = UIUtils.createStyledLabel("下载模式");
        modeLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(modeLabel, gbc);
        
        row++;
        
        // 图片模式单选按钮
        imageModeButton = new JRadioButton("图片模式");
        imageModeButton.setOpaque(false);
        imageModeButton.setForeground(Constants.TEXT_COLOR());
        imageModeButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        imageModeButton.setToolTipText("下载图片文件");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        panel.add(imageModeButton, gbc);
        
        // 视频模式单选按钮
        videoModeButton = new JRadioButton("视频模式");
        videoModeButton.setOpaque(false);
        videoModeButton.setForeground(Constants.TEXT_COLOR());
        videoModeButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        videoModeButton.setToolTipText("下载视频文件");
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        panel.add(videoModeButton, gbc);
        
        // 创建模式按钮组
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(imageModeButton);
        modeGroup.add(videoModeButton);
        
        row++;
        
        // 分隔线
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 8, 15, 8);
        panel.add(createSeparator(), gbc);
        gbc.insets = new Insets(8, 8, 8, 8);
        
        row++;
        
        // 线程模式部分
        JLabel threadModeLabel = UIUtils.createStyledLabel("下载线程");
        threadModeLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(threadModeLabel, gbc);
        
        row++;
        
        // 默认模式
        normalModeButton = new JRadioButton("默认模式（2线程）");
        normalModeButton.setOpaque(false);
        normalModeButton.setForeground(Constants.TEXT_COLOR());
        normalModeButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        normalModeButton.setToolTipText("使用2个线程，适合网络较差或服务器限制严格的情况");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(normalModeButton, gbc);
        
        row++;
        
        // 高速模式
        fastModeButton = new JRadioButton("高速模式（16线程）");
        fastModeButton.setOpaque(false);
        fastModeButton.setForeground(Constants.TEXT_COLOR());
        fastModeButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        fastModeButton.setToolTipText("使用16个线程，适合网络良好且服务器限制宽松的情况");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(fastModeButton, gbc);
        
        row++;
        
        // 极限模式
        extremeModeButton = new JRadioButton("极限模式（64线程）");
        extremeModeButton.setOpaque(false);
        extremeModeButton.setForeground(Constants.TEXT_COLOR());
        extremeModeButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        extremeModeButton.setToolTipText("使用64个线程，适合网络极佳且服务器无限制的情况");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(extremeModeButton, gbc);
        
        // 创建线程模式按钮组
        threadModeGroup = new ButtonGroup();
        threadModeGroup.add(normalModeButton);
        threadModeGroup.add(fastModeButton);
        threadModeGroup.add(extremeModeButton);
        
        row++;
        
        // 分隔线
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 8, 15, 8);
        panel.add(createSeparator(), gbc);
        gbc.insets = new Insets(8, 8, 8, 8);
        
        row++;
        
        // 请求延迟部分
        JLabel delayLabel = UIUtils.createStyledLabel("请求延迟");
        delayLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(delayLabel, gbc);
        
        row++;
        
        JLabel delayHintLabel = UIUtils.createStyledLabel("每个线程在任务间等待的时间（毫秒）");
        delayHintLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        delayHintLabel.setForeground(new java.awt.Color(120, 120, 120));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(delayHintLabel, gbc);
        
        row++;
        
        JPanel delayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        delayPanel.setOpaque(false);
        
        requestDelayField = UIUtils.createStyledTextField(80, 25);
        requestDelayField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        requestDelayField.setToolTipText("请求延迟时间（毫秒）");
        delayPanel.add(requestDelayField);
        
        JLabel msLabel = UIUtils.createStyledLabel("ms");
        msLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        delayPanel.add(msLabel);
        
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(delayPanel, gbc);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setOpaque(false);
        
        // 取消按钮
        JButton cancelButton = UIUtils.createStyledButton("取消");
        cancelButton.addActionListener(e -> dispose());
        panel.add(cancelButton);
        
        // 保存按钮
        JButton saveButton = UIUtils.createStyledButton("保存");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
                dispose();
            }
        });
        panel.add(saveButton);
        
        return panel;
    }
    
    private JLabel createSeparator() {
        JLabel separator = new JLabel();
        separator.setPreferredSize(new Dimension(400, 1));
        separator.setBackground(new java.awt.Color(200, 200, 200));
        separator.setOpaque(true);
        return separator;
    }
    
    private void loadSettings() {
        ConfigManager config = ConfigManager.getInstance();
        
        // 加载模式设置
        int mode = config.getInt("mode", 0);
        currentMode = mode;
        if (mode == 1) {
            videoModeButton.setSelected(true);
        } else {
            imageModeButton.setSelected(true);
        }
        
        // 加载线程模式
        int threadMode = config.getInt("threadMode", 0);
        currentThreadMode = threadMode;
        switch (threadMode) {
            case 1:
                fastModeButton.setSelected(true);
                break;
            case 2:
                extremeModeButton.setSelected(true);
                break;
            default:
                normalModeButton.setSelected(true);
                break;
        }
        
        // 加载请求延迟
        int delay = config.getInt("requestDelay", 100);
        currentDelay = delay;
        requestDelayField.setText(String.valueOf(delay));
    }
    
    private void saveSettings() {
        ConfigManager config = ConfigManager.getInstance();
        
        // 保存模式设置
        int mode = imageModeButton.isSelected() ? 0 : 1;
        config.setInt("mode", mode);
        currentMode = mode;
        
        // 保存线程模式
        int threadMode = 0;
        if (fastModeButton.isSelected()) {
            threadMode = 1;
        } else if (extremeModeButton.isSelected()) {
            threadMode = 2;
        }
        config.setInt("threadMode", threadMode);
        currentThreadMode = threadMode;
        
        // 保存请求延迟
        try {
            int delay = Integer.parseInt(requestDelayField.getText());
            if (delay < 0) delay = 0;
            config.setInt("requestDelay", delay);
            currentDelay = delay;
        } catch (NumberFormatException e) {
            config.setInt("requestDelay", 100);
            currentDelay = 100;
        }
        
        // 刷新配置
        config.save();
        
        // 通知主窗口更新设置
        SwingUtilities.invokeLater(() -> {
            MainFrame.getInstance().applySettings();
        });
    }
    
    public int getCurrentMode() {
        return currentMode;
    }
    
    public int getCurrentThreadMode() {
        return currentThreadMode;
    }
    
    public int getCurrentDelay() {
        return currentDelay;
    }
    
    public void showDialog() {
        loadSettings();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }
}
