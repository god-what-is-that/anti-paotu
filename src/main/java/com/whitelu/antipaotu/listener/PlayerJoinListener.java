package com.whitelu.antipaotu.listener;

import com.whitelu.antipaotu.AntiPaotuPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家加入监听器
 * 处理玩家进入和离开事件

 */
public class PlayerJoinListener implements Listener {
    
    private final AntiPaotuPlugin plugin;
    
    public PlayerJoinListener(AntiPaotuPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 监听玩家加入事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getBanManager().isBanned(player.getUniqueId())) {


            long remainingTime = plugin.getBanManager().getRemainingBanTime(player.getUniqueId());
            String kickMessage = plugin.getConfigManager().getFormattedMessage("player.ban-login", 
                "§c你因连续触发多次跑图检测，被暂时禁止进入服务器\n§e请在%time%分钟后再试");
            kickMessage = kickMessage.replace("%time%", String.valueOf(remainingTime));
            
            player.kickPlayer(kickMessage);
        }
    }
    
    /**
     * 监听玩家离开事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getDetectionManager().onPlayerQuit(player.getUniqueId());
    }
} 