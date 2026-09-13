package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.JSONObject;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;

import model.ImageDownloader;
import utils.Constants;
import utils.UIUtils;

public class MainFrame extends JFrame {
    private static MainFrame instance;
    
    public static MainFrame getInstance() {
        return instance;
    }
    private JProgressBar totalProgressBar;
    private JLabel totalDownloadCounterLabel;
    private JLabel totalProgressLabel;
    private JLabel retryLabel;
    private JTextField apiUrlField;
    private JTextField downloadCountField;
    private JTextField downloadPathField;
    private JButton downloadButton;
    private ButtonGroup duplicateThresholdGroup;
    private JRadioButton mediumButton;
    private int selectedDuplicateThreshold = 50; // 默认中等级别
    private boolean isVideoMode = false; // 默认图片模式

    public MainFrame() {
        instance = this;
        // 设置FlatLaf主题
        FlatLightLaf.setup();
        // 应用现代扁平化样式
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.trackArc", 8);
        UIManager.put("Button.borderWidth", 2);
        UIManager.put("TitlePane.unifiedBackground", true);
        
        // 初始化窗口和UI
        initializeFrame();
        createUI();
        
        // 加载保存的设置
        loadSavedSettings();
    }

    private void initializeFrame() {
        setTitle("Imget");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Constants.BACKGROUND_COLOR());
        setLocationRelativeTo(null);
        setResizable(false);
        
        // 添加窗口关闭事件监听器
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (ImageDownloader.isDownloading()) {
                    int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                        "当前有下载任务正在进行，是否等待当前下载完成后关闭？",
                        "确认关闭",
                        JOptionPane.YES_NO_OPTION);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        // 创建一个监控线程等待下载完成
                        new Thread(() -> {
                            while (ImageDownloader.isDownloading()) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                            // 下载完成后关闭窗口
                            SwingUtilities.invokeLater(() -> {
                                dispose();
                                System.exit(0);
                            });
                        }).start();
                    } else {
                        dispose();
                        System.exit(0);
                    }
                } else {
                    dispose();
                    System.exit(0);
                }
            }
        });
        
        try {
            setIconImage(UIUtils.loadIcon(Constants.ICON_PATH));
        } catch (Exception e) {
            System.out.println("Icon loading error: " + e.getMessage());
        }
    }

    private void createUI() {
        // 创建背景面板
        BackgroundPanel backgroundPanel = new BackgroundPanel();
        add(backgroundPanel, BorderLayout.CENTER);

        // 创建主面板
        JPanel mainPanel = createMainPanel();
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainGbc.weightx = 1.0;
        mainGbc.weighty = 1.0;
        mainGbc.fill = GridBagConstraints.BOTH;
        backgroundPanel.add(mainPanel, mainGbc);

        // 创建进度面板
        JPanel progressPanel = createProgressPanel();
        GridBagConstraints progressGbc = new GridBagConstraints();
        progressGbc.gridx = 0;
        progressGbc.gridy = 1;
        progressGbc.weightx = 1.0;
        progressGbc.weighty = 0.0;
        progressGbc.fill = GridBagConstraints.HORIZONTAL;
        progressGbc.insets = new Insets(0, 20, 20, 20);
        backgroundPanel.add(progressPanel, progressGbc);

        // 创建关于按钮和选项按钮面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 10));
        topPanel.setOpaque(false);
        
        // 添加关于按钮
        JPanel aboutPanel = createAboutPanel();
        aboutPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        topPanel.add(aboutPanel);
        
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.gridx = 0;
        topGbc.gridy = -1; // 放在最顶部
        topGbc.weightx = 0.0;
        topGbc.weighty = 0.0;
        topGbc.anchor = GridBagConstraints.NORTHEAST;
        topGbc.insets = new Insets(5, 0, 10, 10);
        backgroundPanel.add(topPanel, topGbc);
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 添加标题
        addTitle(mainPanel);

        // 添加输入字段
        addInputFields(mainPanel);

        // 添加重复检测选择按钮
        addDuplicateThresholdButtons(mainPanel);

        // 添加下载按钮
        addDownloadButton(mainPanel);

        return mainPanel;
    }

    private void addTitle(JPanel panel) {
        JLabel titleLabel = UIUtils.createStyledLabel("Imget");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        
        JLabel descriptionLabel = UIUtils.createStyledLabel("一个轻量级的图片API下载器。简易，简约，简单。");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = -1; // 将标题移到最顶部
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 30, 0); // 增加底部间距
        panel.add(titleLabel, gbc);
        
        gbc.gridy = 0; // 描述文字紧跟标题
        gbc.insets = new Insets(60, 0, 40, 0); // 增加与下方参数区域的间距
        panel.add(descriptionLabel, gbc);
    }

    private void addInputFields(JPanel panel) {
        // API URL输入
        JLabel apiUrlLabel = UIUtils.createStyledLabel("请输入随机图片API链接 (或者加载下载记录JSON内的元数据):");
        
        // 创建API URL输入面板
        JPanel apiUrlPanel = new JPanel(new BorderLayout(5, 0));
        apiUrlPanel.setBackground(Constants.BACKGROUND_COLOR());
        apiUrlField = UIUtils.createStyledTextField(520, 35);
        apiUrlField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                JTextField field = (JTextField) input;
                String url = field.getText();
                if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
                    JOptionPane.showMessageDialog(MainFrame.this, "URL必须以http://或https://开头", "API链接数据错误", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return true;
            }
        });
        apiUrlPanel.add(apiUrlField, BorderLayout.CENTER);
        
        // 添加JSON文件选择按钮
        JButton jsonBrowseButton = UIUtils.createStyledButton("加载");
        jsonBrowseButton.setPreferredSize(new Dimension(80, 35));
        jsonBrowseButton.setMinimumSize(new Dimension(80, 35));
        jsonBrowseButton.setMaximumSize(new Dimension(80, 35));
        disableButtons.add(jsonBrowseButton);
        jsonBrowseButton.addActionListener(e -> {
            JFileChooser chooser = getFileChooser(false);
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                try {
                    String fileContent = new String(Files.readAllBytes(selectedFile.toPath()), "UTF-8");
                    fileContent = fileContent.replace("\uFEFF", ""); // 移除BOM头
                    fileContent = fileContent.trim(); // 移除首尾空白字符
                    
                    // 验证JSON格式
                    if (!(fileContent.startsWith("{") && fileContent.endsWith("}")) && 
                        !(fileContent.startsWith("[") && fileContent.endsWith("]")))
                        throw new Exception("JSON读取格式失败，必须以'{'开头并以'}'结尾，或以'['开头并以']'结尾");
                    
                    // 尝试解析JSON，无论是对象还是数组格式
                    org.json.JSONTokener tokener = new org.json.JSONTokener(fileContent);
                    Object jsonObj = tokener.nextValue();
                    String sourceUrl = "";
                    Set<String> md5Set = new HashSet<>();
                    String detectedType = "image";
                    
                    if (jsonObj instanceof JSONObject json) {
                        if (!json.has("source_url"))
                            throw new Exception("JSON中缺少必需的'source_url'字段");
                        sourceUrl = json.getString("source_url");
                        
                        // 检查是否有type字段
                        if (json.has("type")) {
                            detectedType = json.getString("type");
                            System.out.println("检测到type字段: " + detectedType);
                        }
                        
                        // 只收集status为saved的md5值
                        for (String key : json.keySet()) {
    if (key.startsWith("md5")) {
        JSONObject item = json.getJSONObject(key);
        if (item.has("status") && "saved".equals(item.getString("status"))) {
            String md5Value = item.getString("md5");
            md5Set.add(md5Value);
            System.out.println("发现有效MD5记录: " + md5Value);
        }
    }
}
                        
                        // 创建并写入cachemd5list.json文件
                        File downloadDir = new File(downloadPathField.getText());
if (!downloadDir.exists()) {
    downloadDir.mkdirs();
    System.out.println("创建下载目录: " + downloadDir.getAbsolutePath());
}
File cacheFile = new File(downloadDir, "cachemd5list.json");
System.out.println("生成MD5缓存文件路径: " + cacheFile.getAbsolutePath());
                        if (!cacheFile.getParentFile().exists()) {
                            cacheFile.getParentFile().mkdirs();
                        }
                        JSONObject cacheJson = new JSONObject();
                        cacheJson.put("source_url", sourceUrl);
                        cacheJson.put("md5", md5Set);
                        cacheJson.put("type", detectedType);
                        Files.write(cacheFile.toPath(), cacheJson.toString(4).getBytes("UTF-8"));
                        System.out.println("已将源JSON文件中的" + md5Set.size() + "个md5值写入缓存文件：" + cacheFile.getPath());
                        
                        // 将md5集合存储在内存中
                        System.out.println("成功写入" + md5Set.size() + "个有效MD5到缓存文件");
                    } else if (jsonObj instanceof org.json.JSONArray jsonArray) {
                        if (jsonArray.length() > 0) {
                            // 只收集status为saved的md5值，并检测type字段
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject item = jsonArray.getJSONObject(i);
                                if (item.has("md5") && item.has("status") && "saved".equals(item.getString("status"))) {
                                    md5Set.add(item.getString("md5"));
                                }
                                // 检测type字段，以第一个有效记录为准
                                if (i == 0 && item.has("type")) {
                                    detectedType = item.getString("type");
                                    System.out.println("检测到type字段: " + detectedType);
                                }
                            }
                            
                            // 创建并写入cachemd5list.json文件
                            File downloadDir = new File(downloadPathField.getText());
if (!downloadDir.exists()) {
    downloadDir.mkdirs();
    System.out.println("创建下载目录: " + downloadDir.getAbsolutePath());
}
File cacheFile = new File(downloadDir, "cachemd5list.json");
System.out.println("生成MD5缓存文件路径: " + cacheFile.getAbsolutePath());
                            if (!cacheFile.getParentFile().exists()) {
                                cacheFile.getParentFile().mkdirs();
                            }
                            JSONObject cacheJson = new JSONObject();
                            cacheJson.put("md5", md5Set);
                            cacheJson.put("type", detectedType);
                            Files.write(cacheFile.toPath(), cacheJson.toString(4).getBytes("UTF-8"));
                            System.out.println("已将源JSON文件中的" + md5Set.size() + "个md5值写入缓存文件：" + cacheFile.getPath());
                            
                            // 将md5集合存储在内存中
                            System.out.println("成功写入" + md5Set.size() + "个有效MD5到缓存文件");
                            
                            JSONObject firstItem = jsonArray.getJSONObject(0);
                            if (!firstItem.has("source_url"))
                                throw new Exception("JSON数组中缺少必需的'source_url'字段");
                            sourceUrl = firstItem.getString("source_url");
                        } else {
                            throw new Exception("JSON数组为空");
                        }
                    } else {
                        throw new Exception("JSON格式无效");
                    }
                    
                    // 根据检测到的type字段切换模式
                    isVideoMode = "video".equalsIgnoreCase(detectedType);
                    ConfigManager.getInstance().setInt("mode", isVideoMode ? 1 : 0);
                    apiUrlField.setText(sourceUrl);
                    System.out.println("模式已切换为: " + (isVideoMode ? "视频模式" : "图片模式"));
                } catch (Exception ex) {
                    UIUtils.showErrorMessage("读取JSON文件失败：" + ex.getMessage());
                }
            }
        });
        apiUrlPanel.add(jsonBrowseButton, BorderLayout.EAST);
        

        
        // 下载设置面板
        JPanel downloadSettingsPanel = createDownloadSettingsPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 0);  // 添加底部间距
        panel.add(apiUrlLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);  // 增加底部间距
        panel.add(apiUrlPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);  // 重置间距
        panel.add(downloadSettingsPanel, gbc);
    }

    private JPanel createDownloadSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 下载次数
        JLabel downloadCountLabel = UIUtils.createStyledLabel("下载次数:");
        downloadCountField = UIUtils.createStyledTextField(80, 35);
        downloadCountField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                JTextField field = (JTextField) input;
                try {
                    int value = Integer.parseInt(field.getText());
                    if (value < 0) {
                        JOptionPane.showMessageDialog(MainFrame.this, "下载次数不能为负数", "下载次数数据错误", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                    return true;
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(MainFrame.this, "下载次数必须是有效的数值", "下载次数数据错误", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });
        
        // 下载路径
        JLabel downloadPathLabel = UIUtils.createStyledLabel("下载路径:");
        downloadPathField = UIUtils.createStyledTextField(400, 35);
        downloadPathField.setText("Imget_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        
        // 浏览按钮
        JButton browseButton = UIUtils.createStyledButton("浏览");
        browseButton.setPreferredSize(new Dimension(80, 35));
        disableButtons.add(browseButton);
        browseButton.addActionListener(e -> {
            JFileChooser chooser = getFileChooser(true);
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedDirectory = chooser.getSelectedFile();
                downloadPathField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        // 路径输入面板
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.setBackground(Constants.BACKGROUND_COLOR());
        pathPanel.add(downloadPathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);
        pathPanel.setMinimumSize(new Dimension(500, 35));
        pathPanel.setPreferredSize(new Dimension(500, 35));

        // 布局组件 - 下载次数
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(downloadCountLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.1;
        panel.add(downloadCountField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 10, 0, 10);
        panel.add(downloadPathLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.9;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(pathPanel, gbc);

        return panel;
    }

    private void addDuplicateThresholdButtons(JPanel panel) {
        JPanel duplicatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        duplicatePanel.setOpaque(false);
        
        JLabel thresholdLabel = UIUtils.createStyledLabel("重复检测等级:");
        duplicatePanel.add(thresholdLabel);
        
        duplicateThresholdGroup = new ButtonGroup();
        
        JRadioButton lowButton = new JRadioButton("低(20次)");
        mediumButton = new JRadioButton("中(50次)", true);
        JRadioButton highButton = new JRadioButton("高(100次)");
        
        // 设置按钮样式
        Font radioFont = new Font("微软雅黑", Font.PLAIN, 12);
        lowButton.setFont(radioFont);
        mediumButton.setFont(radioFont);
        highButton.setFont(radioFont);
        
        lowButton.setOpaque(false);
        mediumButton.setOpaque(false);
        highButton.setOpaque(false);
        
        // 添加工具提示
        lowButton.setToolTipText("如果下载的文件连续20次md5校验重复则立即停止下载");
        mediumButton.setToolTipText("如果下载的文件连续50次md5校验重复则立即停止下载");
        highButton.setToolTipText("如果下载的文件连续100次md5校验重复则立即停止下载");
        
        
        // 添加事件监听
        lowButton.addActionListener(e -> selectedDuplicateThreshold = 20);
        mediumButton.addActionListener(e -> selectedDuplicateThreshold = 50);
        highButton.addActionListener(e -> selectedDuplicateThreshold = 100);
        
        duplicateThresholdGroup.add(lowButton);
        duplicateThresholdGroup.add(mediumButton);
        duplicateThresholdGroup.add(highButton);
        
        duplicatePanel.add(lowButton);
        duplicatePanel.add(mediumButton);
        duplicatePanel.add(highButton);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        panel.add(duplicatePanel, gbc);
    }

    // 存储需要在下载时禁用的按钮
    private final List<JButton> disableButtons = new ArrayList<>();
    private JButton stopButton;

    public void updateButtonsState(boolean isDownloading) {
        SwingUtilities.invokeLater(() -> {
            for (JButton button : disableButtons) {
                button.setEnabled(!isDownloading);
            }
            Enumeration<AbstractButton> buttons = duplicateThresholdGroup.getElements();
            while (buttons.hasMoreElements()) {
                buttons.nextElement().setEnabled(!isDownloading);
            }
            stopButton.setEnabled(isDownloading);
        });
    }

    private void addDownloadButton(JPanel panel) {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        buttonPanel.setOpaque(false);
        
        downloadButton = UIUtils.createStyledButton("下载");
        stopButton = UIUtils.createStyledButton("停止");
        downloadButton.addActionListener(e -> handleDownload());
        disableButtons.add(downloadButton);
        downloadButton.setPreferredSize(new Dimension(100, 40));
        downloadButton.setMinimumSize(new Dimension(100, 40));
        
        stopButton.setPreferredSize(new Dimension(100, 40));
        stopButton.setMinimumSize(new Dimension(100, 40));
        stopButton.setEnabled(false);
        
        JButton resetButton = UIUtils.createStyledButton("重置");
        resetButton.setPreferredSize(new Dimension(100, 40));
        resetButton.setMinimumSize(new Dimension(100, 40));
        resetButton.addActionListener(e -> {
            apiUrlField.setText("");
            downloadCountField.setText("");
            duplicateThresholdGroup.clearSelection();
            duplicateThresholdGroup.setSelected(mediumButton.getModel(), true);
            selectedDuplicateThreshold = 50;
            downloadPathField.setText("Imget_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        });
        disableButtons.add(resetButton);
        
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> ImageDownloader.setTerminating(true));
        
        buttonPanel.add(downloadButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(stopButton);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 0, 20, 0);
        
        panel.add(buttonPanel, gbc);
    }

    private JPanel createProgressPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        totalProgressBar = UIUtils.createStyledProgressBar();
        totalProgressBar.setPreferredSize(new Dimension(totalProgressBar.getPreferredSize().width, 40));
        totalDownloadCounterLabel = UIUtils.createStyledLabel("总下载进度: 0/0");
        totalProgressLabel = UIUtils.createStyledLabel("总体进度: 0%");

        GridBagConstraints gbc = new GridBagConstraints();
        
        // 添加总进度组件
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 2, 0);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(totalDownloadCounterLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel.add(totalProgressLabel, gbc);

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(totalProgressBar, gbc);

        return panel;
    }

    private JPanel createAboutPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 10));

        retryLabel = UIUtils.createStyledLabel("");
        retryLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        retryLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        panel.add(retryLabel);

        // 添加选项按钮
        JButton optionsButton = UIUtils.createStyledButton("选项");
        optionsButton.setPreferredSize(new Dimension(60, 30));
        optionsButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        optionsButton.addActionListener(e -> showSettingsDialog());
        panel.add(optionsButton);

        // 添加关于按钮
        JButton aboutButton = UIUtils.createStyledButton("关于");
        aboutButton.setPreferredSize(new Dimension(60, 30));
        aboutButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        aboutButton.addActionListener(e -> showAboutDialog());
        panel.add(aboutButton);

        return panel;
    }

    private void handleDownload() {
        if (ImageDownloader.isDownloading()) {
            return;
        }

        if (ImageDownloader.isRetrying()) {
            downloadButton.setText("立即重试");
            downloadButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        }

        if (apiUrlField.getText().isEmpty() || downloadCountField.getText().isEmpty() || downloadPathField.getText().isEmpty()) {
            UIUtils.showErrorMessage("请完全正确的填写完整信息");
            return;
        }
        
        try {
            int count = Integer.parseInt(downloadCountField.getText());
            if (count <= 0) {
                UIUtils.showErrorMessage("下载次数必须大于0");
                return;
            }
            
            // 更新按钮状态
            updateButtonsState(true);
            downloadButton.setText(ImageDownloader.isRetrying() ? "立即重试" : "下载");
            
            String apiUrl = apiUrlField.getText();
            String downloadPath = downloadPathField.getText();
            
            // 创建下载目录
            File downloadDir = new File(downloadPath);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            
            // 开始下载
            ImageDownloader.downloadImages(
                apiUrl,
                count,
                downloadPath,
                totalProgressBar,
                totalDownloadCounterLabel,
                totalProgressLabel,
                selectedDuplicateThreshold,
                isVideoMode
            );
        } catch (NumberFormatException e) {
            UIUtils.showErrorMessage("下载次数必须是一个有效的数值");
        } catch (Exception e) {
            UIUtils.showErrorMessage("启动下载失败：" + e.getMessage());
        }
    }

    private void showAboutDialog() {
        JDialog dialog = new JDialog(this, "关于", true);
        dialog.setLayout(new BorderLayout());
        dialog.setBackground(Constants.BACKGROUND_COLOR());
        
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setBackground(Constants.BACKGROUND_COLOR());
        editorPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        
        String htmlContent = String.format(
            "<html><body style='width: 230px; padding: 0px; margin: 0px;'>" +
            "<div style='font-family: 微软雅黑; text-align: center;'>" +
            "<h2 style='margin: 0;'>Imget</h2>" +
            "<p style='margin: 10px 0;'>版本：%s</p>" +
            "<p style='margin: 10px 0;'>作者：%s</p>" +
            "<p style='margin: 10px 0;'><a href='%s'>程序官网</a>&nbsp;&nbsp;&nbsp;<a href='%s'>GitHub 仓库</a></p>" +
            "</div></body></html>",
            Constants.VERSION, Constants.AUTHOR, Constants.WEBSITE, Constants.GITHUB_URL
        );
        editorPane.setText(htmlContent);

        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    UIUtils.showErrorMessage("无法打开链接：" + ex.getMessage());
                }
            }
        });

        dialog.add(editorPane, BorderLayout.CENTER);
        dialog.setSize(310, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private JFileChooser fileChooser;

    private JFileChooser getFileChooser(boolean directoriesOnly) {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            // 应用现代文件选择器样式
            fileChooser.putClientProperty(FlatClientProperties.STYLE, ""+ 
                "[style]Button.arc=8;" +
                "Component.arc=8;" +
                "ScrollBar.width=10;" +
                "ScrollBar.track=lighten(@background,3%);" +
                "Table.showHorizontalLines=true;" +
                "Table.showVerticalLines=true"
            );
        }
        fileChooser.setFileSelectionMode(directoriesOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        fileChooser.resetChoosableFileFilters();
        if (!directoriesOnly) {
            fileChooser.setDialogTitle("选择JSON文件");
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON文件", "json"));
        } else {
            fileChooser.setDialogTitle("选择下载目录");
            fileChooser.setFileFilter(null);
        }
        return fileChooser;
    }
    
    private void showSettingsDialog() {
        SettingsDialog.getInstance().showDialog();
    }
    
    public void applySettings() {
        ConfigManager config = ConfigManager.getInstance();
        
        // 应用模式设置
        int mode = config.getInt("mode", 0);
        isVideoMode = (mode == 1);
        
        // 应用线程模式设置
        int threadMode = config.getInt("threadMode", 0);
        ImageDownloader.setThreadMode(threadMode);
        
        // 应用请求延迟设置
        int delay = config.getInt("requestDelay", 100);
        ImageDownloader.setRequestDelay(delay);
    }
    
    public void loadSavedSettings() {
        ConfigManager config = ConfigManager.getInstance();
        applySettings();
    }
}