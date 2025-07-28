package com.whitelu.antipaotu.data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 玩家数据模型类
 * 用于存储玩家的跑图检测相关数据

 */
public class PlayerData {
    
    private final UUID playerId;
    private final String playerName;
    

    private final AtomicInteger continuousCount;
    private volatile LocalDateTime lastDetectionTime;
    private volatile LocalDateTime lastCooldownTime;
    private volatile boolean isInCooldown;
    

    private volatile LocalDateTime currentWindowStart;
    private final List<ChunkData> currentWindowChunks;
    

    private final List<DetectionRecord> detectionHistory;
    

    private volatile boolean isGliding;
    private volatile LocalDateTime glidingStartTime;
    
    public PlayerData(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.continuousCount = new AtomicInteger(0);
        this.currentWindowChunks = new ArrayList<>();
        this.detectionHistory = new ArrayList<>();
        this.isInCooldown = false;
        this.isGliding = false;
    }
    
    /**
     * 开始新的检测窗口
     */
    public synchronized void startNewDetectionWindow() {
        this.currentWindowStart = LocalDateTime.now();
        this.currentWindowChunks.clear();
    }
    
    /**
     * 添加区块数据到当前窗口
     */
    public synchronized void addChunkToCurrentWindow(ChunkData chunkData) {
        this.currentWindowChunks.add(chunkData);
    }
    
    /**
     * 获取当前窗口的区块数量
     */
    public synchronized int getCurrentWindowChunkCount() {
        return this.currentWindowChunks.size();
    }
    
    /**
     * 清理过期的区块数据
     */
    public synchronized void cleanupExpiredChunks(int timeWindowSeconds) {
        if (currentWindowStart == null) {
            return;
        }
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(timeWindowSeconds);
        currentWindowChunks.removeIf(chunk -> chunk.getGenerationTime().isBefore(cutoffTime));
        

        if (currentWindowStart.isBefore(cutoffTime)) {
            if (currentWindowChunks.isEmpty()) {
                currentWindowStart = null;
            } else {
                currentWindowStart = currentWindowChunks.get(0).getGenerationTime();
            }
        }
    }
    
    /**
     * 触发检测，增加连续计数
     */
    public void triggerDetection() {
        this.continuousCount.incrementAndGet();
        this.lastDetectionTime = LocalDateTime.now();
        

        DetectionRecord record = new DetectionRecord(
            LocalDateTime.now(),
            getCurrentWindowChunkCount(),
            continuousCount.get()
        );
        
        synchronized (detectionHistory) {
            detectionHistory.add(record);

            if (detectionHistory.size() > 100) {
                detectionHistory.remove(0);
            }
        }
    }
    
    /**
     * 重置连续计数（例如玩家离线或长时间未检测到跑图）
     */
    public void resetContinuousCount() {
        this.continuousCount.set(0);
    }
    
    /**
     * 设置冷却状态
     */
    public void setCooldown(boolean inCooldown) {
        this.isInCooldown = inCooldown;
        if (inCooldown) {
            this.lastCooldownTime = LocalDateTime.now();
        }
    }
    
    /**
     * 检查是否在冷却期间
     */
    public boolean isInCooldown(int cooldownSeconds) {
        if (!isInCooldown || lastCooldownTime == null) {
            return false;
        }
        
        LocalDateTime cooldownEnd = lastCooldownTime.plusSeconds(cooldownSeconds);
        boolean stillInCooldown = LocalDateTime.now().isBefore(cooldownEnd);
        
        if (!stillInCooldown) {
            this.isInCooldown = false;
        }
        
        return stillInCooldown;
    }
    
    /**
     * 设置鞘翅状态
     */
    public void setGlidingState(boolean gliding) {
        if (gliding && !this.isGliding) {
            this.glidingStartTime = LocalDateTime.now();
        }
        this.isGliding = gliding;
    }
    

    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public int getContinuousCount() {
        return continuousCount.get();
    }
    
    public LocalDateTime getLastDetectionTime() {
        return lastDetectionTime;
    }
    
    public LocalDateTime getLastCooldownTime() {
        return lastCooldownTime;
    }
    
    public boolean isInCooldown() {
        return isInCooldown;
    }
    
    public LocalDateTime getCurrentWindowStart() {
        return currentWindowStart;
    }
    
    public List<ChunkData> getCurrentWindowChunks() {
        return new ArrayList<>(currentWindowChunks);
    }
    
    public List<DetectionRecord> getDetectionHistory() {
        synchronized (detectionHistory) {
            return new ArrayList<>(detectionHistory);
        }
    }
    
    public boolean isGliding() {
        return isGliding;
    }
    
    public LocalDateTime getGlidingStartTime() {
        return glidingStartTime;
    }
    
    /**
     * 检测记录内部类
     */
    public static class DetectionRecord {
        private final LocalDateTime time;
        private final int chunkCount;
        private final int continuousCount;
        
        public DetectionRecord(LocalDateTime time, int chunkCount, int continuousCount) {
            this.time = time;
            this.chunkCount = chunkCount;
            this.continuousCount = continuousCount;
        }
        
        public LocalDateTime getTime() {
            return time;
        }
        
        public int getChunkCount() {
            return chunkCount;
        }
        
        public int getContinuousCount() {
            return continuousCount;
        }
    }
} 