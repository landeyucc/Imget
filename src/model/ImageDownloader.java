package model;

import javax.swing.*;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.json.JSONObject;
import org.json.JSONException;
import java.nio.file.Files;
import java.nio.file.Paths;
import ui.MainFrame;

public class ImageDownloader {
    private static boolean isDownloading = false;
    private static boolean isRetrying = false;
    private static boolean isTerminating = false;
    private static JLabel retryLabel = new JLabel("重试中...");
    private static String detectedImageFormat = null; // 存储检测到的图片格式
    private static volatile Set<String> md5Set = new HashSet<>();
    private static int threadMode = 0; // 0: 普通模式(2线程), 1: 普通加速(16线程), 2: 极限模式(64线程)
    
    public static void setThreadMode(int mode) {
        threadMode = mode;
    }
    
    public static void setTerminating(boolean terminating) {
        isTerminating = terminating;
    }
    
    public static void setMd5Set(Set<String> md5Set) {
        ImageDownloader.md5Set = md5Set;
    }
    
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
            JProgressBar totalProgressBar, JLabel totalDownloadCounterLabel,
            JLabel totalProgressLabel, int duplicateThreshold) {
        isDownloading = true;
        isRetrying = false;
        logEvent("start_download", "total_count", downloadCount, "download_path", downloadPath);

        // 创建共享的imageMap和同步锁
        Map<String, String> imageMap = new LinkedHashMap<>();
        Object mapLock = new Object();
        File folder = new File(downloadPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // 计算线程数量和每个线程需要下载的图片数量
        final int threadCount;
        switch (threadMode) {
            case 1: // 普通加速模式
                threadCount = 16;
                break;
            case 2: // 极限模式
                threadCount = 64;
                break;
            default: // 普通模式
                threadCount = 2;
                break;
        }
        final int imagesPerThread = downloadCount / threadCount;
        final int remainingImages = downloadCount % threadCount;
        
        // 创建共享的进度变量
        final int[] totalDownloaded = {0};
        Object progressLock = new Object();
        
        // 创建并启动下载线程
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            final int startIndex = i * imagesPerThread + Math.min(i, remainingImages);
            final int endIndex = startIndex + imagesPerThread + (i < remainingImages ? 1 : 0);
            
            threads[i] = new Thread(() -> {
                downloadImagesForThread(apiUrl, startIndex, endIndex, downloadPath, imageMap, mapLock,
                    totalProgressBar, totalDownloadCounterLabel, totalProgressLabel,
                    totalDownloaded, progressLock, downloadCount, duplicateThreshold);
            });
            threads[i].start();
        }

        // 创建监控线程状态的SwingWorker
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    File jsonFile = new File("a_image_info.json");
                    if (jsonFile.exists()) {
                        // 读取已有的 JSON 文件
                    }

                    // 等待所有线程完成
                    try {
                        for (Thread thread : threads) {
                            thread.join();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 生成max_info.json，记录总下载数
                    try {
                        JSONObject maxInfo = new JSONObject();
                        maxInfo.put("complete", isTerminating ? "false" : "true");
                        maxInfo.put("apilink", apiUrl);
                        maxInfo.put("maxnumber", String.valueOf(downloadCount));
                        Files.write(Paths.get(downloadPath, "a_max_in.json"), maxInfo.toString(2).getBytes());
                        logEvent("max_info_created", "file", "a_max_in.json");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    SwingUtilities.invokeLater(() -> {
                        totalProgressLabel.setText("下载完成");
                        totalProgressBar.setValue(100);
                        isDownloading = false;
                        isRetrying = false;
                        isTerminating = false;
                        // 更新按钮状态
                        MainFrame.getInstance().updateButtonsState(false);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private static void downloadImagesForThread(String apiUrl, int startIndex, int endIndex,
            String downloadPath, Map<String, String> imageMap, Object mapLock,
            JProgressBar totalProgressBar, JLabel totalDownloadCounterLabel, JLabel totalProgressLabel,
            int[] totalDownloaded, Object progressLock, int totalCount, int duplicateThreshold) {
        int consecutiveDuplicates = 0;
        int fileIndex = 1;
        int baseThreadCount;
        switch (threadMode) {
            case 1: // 普通加速模式
                baseThreadCount = 16;
                break;
            case 2: // 极限模式
                baseThreadCount = 64;
                break;
            default: // 普通模式
                baseThreadCount = 2;
                break;
        }
        String threadPrefix = "thread" + (startIndex / (totalCount / baseThreadCount + 1) + 1) + "_";
        
        boolean isMaxCountReached = false;
        for (int i = startIndex; i < endIndex; i++) {
            final int currentCount = i + 1;
            synchronized (progressLock) {
                totalDownloaded[0]++;
                final int currentTotal = totalDownloaded[0];
                SwingUtilities.invokeLater(() -> {
                    totalDownloadCounterLabel.setText("总下载进度: " + currentTotal + "/" + totalCount);
                    int totalProgress = (int)((currentTotal * 100.0) / totalCount);
                    totalProgressBar.setValue(totalProgress);
                    totalProgressLabel.setText("下载进度百分比: " + totalProgress + "%");
                });
            }

            if (currentCount >= endIndex) {
                isMaxCountReached = true;
            }

            String extension = detectedImageFormat != null ? "." + detectedImageFormat : ".jpg";
            String imageName = downloadPath + "/image_" + threadPrefix + currentCount + extension;
            logEvent("download_start", "file", imageName, "index", currentCount);
            
            try {
                Thread.sleep(100); // 添加短暂延迟，避免请求过于频繁
                if (downloadImage(apiUrl, imageName, totalProgressBar, totalProgressLabel)) {
                    String md5 = calculateMD5(new File(imageName));
                    synchronized (mapLock) {
                        // 优先检测缓存文件
                        File cacheFile = new File(downloadPath, "cachemd5list.json");
                        boolean isDuplicate = false;
                        if (cacheFile.exists()) {
                            try {
                                JSONObject cacheJson = new JSONObject(new String(Files.readAllBytes(cacheFile.toPath()), "UTF-8"));
                                if (cacheJson.getJSONArray("md5").toList().contains(md5)) {
                                    Files.deleteIfExists(Paths.get(imageName));
                                    consecutiveDuplicates++;
                                    isDuplicate = true;
                                    // 将删除的文件信息添加到imageMap，并标记为deleted状态
                                    imageMap.put(imageName, md5);
                                    logEvent("duplicate_file", "file", imageName, "md5", md5, "status", "deleted");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                        // 如果不在缓存中，则检查运行时MD5集合
                        if (!isDuplicate && md5Set.contains(md5)) {
                            logEvent("duplicate_file", "file", imageName, "md5", md5, "status", "deleted");
                            new File(imageName).delete();
                            consecutiveDuplicates++;
                            imageMap.put(imageName, md5);
                        } else if (!isDuplicate) {
                            logEvent("download_success", "file", imageName, "md5", md5);
                            imageMap.put(imageName, md5);
                            consecutiveDuplicates = 0;
                            fileIndex++;
                            md5Set.add(md5);
                        }
                        
                        if (consecutiveDuplicates >= duplicateThreshold || isTerminating) {
                            String terminateReason = isTerminating ? "用户请求终止" : "连续重复次数达到" + duplicateThreshold + "次";
                            logEvent("download_terminated", "reason", terminateReason);
                            SwingUtilities.invokeLater(() -> {
                                totalProgressLabel.setText("已终止：" + terminateReason);
                                totalProgressBar.setValue(100);
                            });
                            // 在终止前写入最后的JSON记录
                            writeToJson(imageMap, downloadPath + "/a_image_info.json", apiUrl);
                            return;
                        }


                        writeToJson(imageMap, downloadPath + "/a_image_info.json", apiUrl);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException | NoSuchAlgorithmException e) {
                logEvent("error", "file", imageName, "error", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static String processImageUrl(String apiUrl) {
        try {
            // 尝试连接API获取响应
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // 读取响应内容
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseStr = response.toString();
            // 检查响应是否为JSON格式
            if (responseStr.trim().startsWith("{")) {
                try {
                    JSONObject json = new JSONObject(responseStr);
                    // 尝试获取imgurl或url字段
                    String imageUrl = null;
                    if (json.has("imgurl")) {
                        imageUrl = json.getString("imgurl");
                    } else if (json.has("url")) {
                        imageUrl = json.getString("url");
                    } else if (json.has("data")) {
                        imageUrl = json.getString("data"); 
                    } else if (json.has("image")) {
                        imageUrl = json.getString("image");
                    } else if (json.has("link")) {
                        imageUrl = json.getString("link"); 
                    } else if (json.has("src")) {
                        imageUrl = json.getString("src"); 
                    } else if (json.has("image_url")) {
                        imageUrl = json.getString("image_url"); 
                    } else if (json.has("acgurl")) {
                        imageUrl = json.getString("acgurl"); 
                    }

                    if (imageUrl != null) {
                        // 处理URL中的转义字符
                        return imageUrl.replace("\\/", "/");
                    }
                } catch (JSONException e) {
                    logEvent("json_parse_error", "error", e.getMessage());
                }
            }
            // 如果不是JSON或没有找到图片URL，直接返回原始URL
            return apiUrl.replace("\\/", "/");
        } catch (IOException e) {
            logEvent("url_process_error", "error", e.getMessage());
            return apiUrl;
        }
    }

    private static boolean downloadImage(String imageUrl, String imageName,
            JProgressBar totalProgressBar, JLabel totalProgressLabel) {
        int maxRetries = 10;
        int retryCount = 0;
        int retryInterval = 5000; // 5秒
        
        while (retryCount < maxRetries) {
            try {
                if (retryCount > 0) {
                    final int currentRetry = retryCount;
                    SwingUtilities.invokeLater(() -> {
                        totalProgressLabel.setText("重试次数: " + currentRetry + "/" + maxRetries);
                    });
                    Thread.sleep(retryInterval);
                }
                
                // 处理图片URL
                String processedUrl = processImageUrl(imageUrl);
                URL url = new URL(processedUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000); // 设置连接超时10秒
                connection.setReadTimeout(10000);    // 设置读取超时10秒

                // 检测图片格式
                if (detectedImageFormat == null) {
                    String contentType = connection.getContentType();
                    if (contentType != null) {
                        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                            detectedImageFormat = "jpg";
                        } else if (contentType.contains("png")) {
                            detectedImageFormat = "png";
                        } else if (contentType.contains("gif")) {
                            detectedImageFormat = "gif";
                        } else if (contentType.contains("webp")) {
                            detectedImageFormat = "webp";
                        } else if (contentType.contains("bmp")) {
                            detectedImageFormat = "bmp";
                        }
                        logEvent("image_format_detected", "format", detectedImageFormat);
                    }
                }
                
                int fileSize = connection.getContentLength();
                InputStream inputStream = new BufferedInputStream(connection.getInputStream(), 8192);
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(imageName), 8192);
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    

                }
                
                outputStream.flush();
                outputStream.close();
                inputStream.close();
                
                SwingUtilities.invokeLater(() -> {
                    if (totalProgressBar.getParent().isAncestorOf(retryLabel)) {
                        totalProgressBar.getParent().remove(retryLabel);
                        totalProgressBar.getParent().revalidate();
                        totalProgressBar.getParent().repaint();
                    }
                    isRetrying = false;
                });
                
                return true;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                retryCount++;
                logEvent("download_retry", "file", imageName, "retry_count", retryCount, "error", e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    totalProgressLabel.setText("连接超时，重试中...");
                    isRetrying = true;
                });
            }
        }
        
        // 达到最大重试次数，更新UI显示下载失败
        SwingUtilities.invokeLater(() -> {
            if (totalProgressBar.getParent().isAncestorOf(retryLabel)) {
                totalProgressBar.getParent().remove(retryLabel);
                totalProgressBar.getParent().revalidate();
                totalProgressBar.getParent().repaint();
            }
            totalProgressLabel.setText("下载失败");
            isRetrying = false;
        });
        logEvent("download_failed", "file", imageName, "max_retries", maxRetries);
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
        Map<String, JsonRecord> existingRecords = new LinkedHashMap<>();
        File jsonFile = new File(jsonFileName);
        
        // Read existing records if file exists
        if (jsonFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(jsonFileName))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                
                // Parse existing JSON
                if (content.length() > 0) {
                    org.json.JSONArray array = new org.json.JSONArray(content.toString());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String name = obj.getString("modified_name");
                        JsonRecord record = new JsonRecord(
                            obj.getString("source_url"),
                            name,
                            obj.getLong("file_size"),
                            obj.getString("md5"),
                            obj.getString("status")
                        );
                        existingRecords.put(name, record);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Update records with new information
        for (Map.Entry<String, String> entry : imageMap.entrySet()) {
            String imagePath = entry.getKey();
            String md5 = entry.getValue();
            File file = new File(imagePath);
            String fileName = file.getName();
            
            JsonRecord record = existingRecords.get(fileName);
            if (record == null || !record.md5.equals(md5)) {
                boolean exists = file.exists();
                long fileSize = exists ? file.length() : 0;
                String status = exists ? "saved" : "deleted";
                existingRecords.put(fileName, new JsonRecord(apiUrl, fileName, fileSize, md5, status));
            }
        }
        
        // Write updated records to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFileName))) {
            writer.write("[\n");
            boolean first = true;
            for (JsonRecord record : existingRecords.values()) {
                if (!first) {
                    writer.write(",\n");
                }
                writer.write("  {\n");
                writer.write("    \"source_url\": \"" + record.sourceUrl + "\",\n");
                writer.write("    \"modified_name\": \"" + record.name + "\",\n");
                writer.write("    \"file_size\": " + record.fileSize + ",\n");
                writer.write("    \"md5\": \"" + record.md5 + "\",\n");
                writer.write("    \"status\": \"" + record.status + "\"\n");
                writer.write("  }");
                first = false;
            }
            writer.write("\n]");
        }
    }
    
    private static class JsonRecord {
        String sourceUrl;
        String name;
        long fileSize;
        String md5;
        String status;
        
        JsonRecord(String sourceUrl, String name, long fileSize, String md5, String status) {
            this.sourceUrl = sourceUrl;
            this.name = name;
            this.fileSize = fileSize;
            this.md5 = md5;
            this.status = status;
        }
    }
    
}