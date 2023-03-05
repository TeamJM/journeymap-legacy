/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

import com.google.common.cache.Cache;
import journeymap.client.data.DataCache;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.io.nbt.RegionLoader;
import net.minecraft.world.ChunkCoordIntPair;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RegionCoord implements Comparable<RegionCoord>
{
    public transient static final int SIZE = 5;
    private transient static final int chunkSqRt = (int) Math.pow(2, SIZE);
    // TODO: worldDir should serialize as a relative path to allow data files to be usable after being moved
    public final File worldDir;
    public final Path dimDir;
    public final int regionX;
    public final int regionZ;
    public final int dimension;
    private final int theHashCode;
    private final String theCacheKey;

    public RegionCoord(File worldDir, int regionX, int regionZ, int dimension)
    {
        this.worldDir = worldDir;
        this.dimDir = getDimPath(worldDir, dimension);
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.dimension = dimension;
        this.theCacheKey = toCacheKey(dimDir, regionX, regionZ);
        this.theHashCode = theCacheKey.hashCode();
    }

    public static RegionCoord fromChunkPos(File worldDir, MapType mapType, int chunkX, int chunkZ)
    {
        final int regionX = getRegionPos(chunkX);
        final int regionZ = getRegionPos(chunkZ);

        return fromRegionPos(worldDir, regionX, regionZ, mapType.dimension);
    }

    public static RegionCoord fromRegionPos(File worldDir, int regionX, int regionZ, int dimension)
    {
        // The cache is primarily just used to reduce heap thrash.  Hashing on x,z has a lot of collisions,
        // unfortunately, so there's no reliable key.  If there's a collision, we put in a new one.
        Cache<String, RegionCoord> cache = DataCache.instance().getRegionCoords();
        RegionCoord regionCoord = cache.getIfPresent(toCacheKey(getDimPath(worldDir, dimension), regionX, regionZ));
        if (regionCoord == null || regionX != regionCoord.regionX || regionZ != regionCoord.regionZ || dimension != regionCoord.dimension)
        {
            regionCoord = new RegionCoord(worldDir, regionX, regionZ, dimension);
            cache.put(regionCoord.theCacheKey, regionCoord);
        }
        return regionCoord;
    }

    public static Path getDimPath(File worldDir, int dimension)
    {
        return new File(worldDir, "DIM" + dimension).toPath();
    }

    public static int getMinChunkX(int rX)
    {
        return rX << SIZE;
    }

    public static int getMaxChunkX(int rX)
    {
        return getMinChunkX(rX) + (int) Math.pow(2, SIZE) - 1;
    }

    public static int getMinChunkZ(int rZ)
    {
        return rZ << SIZE;
    }

    public static int getMaxChunkZ(int rZ)
    {
        return getMinChunkZ(rZ) + (int) Math.pow(2, SIZE) - 1;
    }

    public static int getRegionPos(int chunkPos)
    {
        return chunkPos >> SIZE;
    }

    public static String toCacheKey(Path dimDir, int regionX, int regionZ)
    {
        return regionX + dimDir.toString() + regionZ;
    }

    public boolean exists()
    {
        return RegionLoader.getRegionFile(ForgeHelper.INSTANCE.getClient(), getMinChunkX(), getMinChunkZ()).exists();
    }

    public int getXOffset(int chunkX)
    {
        if (chunkX >> SIZE != regionX)
        {
            throw new IllegalArgumentException("chunkX " + chunkX + " out of bounds for regionX " + regionX); //$NON-NLS-1$ //$NON-NLS-2$
        }
        int offset = ((chunkX % chunkSqRt) * 16);
        if (offset < 0)
        {
            offset = (chunkSqRt * 16) + offset;
        }
        return offset;
    }

    public int getZOffset(int chunkZ)
    {
        if (getRegionPos(chunkZ) != regionZ)
        {
            throw new IllegalArgumentException("chunkZ " + chunkZ + " out of bounds for regionZ " + regionZ); //$NON-NLS-1$ //$NON-NLS-2$
        }
        int offset = ((chunkZ % chunkSqRt) * 16);
        if (offset < 0)
        {
            offset = (chunkSqRt * 16) + offset;
        }
        return offset;
    }

    public int getMinChunkX()
    {
        return getMinChunkX(regionX);
    }

    public int getMaxChunkX()
    {
        return getMaxChunkX(regionX);
    }

    public int getMinChunkZ()
    {
        return getMinChunkZ(regionZ);
    }

    public int getMaxChunkZ()
    {
        return getMaxChunkZ(regionZ);
    }

    public ChunkCoordIntPair getMinChunkCoord()
    {
        return new ChunkCoordIntPair(getMinChunkX(), getMinChunkZ());
    }

    public ChunkCoordIntPair getMaxChunkCoord()
    {
        return new ChunkCoordIntPair(getMaxChunkX(), getMaxChunkZ());
    }

    public List<ChunkCoordIntPair> getChunkCoordsInRegion()
    {
        final List<ChunkCoordIntPair> list = new ArrayList<ChunkCoordIntPair>(1024);
        final ChunkCoordIntPair min = getMinChunkCoord();
        final ChunkCoordIntPair max = getMaxChunkCoord();

        for (int x = min.chunkXPos; x <= max.chunkXPos; x++)
        {
            for (int z = min.chunkZPos; z <= max.chunkZPos; z++)
            {
                list.add(new ChunkCoordIntPair(x, z));
            }
        }

        return list;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("RegionCoord ["); //$NON-NLS-1$
        builder.append(regionX);
        builder.append(","); //$NON-NLS-1$
        builder.append(regionZ);
        builder.append("]"); //$NON-NLS-1$
        return builder.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        RegionCoord that = (RegionCoord) o;

        if (dimension != that.dimension)
        {
            return false;
        }
        if (regionX != that.regionX)
        {
            return false;
        }
        if (regionZ != that.regionZ)
        {
            return false;
        }
        if (!dimDir.equals(that.dimDir))
        {
            return false;
        }
        if (!worldDir.equals(that.worldDir))
        {
            return false;
        }

        return true;
    }

    public String cacheKey()
    {
        return theCacheKey;
    }

    @Override
    public int hashCode()
    {
        return theHashCode;
    }

    @Override
    public int compareTo(RegionCoord o)
    {
        int cx = Double.compare(this.regionX, o.regionX);
        return (cx == 0) ? Double.compare(this.regionZ, o.regionZ) : cx;
    }
}
