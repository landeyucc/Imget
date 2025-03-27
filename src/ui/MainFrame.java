package src.ui;

import src.utils.Constants;
import src.utils.UIUtils;
import src.model.ImageDownloader;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainFrame extends JFrame {
    private JProgressBar progressBar;
    private JLabel downloadCounterLabel;
    private JLabel currentProgressLabel;
    private JTextField apiUrlField;
    private JTextField downloadCountField;
    private JTextField downloadPathField;
    private JButton downloadButton;

    public MainFrame() {
        initializeFrame();
        createUI();
    }

    private void initializeFrame() {
        setTitle("Imget");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Constants.BACKGROUND_COLOR);
        setLocationRelativeTo(null);
        setResizable(false);
        
        try {
            setIconImage(UIUtils.loadIcon("Imget.png"));
        } catch (Exception e) {
            System.out.println("无法加载图标: " + e.getMessage());
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
        aboutGbc.insets = new Insets(5, 0, 0, 10);
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

        // 添加下载按钮
        addDownloadButton(mainPanel);

        return mainPanel;
    }

    private void addTitle(JPanel panel) {
        JLabel titleLabel = new JLabel("Imget");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        titleLabel.setForeground(Constants.TEXT_COLOR);
        
        JLabel descriptionLabel = new JLabel("一个高效的图片api下载器。简易，简约，简单。");
        descriptionLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        descriptionLabel.setForeground(Constants.TEXT_COLOR);

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
        apiUrlField = UIUtils.createStyledTextField(727, 35);
        
        // 下载设置面板
        JPanel downloadSettingsPanel = createDownloadSettingsPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 5, 0);  // 添加底部间距
        panel.add(apiUrlLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 20, 0);  // 增加底部间距
        panel.add(apiUrlField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
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
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("选择下载目录");
            
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                String selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
                downloadPathField.setText(selectedPath + File.separator + 
                    "Imget_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            }
        });

        // 路径输入面板
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.setBackground(Constants.BACKGROUND_COLOR);
        pathPanel.add(downloadPathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);

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

    private void addDownloadButton(JPanel panel) {
        downloadButton = UIUtils.createStyledButton("下载");
        downloadButton.addActionListener(e -> handleDownload());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 15, 20, 15);
        
        downloadButton.setPreferredSize(new Dimension(200, 40));
        downloadButton.setMinimumSize(new Dimension(200, 40));
        downloadButton.setMaximumSize(new Dimension(200, 40));
        
        panel.add(downloadButton, gbc);
    }

    private JPanel createProgressPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        progressBar = UIUtils.createStyledProgressBar();
        downloadCounterLabel = UIUtils.createStyledLabel("当前下载: 0/0");
        currentProgressLabel = UIUtils.createStyledLabel("当前文件进度: 0%");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 2, 0);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(downloadCounterLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel.add(currentProgressLabel, gbc);

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(progressBar, gbc);

        return panel;
    }

    private JPanel createAboutPanel() {
        JButton aboutButton = UIUtils.createStyledButton("关于");
        aboutButton.setPreferredSize(new Dimension(30, 30));
        aboutButton.setFont(Constants.SMALL_FONT);
        aboutButton.addActionListener(e -> showAboutDialog());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 10));
        panel.add(aboutButton);

        return panel;
    }

    private void handleDownload() {
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
            
            downloadButton.setEnabled(false);
            String apiUrl = apiUrlField.getText();
            String downloadPath = downloadPathField.getText();
            
            new Thread(() -> {
                try {
                    ImageDownloader.downloadImages(apiUrl, count, downloadPath, 
                        progressBar, downloadCounterLabel, currentProgressLabel);
                } finally {
                    SwingUtilities.invokeLater(() -> downloadButton.setEnabled(true));
                }
            }).start();
        } catch (NumberFormatException ex) {
            UIUtils.showErrorMessage("请输入有效的下载次数");
        }
    }

    private void showAboutDialog() {
        JDialog dialog = new JDialog(this, "关于 Imget", true);
        dialog.setLayout(new BorderLayout());
        
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setBackground(Constants.BACKGROUND_COLOR);
        
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
}