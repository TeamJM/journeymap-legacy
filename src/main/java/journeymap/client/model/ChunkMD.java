/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

import com.google.common.cache.CacheLoader;
import journeymap.client.data.DataCache;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.io.nbt.ChunkLoader;
import journeymap.common.Journeymap;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * ChunkMD is a MetaData wrapper for a Chunk.
 *
 * @author techbrew
 */
public class ChunkMD
{
    public static final String PROP_IS_SLIME_CHUNK = "isSlimeChunk";
    public static final String PROP_LOADED = "loaded";
    public static final String PROP_LAST_RENDERED = "lastRendered";
    final static DataCache dataCache = DataCache.instance();
    private final WeakReference<Chunk> chunkReference;
    private final ChunkCoordIntPair coord;
    private final HashMap<String, Serializable> properties = new HashMap<String, Serializable>();
    private Chunk retainedChunk;

    public ChunkMD(Chunk chunk)
    {
        this(chunk, false);
    }

    public ChunkMD(Chunk chunk, boolean forceRetain)
    {
        if (chunk == null)
        {
            throw new IllegalArgumentException("Chunk can't be null");
        }
        this.coord = new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition); // avoid GC issue holding onto chunk's coord ref

        // Set load time
        setProperty(PROP_LOADED, System.currentTimeMillis());

        // https://github.com/OpenMods/OpenBlocks/blob/master/src/main/java/openblocks/common/item/ItemSlimalyzer.java#L44
        properties.put(PROP_IS_SLIME_CHUNK, chunk.getRandomWithSeed(987234911L).nextInt(10) == 0);

        this.chunkReference = new WeakReference<Chunk>(chunk);
        if (forceRetain)
        {
            retainedChunk = chunk;
        }
    }

    public Block getBlock(int x, int y, int z)
    {
        return getChunk().getBlock(x, y, z);
    }

    public BlockMD getBlockMD(int x, int y, int z)
    {
        return BlockMD.get(getChunk().getBlock(x, y, z), getBlockMeta(x, y, z));
    }

    /**
     * Added to do a safety check on the world height value
     */
    public int getSavedLightValue(int x, int y, int z)
    {
        return ForgeHelper.INSTANCE.getSavedLightValue(getChunk(), x, y, z);
    }

    /**
     * Get the top block ignoring transparent roof blocks, air. etc.
     */
    public final BlockMD getTopBlockMD(final int x, int y, final int z)
    {
        BlockMD topBlockMd = null;

        do
        {
            topBlockMd = BlockMD.getBlockMD(this, x, y, z);

            // Null check
            if (topBlockMd == null)
            {
                break;
            }

            if (topBlockMd.isTransparentRoof() || topBlockMd.isAir() || topBlockMd.getAlpha() == 0)
            {
                y--;
            }
            else
            {
                break;
            }
        } while (y >= 0);

        return topBlockMd;
    }

    /**
     * Get the block meta from the chunk-local coords.
     */
    public int getBlockMeta(final int x, int y, final int z)
    {
        return ForgeHelper.INSTANCE.getBlockMeta(getChunk(), x, y, z);
    }

    /**
     * Finds the top sky-obscuring block in the column.
     */
    public int ceiling(final int x, final int z)
    {
        final int chunkHeight = getPrecipitationHeight(x, z);
        int y = chunkHeight;

        try
        {
            Chunk chunk = getChunk();
            BlockMD blockMD;
            while (y >= 0)
            {
                blockMD = getBlockMD(x, y, z);

                if (blockMD.isAir() || blockMD.hasFlag(BlockMD.Flag.OpenToSky))
                {
                    y--;
                }
                else if (ForgeHelper.INSTANCE.canBlockSeeTheSky(chunk, x, y, z))
                {
                    y--;
                }
                else
                {
                    break;
                }
            }
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn(e + " at " + toWorldX(x) + "," + y + "," + toWorldX(x));
        }

        return Math.max(0, y);
    }

    public boolean hasChunk()
    {
        return chunkReference.get() != null && !(chunkReference.get() instanceof EmptyChunk);
    }

    public int getHeight(int x, int z)
    {
        return ForgeHelper.INSTANCE.getHeight(getChunk(), x, z);
    }

    public int getPrecipitationHeight(int x, int z)
    {
        return ForgeHelper.INSTANCE.getPrecipitationHeight(getChunk(), x, z);
    }

    public int getLightOpacity(BlockMD blockMD, int localX, int y, int localZ)
    {
        return ForgeHelper.INSTANCE.getLightOpacity(blockMD, toWorldX(localX), y, toWorldZ(localZ));
    }

    public Serializable getProperty(String name)
    {
        return properties.get(name);
    }

    public Serializable getProperty(String name, Serializable defaultValue)
    {
        Serializable currentValue = getProperty(name);
        if (currentValue == null)
        {
            setProperty(name, defaultValue);
            currentValue = defaultValue;
        }
        return currentValue;
    }

    public Serializable setProperty(String name, Serializable value)
    {
        return properties.put(name, value);
    }

    @Override
    public int hashCode()
    {
        return getCoord().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        ChunkMD other = (ChunkMD) obj;
        return getCoord().equals(other.getCoord());
    }

    public Chunk getChunk()
    {
        Chunk chunk = chunkReference.get();
        if (chunk == null)
        {
            throw new ChunkMissingException(getCoord());
        }
        return chunk;
    }

    public World getWorld()
    {
        return ForgeHelper.INSTANCE.getWorld(getChunk());
    }

    public int getWorldActualHeight()
    {
        if (getHasNoSky())
        {
            return getWorld().getActualHeight();
        }
        else
        {
            // add one to get above the top block for some worlds that paste in to 256
            return getWorld().getActualHeight() + 1;
        }
    }

    public Boolean getHasNoSky()
    {
        return ForgeHelper.INSTANCE.hasNoSky(getWorld());
    }

    public boolean canBlockSeeTheSky(int x, int y, int z)
    {
        return ForgeHelper.INSTANCE.canBlockSeeTheSky(getChunk(), x, y, z);
    }

    public ChunkCoordIntPair getCoord()
    {
        return coord;
    }

    public long getCoordLong()
    {
        return ChunkCoordIntPair.chunkXZ2Int(coord.chunkXPos, coord.chunkZPos);
    }

    public boolean isSlimeChunk()
    {
        return (Boolean) getProperty(PROP_IS_SLIME_CHUNK, Boolean.FALSE);
    }

    public long getLoaded()
    {
        return (Long) getProperty(PROP_LOADED, 0L);
    }

    public long getLastRendered()
    {
        return (Long) getProperty(PROP_LAST_RENDERED, 0L);
    }

    public long setRendered()
    {
        long now = System.currentTimeMillis();
        setProperty(PROP_LAST_RENDERED, now);
        return now;
    }

    public int toWorldX(int localX)
    {
        return (coord.chunkXPos << 4) + localX;
    }

    public int toWorldZ(int localZ)
    {
        return (coord.chunkZPos << 4) + localZ;
    }

    @Override
    public String toString()
    {
        return "ChunkMD{" +
                "coord=" + coord +
                ", properties=" + properties +
                '}';
    }

    public int getDimension()
    {
        return ForgeHelper.INSTANCE.getDimension(getWorld());
    }

    public void stopChunkRetention()
    {
        this.retainedChunk = null;
    }

    @Override
    protected void finalize() throws Throwable
    {
        if (retainedChunk != null)
        {
            super.finalize();
        }
    }

    public static class ChunkMissingException extends RuntimeException
    {
        ChunkMissingException(ChunkCoordIntPair coord)
        {
            super("Chunk missing: " + coord);
        }
    }

    public static class SimpleCacheLoader extends CacheLoader<Long, ChunkMD>
    {
        Minecraft mc = ForgeHelper.INSTANCE.getClient();

        @Override
        public ChunkMD load(Long coord) throws Exception
        {
            int x = (int) (coord & 4294967295L);
            int z = (int) (coord >>> 32 & 4294967295L);
            return ChunkLoader.getChunkMdFromMemory(mc.theWorld, x, z);
        }
    }
}
