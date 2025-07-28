package com.whitelu.antipaotu.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.whitelu.antipaotu.AntiPaotuPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * OneBot管理器类
 * 负责处理与OneBot服务器的WebSocket连接和消息发送

 */
public class OneBotManager {
    
    private final AntiPaotuPlugin plugin;
    private final Gson gson;
    private final ExecutorService executor;
    
    public OneBotManager(AntiPaotuPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("OneBot-Worker");
            return thread;
        });
    }
    
    /**
     * 发送检测通知到QQ
     * 
     * @param playerName 玩家名
     * @param timeWindow 时间窗口
     * @param chunkCount 区块数量
     * @param continuousCount 连续次数
     */
    public void sendDetectionNotification(String playerName, int timeWindow, int chunkCount, int continuousCount) {
        if (!plugin.getConfigManager().isOneBotEnabled()) {
            return;
        }
        
        String message = plugin.getConfigManager().getOneBotMessage("detection", 
            "已检测到玩家[%player%]触发跑图阈值，在%time%秒内生成了%chunks%个区块，连续次数%count%");
        
        message = message.replace("%player%", playerName)
                         .replace("%time%", String.valueOf(timeWindow))
                         .replace("%chunks%", String.valueOf(chunkCount))
                         .replace("%count%", String.valueOf(continuousCount));
        
        sendMessage(message);
    }
    
    /**
     * 发送封禁通知到QQ
     * 
     * @param playerName 玩家名
     */
    public void sendBanNotification(String playerName) {
        if (!plugin.getConfigManager().isOneBotEnabled()) {
            return;
        }
        
        String message = plugin.getConfigManager().getOneBotMessage("ban", 
            "由于多次触发跑图阈值，玩家[%player%]被暂时禁止进入服务器");
        
        message = message.replace("%player%", playerName);
        
        sendMessage(message);
    }
    
    /**
     * 发送消息到配置的QQ群和私聊
     * 
     * @param message 消息内容
     */
    private void sendMessage(String message) {
        executor.submit(() -> {
            try {
                List<Long> groupIds = plugin.getConfigManager().getOneBotGroupIds();
                List<Long> privateIds = plugin.getConfigManager().getOneBotPrivateIds();
                
                boolean hasTarget = false;
                
                // 向所有配置的群聊发送消息
                for (Long groupId : groupIds) {
                    if (groupId > 0) {
                        hasTarget = true;
                        sendGroupMessage(groupId, message);
                    }
                }
                
                // 向所有配置的私聊发送消息
                for (Long privateId : privateIds) {
                    if (privateId > 0) {
                        hasTarget = true;
                        sendPrivateMessage(privateId, message);
                    }
                }
                
                // 如果没有配置任何目标且启用了调试模式，则记录日志
                if (!hasTarget && plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("OneBot: 未配置群聊或私聊目标，跳过消息发送");
                }
                
            } catch (Exception e) {
                handleSendError("发送消息时发生异常", e);
            }
        });
    }
    
    /**
     * 发送群消息
     * 
     * @param groupId 群号
     * @param message 消息内容
     */
    private void sendGroupMessage(long groupId, String message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            String wsUrl = plugin.getConfigManager().getOneBotWebSocketUrl();
            String accessToken = plugin.getConfigManager().getOneBotAccessToken();
            
            URI uri = new URI(wsUrl);
            
            WebSocketClient client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    try {

                        JsonObject request = new JsonObject();
                        request.addProperty("action", "send_group_msg");
                        
                        JsonObject params = new JsonObject();
                        params.addProperty("group_id", groupId);
                        params.addProperty("message", message);
                        request.add("params", params);
                        

                        request.addProperty("echo", "group_" + System.currentTimeMillis());
                        
                        send(gson.toJson(request));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }
                
                                 @Override
                 public void onMessage(String message) {
                     try {
                         if (plugin.getConfigManager().isDebugEnabled()) {
                             plugin.getLogger().info("OneBot群消息响应: " + message);
                         }
                         
                         JsonObject response = gson.fromJson(message, JsonObject.class);
                         

                         if (response.has("status")) {
                             String status = response.get("status").getAsString();
                             if ("ok".equals(status)) {
                                 future.complete(null);
                             } else {
                                 String reason = response.has("msg") ? response.get("msg").getAsString() : "未知错误";
                                 future.completeExceptionally(new RuntimeException("群消息发送失败: " + reason));
                             }
                         } else if (response.has("retcode")) {

                             int retcode = response.get("retcode").getAsInt();
                             if (retcode == 0) {
                                 future.complete(null);
                             } else {
                                 String reason = response.has("message") ? response.get("message").getAsString() : "未知错误";
                                 future.completeExceptionally(new RuntimeException("群消息发送失败: " + reason));
                             }
                         } else {

                             future.complete(null);
                         }
                     } catch (Exception e) {
                         future.completeExceptionally(e);
                     }
                     close();
                 }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (!future.isDone()) {
                        future.completeExceptionally(new RuntimeException("连接关闭: " + reason));
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    future.completeExceptionally(ex);
                }
            };
            

            if (accessToken != null && !accessToken.isEmpty()) {
                client.addHeader("Authorization", "Bearer " + accessToken);
            }
            
            client.connect();
            

            future.get(plugin.getConfigManager().getOneBotConnectTimeout(), TimeUnit.SECONDS);
            
        } catch (Exception e) {
            handleSendError("发送群消息失败", e);
        }
    }
    
    /**
     * 发送私聊消息
     * 
     * @param userId 用户QQ号
     * @param message 消息内容
     */
    private void sendPrivateMessage(long userId, String message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            String wsUrl = plugin.getConfigManager().getOneBotWebSocketUrl();
            String accessToken = plugin.getConfigManager().getOneBotAccessToken();
            
            URI uri = new URI(wsUrl);
            
            WebSocketClient client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    try {

                        JsonObject request = new JsonObject();
                        request.addProperty("action", "send_private_msg");
                        
                        JsonObject params = new JsonObject();
                        params.addProperty("user_id", userId);
                        params.addProperty("message", message);
                        request.add("params", params);
                        

                        request.addProperty("echo", "private_" + System.currentTimeMillis());
                        
                        send(gson.toJson(request));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }
                
                                 @Override
                 public void onMessage(String message) {
                     try {
                         if (plugin.getConfigManager().isDebugEnabled()) {
                             plugin.getLogger().info("OneBot私聊响应: " + message);
                         }
                         
                         JsonObject response = gson.fromJson(message, JsonObject.class);
                         

                         if (response.has("status")) {
                             String status = response.get("status").getAsString();
                             if ("ok".equals(status)) {
                                 future.complete(null);
                             } else {
                                 String reason = response.has("msg") ? response.get("msg").getAsString() : "未知错误";
                                 future.completeExceptionally(new RuntimeException("私聊消息发送失败: " + reason));
                             }
                         } else if (response.has("retcode")) {

                             int retcode = response.get("retcode").getAsInt();
                             if (retcode == 0) {
                                 future.complete(null);
                             } else {
                                 String reason = response.has("message") ? response.get("message").getAsString() : "未知错误";
                                 future.completeExceptionally(new RuntimeException("私聊消息发送失败: " + reason));
                             }
                         } else {

                             future.complete(null);
                         }
                     } catch (Exception e) {
                         future.completeExceptionally(e);
                     }
                     close();
                 }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (!future.isDone()) {
                        future.completeExceptionally(new RuntimeException("连接关闭: " + reason));
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    future.completeExceptionally(ex);
                }
            };
            

            if (accessToken != null && !accessToken.isEmpty()) {
                client.addHeader("Authorization", "Bearer " + accessToken);
            }
            
            client.connect();
            

            future.get(plugin.getConfigManager().getOneBotConnectTimeout(), TimeUnit.SECONDS);
            
        } catch (Exception e) {
            handleSendError("发送私聊消息失败", e);
        }
    }
    
         /**
      * 处理发送错误
      * 
      * @param errorType 错误类型
      * @param throwable 异常
      */
     private void handleSendError(String errorType, Throwable throwable) {

         String errorMsg = throwable.getMessage();
         if (errorMsg != null) {

             if (errorMsg.contains("Connection refused") || 
                 errorMsg.contains("连接被拒绝") ||
                 errorMsg.contains("timeout") ||
                 errorMsg.contains("超时")) {
                 

                 if (plugin.getConfigManager().isDebugEnabled()) {
                     plugin.getLogger().info("OneBot连接失败（这是正常现象如果未启用OneBot）: " + errorMsg);
                 }
                 return;
             }
         }
         
         String errorMessage = "§cQQBot消息发送失败，请检查onebot接口是否可用！报错信息：" + throwable.getMessage();
         

         Bukkit.getScheduler().runTask(plugin, () -> {
             for (Player player : Bukkit.getOnlinePlayers()) {
                 if (player.hasPermission("antipaotu.notice")) {
                     player.sendMessage(errorMessage);
                 }
             }
         });
         

         plugin.getLogger().warning("OneBot " + errorType + ": " + throwable.getMessage());
         if (plugin.getConfigManager().isDebugEnabled()) {
             throwable.printStackTrace();
         }
     }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 