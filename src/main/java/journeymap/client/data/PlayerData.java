/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.data;


import com.google.common.cache.CacheLoader;
import journeymap.client.JourneymapClient;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.JMLogger;
import journeymap.client.model.ChunkMD;
import journeymap.client.model.EntityDTO;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.biome.BiomeGenBase;

/**
 * Provides game-related properties in a Map.
 *
 * @author techbrew
 */
public class PlayerData extends CacheLoader<Class, EntityDTO>
{
    /**
     * "Underground" means the player isn't under sky (within a 1 block radius),
     * or is under a block that should be treated as potential sky like glass or ladders.
     */
    public static boolean playerIsUnderground(Minecraft mc, EntityPlayer player)
    {
        if (ForgeHelper.INSTANCE.hasNoSky(player.getEntityWorld()))
        {
            return true;
        }

        final int posX = MathHelper.floor_double(player.posX);
        final int posY = MathHelper.floor_double(ForgeHelper.INSTANCE.getEntityBoundingBox(player).minY);
        final int posZ = MathHelper.floor_double(player.posZ);
        final int offset = 1;

        boolean isUnderground = true;

        if (posY < 0)
        {
            return true;
        }

        check:
        {
            int y = posY;
            for (int x = (posX - offset); x <= (posX + offset); x++)
            {
                for (int z = (posZ - offset); z <= (posZ + offset); z++)
                {
                    y = posY + 1;

                    ChunkMD chunkMD = DataCache.instance().getChunkMD(new ChunkCoordIntPair(x >> 4, z >> 4));
                    if (chunkMD != null)
                    {
                        if (chunkMD.ceiling(x & 15, z & 15) <= y)
                        {
                            isUnderground = false;
                            break check;
                        }
                    }
                }
            }
        }

        return isUnderground;
    }

    @Override
    public EntityDTO load(Class aClass) throws Exception
    {
        Minecraft mc = ForgeHelper.INSTANCE.getClient();
        EntityPlayer player = mc.thePlayer;

        EntityDTO dto = DataCache.instance().getEntityDTO(player);
        dto.update(player, false);
        dto.biome = getPlayerBiome(player);
        dto.underground = playerIsUnderground(mc, player);
        return dto;
    }

    /**
     * Get the biome name where the player is standing.
     */
    private String getPlayerBiome(EntityPlayer player)
    {
        int x = (MathHelper.floor_double(player.posX) >> 4) & 15;
        int z = (MathHelper.floor_double(player.posZ) >> 4) & 15;

        ChunkMD playerChunk = DataCache.instance().getChunkMD(new ChunkCoordIntPair(player.chunkCoordX, player.chunkCoordZ));
        if (playerChunk != null)
        {
            try
            {
                BiomeGenBase biome = ForgeHelper.INSTANCE.getBiome(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ));
                // Can be null right after spawn/teleport before chunks loaded
                if (biome != null)
                {
                    return biome.biomeName;
                }
                //return playerChunk.getBiome(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).biomeName;
            }
            catch (Exception e)
            {
                JMLogger.logOnce("Couldn't get player biome: " + e.getMessage(), e);
            }
        }
        return "?";
    }

    public long getTTL()
    {
        return JourneymapClient.getCoreProperties().cachePlayerData.get();
    }
}
