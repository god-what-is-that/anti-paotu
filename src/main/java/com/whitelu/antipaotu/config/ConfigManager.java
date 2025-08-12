package com.whitelu.antipaotu.config;

import com.whitelu.antipaotu.AntiPaotuPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * 配置管理器类
 * 用于加载和管理插件配置

 */
public class ConfigManager {
    
    private final AntiPaotuPlugin plugin;
    private FileConfiguration config;
    

    private int timeWindow;
    private boolean heightFilterEnabled;
    private int heightThreshold;
    private int cooldownSeconds;
    private int continuousThreshold;
    private int banDurationMinutes;
    private boolean debugEnabled;
    private boolean debugVerbose;
    
    // 新增配置项
    private boolean disableDetectionInWater;
    private int dimensionSwitchCooldownSeconds;

    private boolean oneBotEnabled;
    private String oneBotWebSocketUrl;
    private String oneBotAccessToken;
    private List<Long> oneBotGroupIds;
    private List<Long> oneBotPrivateIds;
    private int oneBotConnectTimeout;
    
    public ConfigManager(AntiPaotuPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 加载配置文件
     * 
     * @return 是否加载成功
     */
    public boolean loadConfig() {
        try {

            plugin.saveDefaultConfig();
            

            plugin.reloadConfig();
            this.config = plugin.getConfig();
            

            cacheConfigValues();
            

            if (!validateConfig()) {
                plugin.getLogger().severe("配置文件验证失败！");
                return false;
            }

            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("加载配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 重载配置文件
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        cacheConfigValues();
        
        if (!validateConfig()) {
            plugin.getLogger().warning("重载的配置文件验证失败！");
        }
        
        plugin.getLogger().info("配置文件已重载");
    }
    
    /**
     * 缓存配置值
     */
    private void cacheConfigValues() {
        this.timeWindow = config.getInt("detection.time-window", 30);
        this.heightFilterEnabled = config.getBoolean("detection.height-filter-enabled", false);
        this.heightThreshold = config.getInt("detection.height-threshold", 200);
        this.cooldownSeconds = config.getInt("detection.cooldown-seconds", 3);
        this.continuousThreshold = config.getInt("detection.continuous-threshold", 5);
        this.banDurationMinutes = config.getInt("ban.duration-minutes", 10);
        this.debugEnabled = config.getBoolean("debug.enabled", false);
        this.debugVerbose = config.getBoolean("debug.verbose", false);
        
        // 新增配置项
        this.disableDetectionInWater = config.getBoolean("detection.disable-detection-in-water", true);
        this.dimensionSwitchCooldownSeconds = config.getInt("detection.dimension-switch-cooldown-seconds", 30);

        this.oneBotEnabled = config.getBoolean("onebot.enabled", false);
        this.oneBotWebSocketUrl = config.getString("onebot.websocket-url", "ws://localhost:6700");
        this.oneBotAccessToken = config.getString("onebot.access-token", "");
        
        // 加载群聊ID列表，支持新格式和旧格式
        this.oneBotGroupIds = loadGroupIds();
        
        // 加载私聊ID列表，支持新格式和旧格式
        this.oneBotPrivateIds = loadPrivateIds();
        
        this.oneBotConnectTimeout = config.getInt("onebot.connect-timeout", 10);
    }
    
    /**
     * 加载群聊ID列表
     * 支持新的 group-ids 列表格式和旧的 group-id 单个值格式
     */
    private List<Long> loadGroupIds() {
        List<Long> groupIds = new ArrayList<>();
        
        // 首先尝试读取新格式 group-ids 列表
        if (config.isList("onebot.group-ids")) {
            List<?> configList = config.getList("onebot.group-ids");
            if (configList != null) {
                for (Object item : configList) {
                    try {
                        long groupId = Long.parseLong(item.toString());
                        if (groupId > 0) { // 只添加有效的群聊ID
                            groupIds.add(groupId);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("无效的群聊ID: " + item);
                    }
                }
            }
        }
        
        // 如果没有新格式配置，尝试读取旧格式 group-id 兼容性
        if (groupIds.isEmpty() && config.contains("onebot.group-id")) {
            long oldGroupId = config.getLong("onebot.group-id", 0);
            if (oldGroupId > 0) {
                groupIds.add(oldGroupId);
                plugin.getLogger().info("检测到旧格式的群聊ID配置，建议更新为 group-ids 列表格式");
            }
        }
        
        return groupIds;
    }
    
    /**
     * 加载私聊ID列表
     * 支持新的 private-ids 列表格式和旧的 private-id 单个值格式
     */
    private List<Long> loadPrivateIds() {
        List<Long> privateIds = new ArrayList<>();
        
        // 首先尝试读取新格式 private-ids 列表
        if (config.isList("onebot.private-ids")) {
            List<?> configList = config.getList("onebot.private-ids");
            if (configList != null) {
                for (Object item : configList) {
                    try {
                        long privateId = Long.parseLong(item.toString());
                        if (privateId > 0) { // 只添加有效的私聊ID
                            privateIds.add(privateId);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("无效的私聊ID: " + item);
                    }
                }
            }
        }
        
        // 如果没有新格式配置，尝试读取旧格式 private-id 兼容性
        if (privateIds.isEmpty() && config.contains("onebot.private-id")) {
            long oldPrivateId = config.getLong("onebot.private-id", 0);
            if (oldPrivateId > 0) {
                privateIds.add(oldPrivateId);
                plugin.getLogger().info("检测到旧格式的私聊ID配置，建议更新为 private-ids 列表格式");
            }
        }
        
        return privateIds;
    }
    
    /**
     * 验证配置值的合理性
     * 
     * @return 配置是否有效
     */
    private boolean validateConfig() {
        boolean valid = true;
        
        if (timeWindow <= 0) {
            plugin.getLogger().warning("检测时间窗口必须大于0，当前值: " + timeWindow);
            valid = false;
        }
        
        if (heightThreshold < 0) {
            plugin.getLogger().warning("高度阈值不能为负数，当前值: " + heightThreshold);
            valid = false;
        }
        
        if (cooldownSeconds < 0) {
            plugin.getLogger().warning("冷却时间不能为负数，当前值: " + cooldownSeconds);
            valid = false;
        }
        
        if (continuousThreshold <= 0) {
            plugin.getLogger().warning("连续检测次数阈值必须大于0，当前值: " + continuousThreshold);
            valid = false;
        }
        
        if (banDurationMinutes <= 0) {
            plugin.getLogger().warning("封禁时长必须大于0，当前值: " + banDurationMinutes);
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * 获取消息
     * 
     * @param path 消息路径
     * @param defaultValue 默认值
     * @return 消息内容
     */
    public String getMessage(String path, String defaultValue) {
        return config.getString("messages." + path, defaultValue);
    }
    
    /**
     * 获取消息（带颜色代码转换）
     * 
     * @param path 消息路径
     * @param defaultValue 默认值
     * @return 处理后的消息内容
     */
    public String getFormattedMessage(String path, String defaultValue) {
        String message = getMessage(path, defaultValue);
        return message.replace("&", "§");
    }
    

    public int getTimeWindow() {
        return timeWindow;
    }
    
    public boolean isHeightFilterEnabled() {
        return heightFilterEnabled;
    }
    
    public int getHeightThreshold() {
        return heightThreshold;
    }
    
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
    
    public int getContinuousThreshold() {
        return continuousThreshold;
    }
    
    public int getBanDurationMinutes() {
        return banDurationMinutes;
    }
    
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    public boolean isDebugVerbose() {
        return debugVerbose;
    }
    
    public boolean isDisableDetectionInWater() {
        return disableDetectionInWater;
    }

    public int getDimensionSwitchCooldownSeconds() {
        return dimensionSwitchCooldownSeconds;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    

    public boolean isOneBotEnabled() {
        return oneBotEnabled;
    }
    
    public String getOneBotWebSocketUrl() {
        return oneBotWebSocketUrl;
    }
    
    public String getOneBotAccessToken() {
        return oneBotAccessToken;
    }
    
    /**
     * 获取群聊ID列表
     * 
     * @return 群聊ID列表（只读）
     */
    public List<Long> getOneBotGroupIds() {
        return Collections.unmodifiableList(oneBotGroupIds);
    }
    
    /**
     * 获取私聊ID列表
     * 
     * @return 私聊ID列表（只读）
     */
    public List<Long> getOneBotPrivateIds() {
        return Collections.unmodifiableList(oneBotPrivateIds);
    }
    
    /**
     * 获取第一个群聊ID（向下兼容方法）
     * 
     * @return 第一个群聊ID，没有则返回0
     * @deprecated 建议使用 getOneBotGroupIds() 获取完整列表
     */
    @Deprecated
    public long getOneBotGroupId() {
        return oneBotGroupIds.isEmpty() ? 0 : oneBotGroupIds.get(0);
    }
    
    /**
     * 获取第一个私聊ID（向下兼容方法）
     * 
     * @return 第一个私聊ID，没有则返回0
     * @deprecated 建议使用 getOneBotPrivateIds() 获取完整列表
     */
    @Deprecated
    public long getOneBotPrivateId() {
        return oneBotPrivateIds.isEmpty() ? 0 : oneBotPrivateIds.get(0);
    }
    
    public int getOneBotConnectTimeout() {
        return oneBotConnectTimeout;
    }
    
    /**
     * 获取OneBot消息模板
     * 支持从列表中随机选择消息，同时保持向下兼容性
     * 
     * @param messageType 消息类型
     * @param defaultValue 默认值
     * @return 消息模板
     */
    public String getOneBotMessage(String messageType, String defaultValue) {
        String configPath = "onebot.messages." + messageType;

        if (config.isList(configPath)) {
            List<String> messageList = config.getStringList(configPath);
            if (messageList != null && !messageList.isEmpty()) {
                // 从列表中随机选择一个消息
                Random random = new Random();
                return messageList.get(random.nextInt(messageList.size()));
            }
        }

        String singleMessage = config.getString(configPath);
        if (singleMessage != null && !singleMessage.isEmpty()) {
            return singleMessage;
        }
        
        return defaultValue;
    }
} 