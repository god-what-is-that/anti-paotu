package com.whitelu.antipaotu.util;

import com.whitelu.antipaotu.AntiPaotuPlugin;
import org.bukkit.command.CommandSender;

/**
 * 消息工具类
 * 用于发送格式化的消息

 */
public class MessageUtil {
    
    private final AntiPaotuPlugin plugin;
    
    public MessageUtil(AntiPaotuPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 发送消息
     * 
     * @param sender 消息接收者
     * @param messagePath 消息路径
     */
    public void sendMessage(CommandSender sender, String messagePath) {
        String message = plugin.getConfigManager().getFormattedMessage(messagePath, "§c消息未找到: " + messagePath);
        sender.sendMessage(message);
    }
    
    /**
     * 发送消息（带占位符替换）
     * 
     * @param sender 消息接收者
     * @param messagePath 消息路径
     * @param placeholders 占位符和替换值的键值对
     */
    public void sendMessage(CommandSender sender, String messagePath, String... placeholders) {
        String message = plugin.getConfigManager().getFormattedMessage(messagePath, "§c消息未找到: " + messagePath);
        

        if (placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String replacement = placeholders[i + 1];
                message = message.replace(placeholder, replacement);
            }
        }
        
        sender.sendMessage(message);
    }
    
    /**
     * 格式化消息
     * 
     * @param messagePath 消息路径
     * @param placeholders 占位符和替换值的键值对
     * @return 格式化后的消息
     */
    public String formatMessage(String messagePath, String... placeholders) {
        String message = plugin.getConfigManager().getFormattedMessage(messagePath, "§c消息未找到: " + messagePath);
        

        if (placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String replacement = placeholders[i + 1];
                message = message.replace(placeholder, replacement);
            }
        }
        
        return message;
    }
    
    /**
     * 格式化时间
     * 
     * @param seconds 秒数
     * @return 格式化的时间字符串
     */
    public String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0秒";
        }
        
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        
        if (minutes > 0) {
            if (remainingSeconds > 0) {
                return minutes + "分" + remainingSeconds + "秒";
            } else {
                return minutes + "分钟";
            }
        } else {
            return remainingSeconds + "秒";
        }
    }
    
    /**
     * 将颜色代码转换为Minecraft颜色
     * 
     * @param text 原始文本
     * @return 转换后的文本
     */
    public String colorize(String text) {
        return text.replace("&", "§");
    }
} 