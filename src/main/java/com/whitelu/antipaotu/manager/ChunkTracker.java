package com.whitelu.antipaotu.manager;

import com.whitelu.antipaotu.AntiPaotuPlugin;
import com.whitelu.antipaotu.data.ChunkData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区块追踪管理器
 * 用于监控和记录区块生成事件

 */
public class ChunkTracker implements Listener {
    
    private final AntiPaotuPlugin plugin;
    

    private final Map<String, ChunkData> recentChunks;
    

    private int cleanupTaskId = -1;
    
    public ChunkTracker(AntiPaotuPlugin plugin) {
        this.plugin = plugin;
        this.recentChunks = new ConcurrentHashMap<>();
        

        Bukkit.getPluginManager().registerEvents(this, plugin);
        

        startCleanupTask();
    }
    
    /**
     * 监听区块加载事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {

        if (!event.isNewChunk()) {
            return;
        }
        
        Chunk chunk = event.getChunk();
        

        Player triggerPlayer = findNearestPlayer(chunk);
        if (triggerPlayer == null) {
            return;
        }
        

        if (!triggerPlayer.isGliding()) {
            return;
        }
        

        if (triggerPlayer.hasPermission("antipaotu.bypass")) {
            return;
        }
        

        if (plugin.getConfigManager().isHeightFilterEnabled()) {
            int heightThreshold = plugin.getConfigManager().getHeightThreshold();
            if (triggerPlayer.getLocation().getBlockY() > heightThreshold) {
                if (plugin.getConfigManager().isDebugVerbose()) {
                    plugin.getLogger().info("玩家 " + triggerPlayer.getName() + 
                                          " 高度超过阈值，忽略检测");
                }
                return;
            }
        }
        

        ChunkData chunkData = new ChunkData(
            chunk,
            triggerPlayer.getUniqueId(),
            triggerPlayer.getName(),
            triggerPlayer.getLocation()
        );
        

        String chunkKey = chunkData.getChunkKey();
        recentChunks.put(chunkKey, chunkData);
        

        plugin.getDetectionManager().onChunkGenerated(chunkData);
        
        if (plugin.getConfigManager().isDebugVerbose()) {
            plugin.getLogger().info("检测到区块生成: " + chunkKey + 
                                  " 触发玩家: " + triggerPlayer.getName());
        }
    }
    
    /**
     * 查找距离区块最近的正在使用鞘翅的玩家
     * 
     * @param chunk 区块
     * @return 最近的玩家，如果没有则返回null
     */
    private Player findNearestPlayer(Chunk chunk) {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        

        int chunkCenterX = (chunk.getX() << 4) + 8;
        int chunkCenterZ = (chunk.getZ() << 4) + 8;
        Location chunkCenter = new Location(chunk.getWorld(), chunkCenterX, 128, chunkCenterZ);
        
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!player.getWorld().equals(chunk.getWorld())) {
                continue;
            }
            

            if (!player.isGliding()) {
                continue;
            }
            

            Location playerLocation = player.getLocation();
            double distance = playerLocation.distance(chunkCenter);
            

            int viewDistance = getPlayerViewDistance(player);
            double maxDistance = viewDistance * 16 * 1.5;
            
            if (distance <= maxDistance && distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }
        
        return nearestPlayer;
    }
    
    /**
     * 获取玩家的视距
     * 
     * @param player 玩家
     * @return 视距
     */
    private int getPlayerViewDistance(Player player) {
        try {

            return player.getClientViewDistance();
        } catch (Exception e) {

            return Bukkit.getViewDistance();
        }
    }
    
    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {

        cleanupTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
            this::cleanupExpiredChunks, 
            20L * 60 * 5,
            20L * 60 * 5
        ).getTaskId();
    }
    
    /**
     * 清理过期的区块数据
     */
    private void cleanupExpiredChunks() {
        int timeWindow = plugin.getConfigManager().getTimeWindow();
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(timeWindow * 2);
        
        recentChunks.entrySet().removeIf(entry -> 
            entry.getValue().getGenerationTime().isBefore(cutoffTime));
        
        if (plugin.getConfigManager().isDebugVerbose()) {
            plugin.getLogger().info("清理过期区块数据，当前区块数量: " + recentChunks.size());
        }
    }
    
    /**
     * 获取指定时间窗口内的区块数据
     * 
     * @param playerId 玩家ID
     * @param timeWindowSeconds 时间窗口（秒）
     * @return 区块数据列表
     */
    public long getChunkCountInTimeWindow(UUID playerId, int timeWindowSeconds) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(timeWindowSeconds);
        
        return recentChunks.values().stream()
            .filter(chunk -> chunk.getTriggerPlayerId().equals(playerId))
            .filter(chunk -> chunk.getGenerationTime().isAfter(cutoffTime))
            .count();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {

        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }
        

        recentChunks.clear();
        
        plugin.getLogger().info("区块追踪器已清理");
    }
    
    /**
     * 获取当前追踪的区块数量
     * 
     * @return 区块数量
     */
    public int getTrackedChunkCount() {
        return recentChunks.size();
    }
} 