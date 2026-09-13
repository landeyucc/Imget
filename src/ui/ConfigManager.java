package ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private String configFilePath;
    private Map<String, String> configMap;
    private boolean modified = false;
    
    private ConfigManager() {
        configMap = new HashMap<>();
        // 配置文件放在程序根目录
        String userDir = System.getProperty("user.dir");
        configFilePath = userDir + File.separator + "config.ini";
        load();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    public void load() {
        File configFile = new File(configFilePath);
        
        if (!configFile.exists()) {
            System.out.println("配置文件不存在，将使用默认设置");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            String currentSection = "";
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 跳过空行和注释
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                
                // 解析节
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    continue;
                }
                
                // 解析键值对
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    
                    // 添加节前缀以支持节功能（可选）
                    if (!currentSection.isEmpty()) {
                        key = currentSection + "." + key;
                    }
                    
                    configMap.put(key, value);
                }
            }
            
            System.out.println("配置文件加载成功: " + configFilePath);
        } catch (IOException e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
        }
    }
    
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFilePath))) {
            // 写入文件头
            writer.write("# Imget 配置文件");
            writer.newLine();
            writer.write("# 格式: 键=值");
            writer.newLine();
            writer.newLine();
            
            // 写入设置节
            writer.write("[Settings]");
            writer.newLine();
            
            // 写入所有配置
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
            
            modified = false;
            System.out.println("配置文件保存成功: " + configFilePath);
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }
    
    public String getString(String key, String defaultValue) {
        String value = configMap.get(key);
        return value != null ? value : defaultValue;
    }
    
    public int getInt(String key, int defaultValue) {
        String value = configMap.get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("配置项 " + key + " 不是有效的整数，使用默认值: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = configMap.get(key);
        if (value != null) {
            return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("1") 
                   || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on");
        }
        return defaultValue;
    }
    
    public void setString(String key, String value) {
        configMap.put(key, value);
        modified = true;
    }
    
    public void setInt(String key, int value) {
        configMap.put(key, String.valueOf(value));
        modified = true;
    }
    
    public void setBoolean(String key, boolean value) {
        configMap.put(key, String.valueOf(value));
        modified = true;
    }
    
    public boolean isModified() {
        return modified;
    }
    
    public String getConfigFilePath() {
        return configFilePath;
    }
}
