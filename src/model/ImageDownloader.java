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
    private static boolean isDownloading = false;
    private static boolean isRetrying = false;
    
    public static boolean isDownloading() {
        return isDownloading;
    }
    
    public static boolean isRetrying() {
        return isRetrying;
    }
    
    private static void logEvent(String event, Object... params) {
        StringBuilder logJson = new StringBuilder();
        logJson.append("{\"event\":\"" + event + "\",");
        for (int i = 0; i < params.length; i += 2) {
            if (i > 0) logJson.append(",");
            logJson.append("\"" + params[i] + "\":");
            if (params[i + 1] instanceof String) {
                logJson.append("\"" + params[i + 1] + "\"");
            } else {
                logJson.append(params[i + 1]);
            }
        }
        logJson.append("}");
        System.out.println(logJson);
    }

    public static void downloadImages(String apiUrl, int downloadCount, String downloadPath,
            JProgressBar progressBar, JLabel downloadCounterLabel, JLabel currentProgressLabel) {
        isDownloading = true;
        isRetrying = false;
        logEvent("start_download", "total_count", downloadCount, "download_path", downloadPath);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Map<String, String> imageMap = new HashMap<>();
                File folder = new File(downloadPath);
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                try {
                    File jsonFile = new File("image_info.json");
                    if (jsonFile.exists()) {
                        // 读取已有的 JSON 文件
                    }

            for (int i = 0; i < downloadCount; i++) {
                final int currentCount = i + 1;
                SwingUtilities.invokeLater(() -> 
                    downloadCounterLabel.setText("当前下载: " + currentCount + "/" + downloadCount));

                String imageName = downloadPath + "/image_" + i + ".jpg";
                logEvent("download_start", "file", imageName, "index", currentCount);
                Thread.sleep(100); // 添加短暂延迟，避免请求过于频繁
                if (downloadImage(apiUrl, imageName, progressBar, currentProgressLabel)) {
                    String md5 = calculateMD5(new File(imageName));
                    if (imageMap.containsValue(md5)) {
                        logEvent("duplicate_file", "file", imageName, "md5", md5, "status", "deleted");
                        new File(imageName).delete();
                        System.out.println(String.format("{\"event\":\"file_status\",\"file\":\"%s\",\"status\":\"deleted\",\"reason\":\"duplicate\"}", new File(imageName).getName()));
                    } else {
                        logEvent("download_success", "file", imageName, "md5", md5);
                        imageMap.put(imageName, md5);
                        writeToJson(imageMap, "image_info.json", apiUrl);
                    }
                }
            }
    
            // 复制 JSON 文件到下载文件夹
            File sourceJson = new File("image_info.json");
            File targetJson = new File(downloadPath + "/image_info.json");
            if (sourceJson.exists()) {
                try {
                    // 使用异步方式复制文件
                    SwingWorker<Void, Void> copyWorker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            java.nio.file.Files.copy(sourceJson.toPath(), targetJson.toPath(), 
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            sourceJson.delete();
                            return null;
                        }
                    };
                    copyWorker.execute();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            
                    SwingUtilities.invokeLater(() -> {
                        currentProgressLabel.setText("下载完成");
                        progressBar.setValue(100);
                        isDownloading = false;
                        isRetrying = false;
                    });
                } catch (IOException | NoSuchAlgorithmException ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        }.execute();
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
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000); // 设置连接超时10秒
                connection.setReadTimeout(10000);    // 设置读取超时10秒
                
                int fileSize = connection.getContentLength();
                InputStream inputStream = new BufferedInputStream(connection.getInputStream(), 8192);
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(imageName), 8192);
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (fileSize > 0) {
                        final int progress = (int) ((totalBytesRead * 100) / fileSize);
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(progress);
                            currentProgressLabel.setText("当前文件进度: " + progress + "%");
                        });
                    }
                }
                
                outputStream.flush();
                outputStream.close();
                inputStream.close();
                
                SwingUtilities.invokeLater(() -> {
                    if (progressBar.getParent().isAncestorOf(retryLabel)) {
                        progressBar.getParent().remove(retryLabel);
                        progressBar.getParent().revalidate();
                        progressBar.getParent().repaint();
                    }
                    isRetrying = false;
                });
                
                return true;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                retryCount++;
                logEvent("download_retry", "file", imageName, "retry_count", retryCount, "error", e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    currentProgressLabel.setText("连接超时");
                    isRetrying = true;
                });
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

    private static void writeToJson(Map<String, String> imageMap, String jsonFileName, String apiUrl) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFileName));
        writer.write("[\n");
        boolean first = true;
        for (Map.Entry<String, String> entry : imageMap.entrySet()) {
            if (!first) {
                writer.write(",\n");
            }
            File file = new File(entry.getKey());
            writer.write("  {\n");
            writer.write("    \"source_url\": \"" + apiUrl + "\",\n");
            writer.write("    \"modified_name\": \"" + file.getName() + "\",\n");
            writer.write("    \"file_size\": " + file.length() + ",\n");
            writer.write("    \"md5\": \"" + entry.getValue() + "\",\n");
            writer.write("    \"status\": \"saved\"\n");
            writer.write("  }");
            first = false;
            System.out.println(String.format("{\"event\":\"file_status\",\"file\":\"%s\",\"status\":\"saved\",\"size\":%d}", file.getName(), file.length()));
        }
        writer.write("\n]");
        writer.close();
    }
}