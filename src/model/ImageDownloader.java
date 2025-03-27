package src.model;

import javax.swing.*;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class ImageDownloader {
    public static void downloadImages(String apiUrl, int downloadCount, String downloadPath,
            JProgressBar progressBar, JLabel downloadCounterLabel, JLabel currentProgressLabel) {
        Map<String, String> imageMap = new HashMap<>();
        File folder = new File(downloadPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        try {
            File jsonFile = new File("image_info.json");
            if (jsonFile.exists()) {
                // 读取已有的 JSON 文件
                // 这里省略具体实现，可根据需求添加
            }

            for (int i = 0; i < downloadCount; i++) {
                final int currentCount = i + 1;
                SwingUtilities.invokeLater(() -> 
                    downloadCounterLabel.setText("当前下载: " + currentCount + "/" + downloadCount));

                String imageName = downloadPath + "/image_" + i + ".jpg";
                if (downloadImage(apiUrl, imageName, progressBar, currentProgressLabel)) {
                    String md5 = calculateMD5(new File(imageName));
                    if (imageMap.containsValue(md5)) {
                        new File(imageName).delete();
                    } else {
                        imageMap.put(imageName, md5);
                        writeToJson(imageMap, "image_info.json");
                    }
                }
            }
    
            // 复制 JSON 文件到下载文件夹
            File sourceJson = new File("image_info.json");
            File targetJson = new File(downloadPath + "/image_info.json");
            if (sourceJson.exists()) {
                try {
                    java.nio.file.Files.copy(sourceJson.toPath(), targetJson.toPath(), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    sourceJson.delete();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException | NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }

    private static boolean downloadImage(String imageUrl, String imageName,
            JProgressBar progressBar, JLabel currentProgressLabel) {
        int maxRetries = 10;
        int retryCount = 0;
        int retryInterval = 5000; // 5秒
        JLabel retryLabel = new JLabel();
        retryLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        while (retryCount <= maxRetries) {
            try {
                if (retryCount > 0) {
                    final int currentRetry = retryCount;
                    SwingUtilities.invokeLater(() -> {
                        if (!progressBar.getParent().isAncestorOf(retryLabel)) {
                            progressBar.getParent().add(retryLabel);
                            progressBar.getParent().revalidate();
                        }
                        retryLabel.setText("重试次数: " + currentRetry + "/" + maxRetries);
                    });
                    Thread.sleep(retryInterval);
                }
                
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                // 先发送HEAD请求检查Content-Type
                connection.setRequestMethod("HEAD");
                String contentType = connection.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    SwingUtilities.invokeLater(() -> {
                        currentProgressLabel.setText("错误: 非图片类型文件 (" + contentType + ")");
                    });
                    return false;
                }
                
                // 重新建立连接用于下载
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                int fileSize = connection.getContentLength();
                InputStream inputStream = connection.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(inputStream);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(imageName);
                
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (fileSize > 0) {
                        final int progress = (int) ((totalBytesRead * 100) / fileSize);
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(progress);
                            currentProgressLabel.setText("当前文件进度: " + progress + "%");
                        });
                    }
                }
                
                fos.close();
                bis.close();
                inputStream.close();
                
                SwingUtilities.invokeLater(() -> {
                    if (progressBar.getParent().isAncestorOf(retryLabel)) {
                        progressBar.getParent().remove(retryLabel);
                        progressBar.getParent().revalidate();
                        progressBar.getParent().repaint();
                    }
                });
                
                return true;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                retryCount++;
                if (retryCount > maxRetries) {
                    SwingUtilities.invokeLater(() -> {
                        if (progressBar.getParent().isAncestorOf(retryLabel)) {
                            progressBar.getParent().remove(retryLabel);
                            progressBar.getParent().revalidate();
                            progressBar.getParent().repaint();
                        }
                    });
                    return false;
                }
            }
        }
        return false;
    }

    private static String calculateMD5(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(file);
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }
        fis.close();
        byte[] bytes = digest.digest();
        BigInteger bigInt = new BigInteger(1, bytes);
        StringBuilder hashText = new StringBuilder(bigInt.toString(16));
        while (hashText.length() < 32) {
            hashText.insert(0, "0");
        }
        return hashText.toString();
    }

    private static void writeToJson(Map<String, String> imageMap, String jsonFileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFileName));
        writer.write("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : imageMap.entrySet()) {
            if (!first) {
                writer.write(",");
            }
            writer.write("\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"");
            first = false;
        }
        writer.write("}");
        writer.close();
    }
}