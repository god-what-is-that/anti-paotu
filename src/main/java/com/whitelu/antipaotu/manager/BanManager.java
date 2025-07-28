package com.whitelu.antipaotu.manager;

import com.whitelu.antipaotu.AntiPaotuPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 封禁管理器类
 * 处理玩家临时封禁逻辑

 */
public class BanManager implements Listener {
    
    private final AntiPaotuPlugin plugin;
    private final Map<UUID, BanRecord> bannedPlayers;
    

    private int cleanupTaskId = -1;
    
    public BanManager(AntiPaotuPlugin plugin) {
        this.plugin = plugin;
        this.bannedPlayers = new ConcurrentHashMap<>();
        

        Bukkit.getPluginManager().registerEvents(this, plugin);
        

        startCleanupTask();
    }
    
    /**
     * 监听玩家登录事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        if (isBanned(playerId)) {
            BanRecord banRecord = bannedPlayers.get(playerId);
            if (banRecord != null) {
                LocalDateTime banEnd = banRecord.getBanTime().plusMinutes(plugin.getConfigManager().getBanDurationMinutes());
                LocalDateTime now = LocalDateTime.now();
                
                if (now.isBefore(banEnd)) {

                    long remainingMinutes = java.time.Duration.between(now, banEnd).toMinutes() + 1;
                    
                    String kickMessage = plugin.getConfigManager().getFormattedMessage("player.ban-login", 
                        "§c你因连续触发多次跑图检测，被暂时禁止进入服务器\n§e请在%time%分钟后再试");
                    kickMessage = kickMessage.replace("%time%", String.valueOf(remainingMinutes));
                    
                    event.disallow(PlayerLoginEvent.Result.KICK_BANNED, kickMessage);
                    return;
                } else {

                    unbanPlayer(playerId);
                }
            }
        }
    }
    
    /**
     * 封禁玩家
     * 
     * @param playerId 玩家ID
     * @param playerName 玩家名称
     */
    public void banPlayer(UUID playerId, String playerName) {
        BanRecord banRecord = new BanRecord(playerId, playerName, LocalDateTime.now());
        bannedPlayers.put(playerId, banRecord);
        

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int banDuration = plugin.getConfigManager().getBanDurationMinutes();
                String kickMessage = plugin.getConfigManager().getFormattedMessage("player.ban-kick", 
                    "§c你因连续触发多次跑图检测，被暂时禁止进入服务器\n§e请在%time%分钟后再试");
                kickMessage = kickMessage.replace("%time%", String.valueOf(banDuration));
                
                player.kickPlayer(kickMessage);
            });
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("玩家" + playerName + "已被封禁，时长: " + 
                                  plugin.getConfigManager().getBanDurationMinutes() + " 分钟");
        }
    }
    
    /**
     * 解封玩家
     * 
     * @param playerId 玩家ID
     */
    public void unbanPlayer(UUID playerId) {
        BanRecord banRecord = bannedPlayers.remove(playerId);
        if (banRecord != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("玩家" + banRecord.getPlayerName() + "的封禁已解除");
        }
    }
    
    /**
     * 检查玩家是否被封禁
     * 
     * @param playerId 玩家ID
     * @return 是否被封禁
     */
    public boolean isBanned(UUID playerId) {
        BanRecord banRecord = bannedPlayers.get(playerId);
        if (banRecord == null) {
            return false;
        }
        

        LocalDateTime banEnd = banRecord.getBanTime().plusMinutes(plugin.getConfigManager().getBanDurationMinutes());
        if (LocalDateTime.now().isAfter(banEnd)) {

            unbanPlayer(playerId);
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取封禁记录
     * 
     * @param playerId 玩家ID
     * @return 封禁记录，如果没有则返回null
     */
    public BanRecord getBanRecord(UUID playerId) {
        return bannedPlayers.get(playerId);
    }
    
    /**
     * 获取剩余封禁时间（分钟）
     * 
     * @param playerId 玩家ID
     * @return 剩余时间，如果没有被封禁则返回0
     */
    public long getRemainingBanTime(UUID playerId) {
        BanRecord banRecord = bannedPlayers.get(playerId);
        if (banRecord == null) {
            return 0;
        }
        
        LocalDateTime banEnd = banRecord.getBanTime().plusMinutes(plugin.getConfigManager().getBanDurationMinutes());
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(banEnd)) {
            return 0;
        }
        
        return java.time.Duration.between(now, banEnd).toMinutes() + 1;
    }
    
    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {

        cleanupTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
            this::cleanupExpiredBans,
            20L * 60 * 10,
            20L * 60 * 10
        ).getTaskId();
    }
    
    /**
     * 清理过期的封禁记录
     */
    private void cleanupExpiredBans() {
        int banDuration = plugin.getConfigManager().getBanDurationMinutes();
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(banDuration);
        
        int removedCount = 0;
        for (Map.Entry<UUID, BanRecord> entry : bannedPlayers.entrySet()) {
            if (entry.getValue().getBanTime().isBefore(cutoffTime)) {
                bannedPlayers.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0 && plugin.getConfigManager().isDebugVerbose()) {
            plugin.getLogger().info("清理了" + removedCount + "个过期的封禁记录");
        }
    }
    
    /**
     * 获取当前被封禁的玩家数量
     * 
     * @return 被封禁的玩家数量
     */
    public int getBannedPlayerCount() {

        cleanupExpiredBans();
        return bannedPlayers.size();
    }
    
    /**
     * 获取当前被封禁的玩家名单
     * 
     * @return 被封禁的玩家名字列表
     */
    public java.util.List<String> getBannedPlayerNames() {

        cleanupExpiredBans();
        
        return bannedPlayers.values().stream()
                .map(BanRecord::getPlayerName)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {

        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }
        
        bannedPlayers.clear();
    }
    
    /**
     * 封禁记录内部类
     */
    public static class BanRecord {
        private final UUID playerId;
        private final String playerName;
        private final LocalDateTime banTime;
        
        public BanRecord(UUID playerId, String playerName, LocalDateTime banTime) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.banTime = banTime;
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public LocalDateTime getBanTime() {
            return banTime;
        }
    }
} 