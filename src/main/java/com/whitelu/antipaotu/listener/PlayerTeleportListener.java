package com.whitelu.antipaotu.listener;

import com.whitelu.antipaotu.AntiPaotuPlugin;
import com.whitelu.antipaotu.data.PlayerData;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 玩家传送监听器
 * 用于监控玩家维度切换，设置维度切换冷却期
 */
public class PlayerTeleportListener implements Listener {
    
    private final AntiPaotuPlugin plugin;
    
    public PlayerTeleportListener(AntiPaotuPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 监听玩家传送事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 检查是否有bypass权限
        if (player.hasPermission("antipaotu.bypass")) {
            return;
        }
        
        // 检查是否被封禁
        if (plugin.getBanManager().isBanned(player.getUniqueId())) {
            return;
        }
        
        // 检查是否切换了维度
        if (isDimensionSwitch(event)) {
            // 获取玩家数据
            PlayerData playerData = plugin.getDetectionManager().getPlayerData(player.getUniqueId());
            if (playerData != null) {
                // 设置维度切换冷却
                playerData.setDimensionSwitchCooldown();
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("玩家 " + player.getName() + 
                                          " 切换维度：从 " + getWorldTypeName(event.getFrom().getWorld()) + 
                                          " 到 " + getWorldTypeName(event.getTo().getWorld()) + 
                                          "，设置维度切换冷却期");
                }
            }
        }
    }
    
    /**
     * 判断是否为维度切换
     */
    private boolean isDimensionSwitch(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() == null || event.getTo().getWorld() == null) {
            return false;
        }
        
        World fromWorld = event.getFrom().getWorld();
        World toWorld = event.getTo().getWorld();
        
        // 判断是否切换了世界
        if (!fromWorld.equals(toWorld)) {
            // 检查是否是主要维度（主世界、地狱、末地）之间的切换
            World.Environment fromEnv = fromWorld.getEnvironment();
            World.Environment toEnv = toWorld.getEnvironment();
            
            return (fromEnv == World.Environment.NORMAL || 
                    fromEnv == World.Environment.NETHER || 
                    fromEnv == World.Environment.THE_END) &&
                   (toEnv == World.Environment.NORMAL || 
                    toEnv == World.Environment.NETHER || 
                    toEnv == World.Environment.THE_END);
        }
        
        return false;
    }
    
    /**
     * 获取世界类型名称
     */
    private String getWorldTypeName(World world) {
        if (world == null) {
            return "未知";
        }
        
        switch (world.getEnvironment()) {
            case NORMAL:
                return "主世界";
            case NETHER:
                return "地狱";
            case THE_END:
                return "末地";
            default:
                return world.getName();
        }
    }
} 