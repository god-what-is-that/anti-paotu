package com.whitelu.antipaotu;

import com.whitelu.antipaotu.config.ConfigManager;
import com.whitelu.antipaotu.listener.PlayerJoinListener;
import com.whitelu.antipaotu.listener.PlayerMoveListener;
import com.whitelu.antipaotu.manager.BanManager;
import com.whitelu.antipaotu.manager.ChunkTracker;
import com.whitelu.antipaotu.manager.DetectionManager;
import com.whitelu.antipaotu.manager.OneBotManager;
import com.whitelu.antipaotu.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Anti-Paotu 插件主类
 * 用于检测玩家跑图行为并进行相应处理

 */
public class AntiPaotuPlugin extends JavaPlugin implements TabCompleter {
    
    private static AntiPaotuPlugin instance;
    

    private ConfigManager configManager;
    private ChunkTracker chunkTracker;
    private DetectionManager detectionManager;
    private BanManager banManager;
    private OneBotManager oneBotManager;
    private MessageUtil messageUtil;
    

    private PlayerMoveListener playerMoveListener;
    private PlayerJoinListener playerJoinListener;
    
    @Override
    public void onEnable() {
        instance = this;
        

        this.configManager = new ConfigManager(this);
        this.messageUtil = new MessageUtil(this);
        this.chunkTracker = new ChunkTracker(this);
        this.detectionManager = new DetectionManager(this);
        this.banManager = new BanManager(this);
        this.oneBotManager = new OneBotManager(this);
        

        if (!configManager.loadConfig()) {
            getLogger().severe("配置文件加载失败，插件将被禁用！");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        

        registerListeners();
        

        detectionManager.startDetectionTask();
        

        this.getCommand("antipaotu").setTabCompleter(this);
    }
    
    @Override
    public void onDisable() {

        if (detectionManager != null) {
            detectionManager.stopDetectionTask();
        }
        

        if (chunkTracker != null) {
            chunkTracker.cleanup();
        }
        
        if (banManager != null) {
            banManager.cleanup();
        }
        

        if (oneBotManager != null) {
            oneBotManager.shutdown();
        }

        instance = null;
    }
    
    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        this.playerMoveListener = new PlayerMoveListener(this);
        this.playerJoinListener = new PlayerJoinListener(this);
        
        Bukkit.getPluginManager().registerEvents(playerMoveListener, this);
        Bukkit.getPluginManager().registerEvents(playerJoinListener, this);
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("antipaotu")) {
            return false;
        }
        
        if (args.length == 0) {
            messageUtil.sendMessage(sender, "commands.usage-main");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReloadCommand(sender);
                
            case "status":
                return handleStatusCommand(sender, args);
                
            case "unban":
                return handleUnbanCommand(sender, args);
                
            default:
                messageUtil.sendMessage(sender, "commands.usage-main");
                return true;
        }
    }
    
    /**
     * 处理重载命令
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("antipaotu.reload")) {
            messageUtil.sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        try {
            configManager.reloadConfig();
            messageUtil.sendMessage(sender, "commands.reload-success");
            getLogger().info("配置文件已由" + sender.getName() + "重载");
        } catch (Exception e) {
            messageUtil.sendMessage(sender, "commands.reload-failed", 
                                  "%error%", e.getMessage());
            getLogger().warning("配置文件重载失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 处理状态查询命令
     */
    private boolean handleStatusCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("antipaotu.status")) {
            messageUtil.sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageUtil.sendMessage(sender, "commands.usage-status");
            return true;
        }
        
        String playerName = args[1];

        org.bukkit.OfflinePlayer target = Bukkit.getPlayer(playerName);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(playerName);
        }
        
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messageUtil.sendMessage(sender, "commands.player-not-found", 
                                  "%player%", playerName);
            return true;
        }
        

        var playerData = detectionManager.getPlayerData(target.getUniqueId());
        if (playerData == null) {
            messageUtil.sendMessage(sender, "commands.no-detection-data", 
                                  "%player%", playerName);
            return true;
        }
        

        boolean isBanned = banManager.isBanned(target.getUniqueId());
        String lastDetectionTime = playerData.getLastDetectionTime() != null 
                                   ? playerData.getLastDetectionTime().toString()
                                   : "无";
        messageUtil.sendMessage(sender, "commands.status-info",
                              "%player%", playerName,
                              "%count%", String.valueOf(playerData.getContinuousCount()),
                              "%time%", lastDetectionTime,
                              "%banned%", isBanned ? "是" : "否");
        
        return true;
    }
    
    /**
     * 处理解封命令
     */
    private boolean handleUnbanCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("antipaotu.unban")) {
            messageUtil.sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageUtil.sendMessage(sender, "commands.usage-unban");
            return true;
        }
        
        String playerName = args[1];

        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messageUtil.sendMessage(sender, "commands.player-not-found", 
                                  "%player%", playerName);
            return true;
        }
        
        if (!banManager.isBanned(target.getUniqueId())) {
            messageUtil.sendMessage(sender, "commands.not-banned", 
                                  "%player%", playerName);
            return true;
        }
        
        banManager.unbanPlayer(target.getUniqueId());
        messageUtil.sendMessage(sender, "commands.unban-success", 
                              "%player%", playerName);
        getLogger().info("玩家" + playerName + "的封禁已被" + sender.getName() + "解除");
        
        return true;
    }
    
    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                     @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("antipaotu")) {
            return null;
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {

            List<String> subCommands = Arrays.asList("reload", "status", "unban");
            return subCommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "status":

                    if (sender.hasPermission("antipaotu.status")) {
                        List<String> players = new ArrayList<>();
                        

                        Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .forEach(players::add);
                        

                        detectionManager.getAllPlayerData().values().stream()
                                .map(playerData -> playerData.getPlayerName())
                                .forEach(players::add);
                        
                        return players.stream()
                                .distinct()
                                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    break;
                    
                case "unban":

                    if (sender.hasPermission("antipaotu.unban")) {
                        return banManager.getBannedPlayerNames().stream()
                                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    break;
            }
        }
        
        return completions;
    }
    

    public static AntiPaotuPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ChunkTracker getChunkTracker() {
        return chunkTracker;
    }
    
    public DetectionManager getDetectionManager() {
        return detectionManager;
    }
    
    public BanManager getBanManager() {
        return banManager;
    }
    
    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
    
    public OneBotManager getOneBotManager() {
        return oneBotManager;
    }
} 