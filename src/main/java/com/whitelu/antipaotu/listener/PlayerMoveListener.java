package com.whitelu.antipaotu.listener;

import com.whitelu.antipaotu.AntiPaotuPlugin;
import com.whitelu.antipaotu.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * 玩家移动监听器
 * 用于监控玩家鞘翅状态

 */
public class PlayerMoveListener implements Listener {
    
    private final AntiPaotuPlugin plugin;
    
    public PlayerMoveListener(AntiPaotuPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 监听玩家移动事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        

        if (player.hasPermission("antipaotu.bypass")) {
            return;
        }
        

        if (plugin.getBanManager().isBanned(player.getUniqueId())) {
            return;
        }
        

        PlayerData playerData = plugin.getDetectionManager().getPlayerData(player.getUniqueId());
        

        boolean isGliding = player.isGliding();
        
        if (playerData != null) {

            boolean wasGliding = playerData.isGliding();
            playerData.setGlidingState(isGliding);
            

            if (isGliding && !wasGliding) {
                playerData.startNewDetectionWindow();
                
                if (plugin.getConfigManager().isDebugVerbose()) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 开始使用鞘翅，启动新的检测窗口");
                }
            }
            

            if (!isGliding && wasGliding) {


                if (plugin.getConfigManager().isDebugVerbose()) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 停止使用鞘翅");
                }
            }
        } else if (isGliding) {

            playerData = new PlayerData(player.getUniqueId(), player.getName());
            playerData.setGlidingState(true);
            playerData.startNewDetectionWindow();
            

        }
    }
} 