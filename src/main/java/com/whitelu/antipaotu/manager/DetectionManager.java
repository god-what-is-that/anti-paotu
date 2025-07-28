package com.whitelu.antipaotu.manager;

import com.whitelu.antipaotu.AntiPaotuPlugin;
import com.whitelu.antipaotu.data.ChunkData;
import com.whitelu.antipaotu.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检测管理器类
 * 处理跑图检测逻辑和玩家数据管理

 */
public class DetectionManager {
    
    private final AntiPaotuPlugin plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    

    private int detectionTaskId = -1;
    
    public DetectionManager(AntiPaotuPlugin plugin) {
        this.plugin = plugin;
        this.playerDataMap = new ConcurrentHashMap<>();
    }
    
    /**
     * 启动检测任务
     */
    public void startDetectionTask() {

        detectionTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
            this::performDetectionCheck,
            20L,
            20L
        ).getTaskId();
    }
    
    /**
     * 停止检测任务
     */
    public void stopDetectionTask() {
        if (detectionTaskId != -1) {
            Bukkit.getScheduler().cancelTask(detectionTaskId);
            detectionTaskId = -1;
        }
    }
    
    /**
     * 当区块生成时调用
     * 
     * @param chunkData 区块数据
     */
    public void onChunkGenerated(ChunkData chunkData) {
        UUID playerId = chunkData.getTriggerPlayerId();
        

        PlayerData playerData = getOrCreatePlayerData(playerId, chunkData.getTriggerPlayerName());
        

        playerData.addChunkToCurrentWindow(chunkData);
        


        if (plugin.getConfigManager().isDebugVerbose()) {
            plugin.getLogger().info("检测到区块生成: " + chunkData.getChunkKey() + 
                                  " 触发玩家: " + chunkData.getTriggerPlayerName());
        }
    }
    
    /**
     * 执行检测检查
     */
    private void performDetectionCheck() {
        for (PlayerData playerData : playerDataMap.values()) {

            playerData.cleanupExpiredChunks(plugin.getConfigManager().getTimeWindow());
            

            Player player = Bukkit.getPlayer(playerData.getPlayerId());
            if (player == null || !player.isOnline()) {
                continue;
            }
            

            if (playerData.isInCooldown(plugin.getConfigManager().getCooldownSeconds())) {
                if (plugin.getConfigManager().isDebugVerbose()) {
                    plugin.getLogger().info("玩家 " + playerData.getPlayerName() + 
                                          " 仍在冷却期，跳过检测");
                }
                continue;
            }
            

            if (plugin.getConfigManager().isDebugVerbose()) {
                plugin.getLogger().info("玩家 " + playerData.getPlayerName() + 
                                      " 冷却期结束，开始检测 (当前区块数: " + 
                                      playerData.getCurrentWindowChunkCount() + ")");
            }
            checkDetectionThreshold(playerData);
        }
        

        cleanupOfflinePlayerData();
    }
    
    /**
     * 检查检测阈值
     * 
     * @param playerData 玩家数据
     */
    private void checkDetectionThreshold(PlayerData playerData) {
        int timeWindow = plugin.getConfigManager().getTimeWindow();
        int chunkCount = playerData.getCurrentWindowChunkCount();
        

        if (chunkCount == 0) {
            return;
        }
        

        Player player = Bukkit.getPlayer(playerData.getPlayerId());
        if (player == null) {
            return;
        }
        

        if (!player.isGliding()) {

            if (playerData.getContinuousCount() > 0) {
                playerData.resetContinuousCount();
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("玩家 " + playerData.getPlayerName() + 
                                          " 停止使用鞘翅，连续计数已重置");
                }
            }
            return;
        }
        
        int viewDistance = getPlayerViewDistance(player);
        int expectedChunks = (2 * viewDistance + 1) * 5; // n=(2v+1)×5
        
        if (chunkCount >= expectedChunks) {

            triggerDetection(playerData, timeWindow, chunkCount);
        } else {

            if (playerData.getContinuousCount() > 0 && playerData.getLastDetectionTime() != null) {
                playerData.resetContinuousCount();
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("玩家 " + playerData.getPlayerName() + 
                                          " 在冷却期后未再次触发阈值，连续计数已重置");
                }
            }
        }
    }
    
    /**
     * 触发检测
     * 
     * @param playerData 玩家数据
     * @param timeWindow 时间窗口
     * @param chunkCount 区块数量
     */
    private void triggerDetection(PlayerData playerData, int timeWindow, int chunkCount) {
        playerData.triggerDetection();
        int continuousCount = playerData.getContinuousCount();
        

        playerData.setCooldown(true);
        

        sendDetectionNotifications(playerData, timeWindow, chunkCount, continuousCount);
        

        if (continuousCount >= plugin.getConfigManager().getContinuousThreshold()) {
            plugin.getBanManager().banPlayer(playerData.getPlayerId(), playerData.getPlayerName());
            sendBanNotifications(playerData);
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("玩家 " + playerData.getPlayerName() + 
                                  " 触发检测，连续次数: " + continuousCount);
        }
    }
    
    /**
     * 发送检测通知
     */
    private void sendDetectionNotifications(PlayerData playerData, int timeWindow, 
                                          int chunkCount, int continuousCount) {
        String playerName = playerData.getPlayerName();
        

        String consoleMsg = plugin.getConfigManager().getMessage("console.detection", 
            "[Anti-paotu] 已检测到玩家%player%触发跑图阈值，在%time%秒内生成了%chunks%个区块，连续次数%count%");
        consoleMsg = consoleMsg.replace("%player%", playerName)
                              .replace("%time%", String.valueOf(timeWindow))
                              .replace("%chunks%", String.valueOf(chunkCount))
                              .replace("%count%", String.valueOf(continuousCount));
        plugin.getLogger().info(consoleMsg);
        

        String adminMsg = plugin.getConfigManager().getMessage("admin.detection", 
            "[Anti-paotu] 已检测到玩家%player%触发跑图阈值，在%time%秒内生成了%chunks%个区块，连续次数%count%");
        adminMsg = adminMsg.replace("%player%", playerName)
                          .replace("%time%", String.valueOf(timeWindow))
                          .replace("%chunks%", String.valueOf(chunkCount))
                          .replace("%count%", String.valueOf(continuousCount));
        

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("antipaotu.notice")) {
                player.sendMessage(adminMsg);
            }
        }
        

        Player targetPlayer = Bukkit.getPlayer(playerData.getPlayerId());
        if (targetPlayer != null) {
            String warningMsg = plugin.getConfigManager().getFormattedMessage("player.warning", 
                "§c已触发跑图阈值，如果你连续触发此阈值会导致你被临时封禁！");
            targetPlayer.sendMessage(warningMsg);
        }
        

        plugin.getOneBotManager().sendDetectionNotification(playerName, timeWindow, chunkCount, continuousCount);
    }
    
    /**
     * 发送封禁通知
     */
    private void sendBanNotifications(PlayerData playerData) {
        String playerName = playerData.getPlayerName();
        

        String consoleMsg = plugin.getConfigManager().getMessage("console.ban", 
            "[Anti-paotu] 已将连续多次触发跑图阈值的玩家%player%封禁");
        consoleMsg = consoleMsg.replace("%player%", playerName);
        plugin.getLogger().info(consoleMsg);
        

        String adminMsg = plugin.getConfigManager().getMessage("admin.ban", 
            "[Anti-paotu] 已将连续多次触发跑图阈值的玩家%player%封禁");
        adminMsg = adminMsg.replace("%player%", playerName);
        

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("antipaotu.notice")) {
                player.sendMessage(adminMsg);
            }
        }
        

        plugin.getOneBotManager().sendBanNotification(playerName);
    }
    
    /**
     * 获取玩家的视距
     */
    private int getPlayerViewDistance(Player player) {
        try {
            return player.getClientViewDistance();
        } catch (Exception e) {
            return Bukkit.getViewDistance();
        }
    }
    
    /**
     * 获取或创建玩家数据
     */
    private PlayerData getOrCreatePlayerData(UUID playerId, String playerName) {
        return playerDataMap.computeIfAbsent(playerId, 
            id -> new PlayerData(id, playerName));
    }
    
    /**
     * 清理离线玩家的数据
     */
    private void cleanupOfflinePlayerData() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
        
        playerDataMap.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            PlayerData playerData = entry.getValue();
            

            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                return false;
            }
            

            LocalDateTime lastActivity = playerData.getLastDetectionTime();
            if (lastActivity == null) {
                lastActivity = playerData.getCurrentWindowStart();
            }
            
            return lastActivity == null || lastActivity.isBefore(cutoffTime);
        });
    }
    
    /**
     * 当玩家离开时清理数据
     */
    public void onPlayerQuit(UUID playerId) {
        PlayerData playerData = playerDataMap.get(playerId);
        if (playerData != null) {

            playerData.resetContinuousCount();
        }
    }
    
    /**
     * 获取玩家数据
     */
    public PlayerData getPlayerData(UUID playerId) {
        return playerDataMap.get(playerId);
    }
    
    /**
     * 获取所有玩家数据
     */
    public Map<UUID, PlayerData> getAllPlayerData() {
        return new ConcurrentHashMap<>(playerDataMap);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        stopDetectionTask();
        playerDataMap.clear();
    }
} 