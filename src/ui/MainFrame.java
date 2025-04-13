package ui;

import utils.Constants;
import utils.UIUtils;
import model.ImageDownloader;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONObject;
import java.nio.file.Files;
import java.util.Set;
import java.util.HashSet;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainFrame extends JFrame {
    private JProgressBar progressBar1;
    private JProgressBar progressBar2;
    private JLabel downloadCounterLabel1;
    private JLabel downloadCounterLabel2;
    private JLabel currentProgressLabel1;
    private JLabel currentProgressLabel2;
    private JLabel retryLabel;
    private JTextField apiUrlField;
    private JTextField downloadCountField;
    private JTextField downloadPathField;
    private JButton downloadButton;
    private ButtonGroup duplicateThresholdGroup;
    private int selectedDuplicateThreshold = 50; // 默认中等级别

    public MainFrame() {
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
        
        SwingUtilities.invokeLater(() -> {
            initializeFrame();
            createUI();
            setVisible(true);
        });
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
                                    e.printStackTrace();
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

        // 创建关于按钮面板
        JPanel aboutPanel = createAboutPanel();
        GridBagConstraints aboutGbc = new GridBagConstraints();
        aboutGbc.gridx = 0;
        aboutGbc.gridy = -1; // 放在最顶部
        aboutGbc.weightx = 0.0;
        aboutGbc.weighty = 0.0;
        aboutGbc.anchor = GridBagConstraints.NORTHEAST;
        aboutGbc.insets = new Insets(5, 0, 10, 10);
        backgroundPanel.add(aboutPanel, aboutGbc);
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
        JLabel apiUrlLabel = UIUtils.createStyledLabel("请输入随机图片 API 链接:");
        
        // 创建API URL输入面板
        JPanel apiUrlPanel = new JPanel(new BorderLayout(5, 0));
        apiUrlPanel.setBackground(Constants.BACKGROUND_COLOR());
        apiUrlField = UIUtils.createStyledTextField(520, 35);
        apiUrlPanel.add(apiUrlField, BorderLayout.CENTER);
        
        // 添加JSON文件选择按钮
        JButton jsonBrowseButton = UIUtils.createStyledButton("加载");
        jsonBrowseButton.setPreferredSize(new Dimension(80, 35));
        jsonBrowseButton.setMinimumSize(new Dimension(80, 35));
        jsonBrowseButton.setMaximumSize(new Dimension(80, 35));
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
                        throw new Exception("JSON格式无效，必须以'{'开头并以'}'结尾，或以'['开头并以']'结尾");
                    
                    // 尝试解析JSON，无论是对象还是数组格式
                    Object jsonObj = new org.json.JSONTokener(fileContent).nextValue();
                    String sourceUrl = "";
                    Set<String> md5Set = new HashSet<>();
                    
                    if (jsonObj instanceof JSONObject) {
                        JSONObject json = (JSONObject) jsonObj;
                        if (!json.has("source_url"))
                            throw new Exception("JSON中缺少必需的'source_url'字段");
                        sourceUrl = json.getString("source_url");
                        
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
                        Files.write(cacheFile.toPath(), cacheJson.toString(4).getBytes("UTF-8"));
                        System.out.println("已将源JSON文件中的" + md5Set.size() + "个md5值写入缓存文件：" + cacheFile.getPath());
                        
                        // 将md5集合存储在内存中
                        System.out.println("成功写入" + md5Set.size() + "个有效MD5到缓存文件");
                    } else if (jsonObj instanceof org.json.JSONArray) {
                        org.json.JSONArray jsonArray = (org.json.JSONArray) jsonObj;
                        if (jsonArray.length() > 0) {
                            // 只收集status为saved的md5值
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject item = jsonArray.getJSONObject(i);
                                if (item.has("md5") && item.has("status") && "saved".equals(item.getString("status"))) {
                                    md5Set.add(item.getString("md5"));
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
                    
                    apiUrlField.setText(sourceUrl);
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
        
        // 下载路径
        JLabel downloadPathLabel = UIUtils.createStyledLabel("下载路径:");
        downloadPathField = UIUtils.createStyledTextField(400, 35);
        downloadPathField.setText("Imget_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        
        // 浏览按钮
        JButton browseButton = UIUtils.createStyledButton("浏览");
        browseButton.setPreferredSize(new Dimension(80, 35));
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

        // 布局组件
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
        JRadioButton mediumButton = new JRadioButton("中(50次)", true);
        JRadioButton highButton = new JRadioButton("高(100次)");
        
        // 设置按钮样式
        Font radioFont = new Font("微软雅黑", Font.PLAIN, 12);
        lowButton.setFont(radioFont);
        mediumButton.setFont(radioFont);
        highButton.setFont(radioFont);
        
        lowButton.setOpaque(false);
        mediumButton.setOpaque(false);
        highButton.setOpaque(false);
        
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

    private void addDownloadButton(JPanel panel) {
        downloadButton = UIUtils.createStyledButton("下载");
        downloadButton.addActionListener(e -> handleDownload());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 0, 20, 0);
        
        downloadButton.setPreferredSize(new Dimension(200, 40));
        downloadButton.setMinimumSize(new Dimension(200, 40));
        downloadButton.setMaximumSize(new Dimension(200, 40));
        
        panel.add(downloadButton, gbc);
    }

    private JPanel createProgressPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        progressBar1 = UIUtils.createStyledProgressBar();
        progressBar1.setPreferredSize(new Dimension(progressBar1.getPreferredSize().width, 40));
        progressBar2 = UIUtils.createStyledProgressBar();
        progressBar2.setPreferredSize(new Dimension(progressBar2.getPreferredSize().width, 40));
        downloadCounterLabel1 = UIUtils.createStyledLabel("线程1下载: 0/0");
        downloadCounterLabel2 = UIUtils.createStyledLabel("线程2下载: 0/0");
        currentProgressLabel1 = UIUtils.createStyledLabel("线程1进度: 0%");
        currentProgressLabel2 = UIUtils.createStyledLabel("线程2进度: 0%");

        GridBagConstraints gbc = new GridBagConstraints();
        
        // 线程1的组件
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 2, 0);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(downloadCounterLabel1, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel.add(currentProgressLabel1, gbc);

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(progressBar1, gbc);

        // 线程2的组件
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 2, 0);
        panel.add(downloadCounterLabel2, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel.add(currentProgressLabel2, gbc);

        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(progressBar2, gbc);

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

        JButton aboutButton = UIUtils.createStyledButton("关于");
        aboutButton.setPreferredSize(new Dimension(30, 30));
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
            
            downloadButton.setEnabled(!ImageDownloader.isDownloading());
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
                progressBar1,
                progressBar2,
                downloadCounterLabel1,
                downloadCounterLabel2,
                currentProgressLabel1,
                currentProgressLabel2,
                selectedDuplicateThreshold
            );
        } catch (NumberFormatException e) {
            UIUtils.showErrorMessage("下载次数必须是一个有效的数字");
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
        dialog.setSize(300, 200);
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
        if (!directoriesOnly) {
            fileChooser.setDialogTitle("选择JSON文件");
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON文件", "json"));
        } else {
            fileChooser.setDialogTitle("选择下载目录");
            fileChooser.setFileFilter(null);
        }
        return fileChooser;
    }
}