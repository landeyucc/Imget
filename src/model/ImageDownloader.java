package model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.json.JSONException;
import org.json.JSONObject;

import ui.MainFrame;

public class ImageDownloader {
    private static boolean isDownloading = false;
    private static boolean isRetrying = false;
    private static boolean isTerminating = false;
    private static final JLabel retryLabel = new JLabel("重试中...");
    private static volatile Set<String> md5Set = new HashSet<>();
    private static int threadMode = 0; // 0: 默认模式(2线程), 1: 高速模式(16线程), 2: 极限模式(64线程)
    private static int requestDelay = 100; // 请求延迟（毫秒）
    private static String browserFingerprint = null;
    private static String userAgent = null;
    private static boolean isVideoMode = false; // 当前是否为视频模式
    
    public static void setThreadMode(int mode) {
        threadMode = mode;
    }
    
    public static void setRequestDelay(int delay) {
        requestDelay = delay;
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
    
    private static void generateBrowserIdentity() {
        // 生成随机的Chrome版本号（100-120之间）
        int chromeVersion = 100 + (int)(Math.random() * 20);
        int chromeMinorVersion = (int)(Math.random() * 99);
        int chromeBuild = (int)(Math.random() * 9999);
        
        // 生成随机的浏览器指纹
        browserFingerprint = String.format("\"%s\", \"Chromium\";v=\"%d\", \"Google Chrome\";v=\"%d\"",
            "Not_A Brand;v=99", chromeVersion, chromeVersion);
        
        // 生成随机的User-Agent
        userAgent = String.format("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.%d.%d.0 Safari/537.36",
            chromeVersion, chromeMinorVersion, chromeBuild);
        
        logEvent("browser_identity_generated", "fingerprint", browserFingerprint, "user_agent", userAgent);
    }
    
    private static void logEvent(String event, Object... params) {
        StringBuilder logJson = new StringBuilder();
        logJson.append("{\"event\":\"").append(event).append("\",");
        for (int i = 0; i < params.length; i += 2) {
            if (i > 0) {
                logJson.append(",");
            }
            logJson.append("\"").append(params[i]).append("\":");
            if (params[i + 1] instanceof String) {
                logJson.append("\"").append(params[i + 1]).append("\"");
            } else {
                logJson.append(params[i + 1]);
            }
        }
        logJson.append("}");
        System.out.println(logJson);
    }

    public static void downloadImages(String apiUrl, int downloadCount, String downloadPath,
            JProgressBar totalProgressBar, JLabel totalDownloadCounterLabel,
            JLabel totalProgressLabel, int duplicateThreshold, boolean videoMode) {
        isDownloading = true;
        isRetrying = false;
        isVideoMode = videoMode;
        // 在开始下载前生成浏览器指纹和UA
        generateBrowserIdentity();
        logEvent("start_download", "total_count", downloadCount, "download_path", downloadPath, "mode", videoMode ? "video" : "image");

        // 创建共享的imageMap和同步锁
        Map<String, String> imageMap = new LinkedHashMap<>();
        Object mapLock = new Object();
        File folder = new File(downloadPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // 计算线程数量和每个线程需要下载的图片数量
        final int threadCount = switch (threadMode) {
            case 1 -> 16;  // 高速模式
            case 2 -> 64;  // 极限模式
            default -> 2;  // 默认模式
        };
        final int imagesPerThread = downloadCount / threadCount;
        final int remainingImages = downloadCount % threadCount;
        
        // 创建共享的进度变量
        final int[] totalDownloaded = {0};
        Object progressLock = new Object();
        
        // 创建并启动下载线程
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
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
                    Thread.currentThread().interrupt();
                    logEvent("thread_interrupted", "error", e.getMessage());
                }

                // 生成max_info.json，记录总下载数
                try {
                    JSONObject maxInfo = new JSONObject();
                    maxInfo.put("complete", isTerminating ? "false" : "true");
                    maxInfo.put("apilink", apiUrl);
                    maxInfo.put("maxnumber", String.valueOf(downloadCount));
                    maxInfo.put("browser_fingerprint", browserFingerprint);
                    maxInfo.put("user_agent", userAgent);
                    maxInfo.put("type", isVideoMode ? "video" : "image");
                    Files.write(Paths.get(downloadPath, "a_max_in.json"), maxInfo.toString(2).getBytes());
                    logEvent("max_info_created", "file", "a_max_in.json");
                } catch (IOException e) {
                    logEvent("max_info_write_error", "error", e.getMessage());
                }

                SwingUtilities.invokeLater(() -> {
                    totalProgressLabel.setText("下载完成");
                    totalProgressBar.setValue(100);
                    isDownloading = false;
                    isRetrying = false;
                    isTerminating = false;
                    MainFrame.getInstance().updateButtonsState(false);
                });
                return null;
            }
        }.execute();
    }

    private static void downloadImagesForThread(String apiUrl, int startIndex, int endIndex,
            String downloadPath, Map<String, String> imageMap, Object mapLock,
            JProgressBar totalProgressBar, JLabel totalDownloadCounterLabel, JLabel totalProgressLabel,
            int[] totalDownloaded, Object progressLock, int totalCount, int duplicateThreshold) {
        int consecutiveDuplicates = 0;
        int baseThreadCount = switch (threadMode) {
            case 1 -> 16;  // 高速模式
            case 2 -> 64;  // 极限模式
            default -> 2;  // 默认模式
        };
        String threadPrefix = "thread" + (startIndex / (totalCount / baseThreadCount + 1) + 1) + "_";
        
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

            String extension = isVideoMode ? ".mp4" : ".jpg";
            String prefix = isVideoMode ? "video" : "image";
            String imageName = downloadPath + "/" + prefix + "_" + threadPrefix + currentCount + extension;
            logEvent("download_start", "file", imageName, "index", currentCount);
            
            try {
                if (isTerminating) {
                    return;
                }
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
                                    imageMap.put(imageName, md5);
                                    logEvent("duplicate_file", "file", imageName, "md5", md5, "status", "deleted");
                                }
                            } catch (IOException | JSONException e) {
                                logEvent("cache_read_error", "error", e.getMessage());
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
                            md5Set.add(md5);
                        }
                        
                        if (consecutiveDuplicates >= duplicateThreshold || isTerminating) {
                            String terminateReason = isTerminating ? "用户请求终止" : "连续重复次数达到" + duplicateThreshold + "次";
                            logEvent("download_terminated", "reason", terminateReason);
                            SwingUtilities.invokeLater(() -> {
                                totalProgressLabel.setText("已终止：" + terminateReason);
                                totalProgressBar.setValue(100);
                            });
                            writeToJson(imageMap, downloadPath + "/a_image_info.json", apiUrl);
                            return;
                        }

                        writeToJson(imageMap, downloadPath + "/a_image_info.json", apiUrl);
                    }
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                logEvent("download_error", "file", imageName, "error", e.getMessage());
            }
            
            // 应用请求延迟
            if (requestDelay > 0) {
                try {
                    Thread.sleep(requestDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static String processImageUrl(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String responseStr = response.toString();
                if (responseStr.trim().startsWith("{")) {
                    try {
                        JSONObject json = new JSONObject(responseStr);
                        String mediaUrl = null;
                        
                        if (isVideoMode) {
                            if (json.has("video_url")) {
                                mediaUrl = json.getString("video_url");
                            } else if (json.has("video")) {
                                mediaUrl = json.getString("video");
                            } else if (json.has("vid")) {
                                mediaUrl = json.getString("vid");
                            } else if (json.has("video_src")) {
                                mediaUrl = json.getString("video_src");
                            } else if (json.has("mp4")) {
                                mediaUrl = json.getString("mp4");
                            } else if (json.has("flv")) {
                                mediaUrl = json.getString("flv");
                            } else if (json.has("webm")) {
                                mediaUrl = json.getString("webm");
                            }
                        }
                        
                        if (mediaUrl == null) {
                            if (json.has("imgurl")) {
                                mediaUrl = json.getString("imgurl");
                            } else if (json.has("url")) {
                                mediaUrl = json.getString("url");
                            } else if (json.has("data")) {
                                mediaUrl = json.getString("data"); 
                            } else if (json.has("image")) {
                                mediaUrl = json.getString("image");
                            } else if (json.has("link")) {
                                mediaUrl = json.getString("link"); 
                            } else if (json.has("src")) {
                                mediaUrl = json.getString("src"); 
                            } else if (json.has("image_url")) {
                                mediaUrl = json.getString("image_url"); 
                            } else if (json.has("acgurl")) {
                                mediaUrl = json.getString("acgurl"); 
                            } 
                        }

                        if (mediaUrl != null) {
                            return mediaUrl.replace("\\/", "/");
                        }
                    } catch (JSONException e) {
                        logEvent("json_parse_error", "error", e.getMessage());
                    }
                }
                return apiUrl.replace("\\/", "/");
            }
        } catch (java.net.MalformedURLException e) {
            logEvent("url_format_error", "error", e.getMessage());
            return apiUrl;
        } catch (IOException e) {
            logEvent("url_process_error", "error", e.getMessage());
            return apiUrl;
        }
    }

    private static boolean downloadImage(String imageUrl, String imageName,
            JProgressBar totalProgressBar, JLabel totalProgressLabel) {
        int maxRetries = 10;
        int retryCount = 0;
        long retryInterval = 5000L;
        
        while (retryCount < maxRetries) {
            if (isTerminating) {
                return false;
            }
            
            if (retryCount > 0) {
                final int currentRetry = retryCount;
                SwingUtilities.invokeLater(() -> {
                    totalProgressLabel.setText("重试次数: " + currentRetry + "/" + maxRetries);
                });
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < retryInterval && !isTerminating) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
                if (isTerminating) {
                    return false;
                }
            }
            
            try {
                String processedUrl = processImageUrl(imageUrl);
                URL url = new URL(processedUrl);
                
                try (InputStream inputStream = new BufferedInputStream(url.openStream(), 8192);
                     BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(imageName), 8192)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1 && !isTerminating) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    
                    outputStream.flush();
                }
                
                SwingUtilities.invokeLater(() -> {
                    if (totalProgressBar.getParent().isAncestorOf(retryLabel)) {
                        totalProgressBar.getParent().remove(retryLabel);
                        totalProgressBar.getParent().revalidate();
                        totalProgressBar.getParent().repaint();
                    }
                    isRetrying = false;
                });
                
                return true;
            } catch (java.net.MalformedURLException e) {
                logEvent("url_format_error", "file", imageName, "error", e.getMessage());
                return false;
            } catch (IOException e) {
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
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
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
        
        // 如果文件存在，则读取现有记录
        if (jsonFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(jsonFileName))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                
                // 解析现有的JSON
                if (content.length() > 0) {
                    org.json.JSONArray array = new org.json.JSONArray(content.toString());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String name = obj.getString("modified_name");
                        // 读取type字段，如果不存在则默认为image
                        String type = obj.has("type") ? obj.getString("type") : "image";
                        JsonRecord record = new JsonRecord(
                            obj.getString("source_url"),
                            name,
                            obj.getLong("file_size"),
                            obj.getString("md5"),
                            obj.getString("status"),
                            type
                        );
                        existingRecords.put(name, record);
                    }
                }
            } catch (IOException | JSONException e) {
                logEvent("json_read_error", "error", e.getMessage());
            }
        }
        
        // 用新的信息更新记录
        String currentType = isVideoMode ? "video" : "image";
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
                existingRecords.put(fileName, new JsonRecord(apiUrl, fileName, fileSize, md5, status, currentType));
            }
        }
        
        // 将更新的记录写入文件
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
                writer.write("    \"status\": \"" + record.status + "\",\n");
                writer.write("    \"type\": \"" + record.type + "\"\n");
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
        String type; // "image" 或 "video"
        
        JsonRecord(String sourceUrl, String name, long fileSize, String md5, String status, String type) {
            this.sourceUrl = sourceUrl;
            this.name = name;
            this.fileSize = fileSize;
            this.md5 = md5;
            this.status = status;
            this.type = type;
        }
    }
    
}