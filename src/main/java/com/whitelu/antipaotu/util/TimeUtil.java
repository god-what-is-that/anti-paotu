package com.whitelu.antipaotu.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 时间工具类
 * 用于时间相关的计算和格式化

 */
public class TimeUtil {
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * 格式化日期时间
     * 
     * @param dateTime 日期时间
     * @return 格式化的字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知";
        }
        return dateTime.format(DATETIME_FORMATTER);
    }
    
    /**
     * 格式化时间
     * 
     * @param dateTime 日期时间
     * @return 格式化的时间字符串
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知";
        }
        return dateTime.format(TIME_FORMATTER);
    }
    
    /**
     * 计算两个时间之间的秒数差
     * 
     * @param start 开始时间
     * @param end 结束时间
     * @return 秒数差
     */
    public static long getSecondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }
    
    /**
     * 计算两个时间之间的分钟数差
     * 
     * @param start 开始时间
     * @param end 结束时间
     * @return 分钟数差
     */
    public static long getMinutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }
    
    /**
     * 检查时间是否在指定秒数之前
     * 
     * @param time 要检查的时间
     * @param secondsAgo 多少秒之前
     * @return 是否在指定时间之前
     */
    public static boolean isTimeBefore(LocalDateTime time, long secondsAgo) {
        if (time == null) {
            return true;
        }
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(secondsAgo);
        return time.isBefore(cutoffTime);
    }
    
    /**
     * 检查时间是否在指定分钟数之前
     * 
     * @param time 要检查的时间
     * @param minutesAgo 多少分钟之前
     * @return 是否在指定时间之前
     */
    public static boolean isTimeBeforeMinutes(LocalDateTime time, long minutesAgo) {
        if (time == null) {
            return true;
        }
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutesAgo);
        return time.isBefore(cutoffTime);
    }
    
    /**
     * 获取当前时间到指定时间的剩余秒数
     * 
     * @param targetTime 目标时间
     * @return 剩余秒数，如果目标时间已过则返回0
     */
    public static long getRemainingSeconds(LocalDateTime targetTime) {
        if (targetTime == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(targetTime)) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(now, targetTime);
    }
    
    /**
     * 获取当前时间到指定时间的剩余分钟数（向上取整）
     * 
     * @param targetTime 目标时间
     * @return 剩余分钟数，如果目标时间已过则返回0
     */
    public static long getRemainingMinutes(LocalDateTime targetTime) {
        if (targetTime == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(targetTime)) {
            return 0;
        }
        long seconds = ChronoUnit.SECONDS.between(now, targetTime);
        return (seconds + 59) / 60;
    }
    
    /**
     * 格式化持续时间
     * 
     * @param seconds 总秒数
     * @return 格式化的持续时间字符串
     */
    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0秒";
        }
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        
        if (minutes > 0) {
            sb.append(minutes).append("分钟");
        }
        
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append("秒");
        }
        
        return sb.toString();
    }
    
    /**
     * 获取相对时间描述
     * 
     * @param time 时间
     * @return 相对时间描述
     */
    public static String getRelativeTime(LocalDateTime time) {
        if (time == null) {
            return "未知时间";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long seconds = ChronoUnit.SECONDS.between(time, now);
        
        if (seconds < 0) {
            return "未来时间";
        } else if (seconds < 60) {
            return seconds + "秒前";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + "分钟前";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + "小时前";
        } else {
            long days = seconds / 86400;
            return days + "天前";
        }
    }
} 