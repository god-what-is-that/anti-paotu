package com.whitelu.antipaotu.data;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 区块数据模型类
 * 用于存储区块生成的相关信息

 */
public class ChunkData {
    
    private final int chunkX;
    private final int chunkZ;
    private final String worldName;
    private final UUID worldId;
    private final LocalDateTime generationTime;
    private final UUID triggerPlayerId;
    private final String triggerPlayerName;
    private final Location playerLocationAtGeneration;
    
    /**
     * 构造函数
     * 
     * @param chunk 生成的区块
     * @param triggerPlayerId 触发区块生成的玩家ID
     * @param triggerPlayerName 触发区块生成的玩家名称
     * @param playerLocation 玩家在区块生成时的位置
     */
    public ChunkData(Chunk chunk, UUID triggerPlayerId, String triggerPlayerName, Location playerLocation) {
        this.chunkX = chunk.getX();
        this.chunkZ = chunk.getZ();
        this.worldName = chunk.getWorld().getName();
        this.worldId = chunk.getWorld().getUID();
        this.generationTime = LocalDateTime.now();
        this.triggerPlayerId = triggerPlayerId;
        this.triggerPlayerName = triggerPlayerName;
        this.playerLocationAtGeneration = playerLocation.clone();
    }
    
    /**
     * 构造函数（简化版）
     * 
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @param world 世界
     * @param triggerPlayerId 触发区块生成的玩家ID
     * @param triggerPlayerName 触发区块生成的玩家名称
     * @param playerLocation 玩家位置
     */
    public ChunkData(int chunkX, int chunkZ, World world, UUID triggerPlayerId, 
                    String triggerPlayerName, Location playerLocation) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.worldName = world.getName();
        this.worldId = world.getUID();
        this.generationTime = LocalDateTime.now();
        this.triggerPlayerId = triggerPlayerId;
        this.triggerPlayerName = triggerPlayerName;
        this.playerLocationAtGeneration = playerLocation.clone();
    }
    
    /**
     * 计算此区块与玩家位置的距离（以区块为单位）
     * 
     * @param playerLocation 玩家位置
     * @return 距离（区块单位）
     */
    public double getDistanceToPlayer(Location playerLocation) {
        if (!playerLocation.getWorld().getUID().equals(this.worldId)) {
            return Double.MAX_VALUE; // 不同世界返回最大值
        }
        
        int playerChunkX = playerLocation.getBlockX() >> 4;
        int playerChunkZ = playerLocation.getBlockZ() >> 4;
        
        double deltaX = this.chunkX - playerChunkX;
        double deltaZ = this.chunkZ - playerChunkZ;
        
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
    
    /**
     * 检查此区块是否在玩家视距范围内
     * 
     * @param playerLocation 玩家位置
     * @param viewDistance 视距
     * @return 是否在视距范围内
     */
    public boolean isWithinViewDistance(Location playerLocation, int viewDistance) {
        return getDistanceToPlayer(playerLocation) <= viewDistance;
    }
    
    /**
     * 获取区块的唯一标识符
     * 
     * @return 区块标识符
     */
    public String getChunkKey() {
        return worldName + ":" + chunkX + ":" + chunkZ;
    }
    
    /**
     * 检查是否与另一个区块数据相同
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ChunkData chunkData = (ChunkData) obj;
        return chunkX == chunkData.chunkX && 
               chunkZ == chunkData.chunkZ && 
               Objects.equals(worldId, chunkData.worldId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(chunkX, chunkZ, worldId);
    }
    
    @Override
    public String toString() {
        return String.format("ChunkData{world=%s, x=%d, z=%d, time=%s, player=%s}", 
                           worldName, chunkX, chunkZ, generationTime, triggerPlayerName);
    }
    

    public int getChunkX() {
        return chunkX;
    }
    
    public int getChunkZ() {
        return chunkZ;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public UUID getWorldId() {
        return worldId;
    }
    
    public LocalDateTime getGenerationTime() {
        return generationTime;
    }
    
    public UUID getTriggerPlayerId() {
        return triggerPlayerId;
    }
    
    public String getTriggerPlayerName() {
        return triggerPlayerName;
    }
    
    public Location getPlayerLocationAtGeneration() {
        return playerLocationAtGeneration.clone();
    }
} 