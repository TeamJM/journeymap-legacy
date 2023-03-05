/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.io.nbt;

import journeymap.client.io.FileHandler;
import journeymap.client.io.RegionImageHandler;
import journeymap.client.model.MapType;
import journeymap.client.model.RegionCoord;
import journeymap.client.model.RegionImageCache;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegionLoader
{

    private static final Pattern anvilPattern = Pattern.compile("r\\.([^\\.]+)\\.([^\\.]+)\\.mca");

    final Logger logger = Journeymap.getLogger();

    final MapType mapType;
    final Stack<RegionCoord> regions;
    final int regionsFound;

    public RegionLoader(final Minecraft minecraft, final MapType mapType, boolean all) throws IOException
    {
        this.mapType = mapType;
        this.regions = findRegions(minecraft, mapType, all);
        this.regionsFound = regions.size();
    }

    public static File getRegionFile(Minecraft minecraft, int dimension, int chunkX, int chunkZ)
    {
        File regionDir = new File(FileHandler.getWorldSaveDir(minecraft), "region");
        File regionFile = new File(regionDir, String.format("r.%s.%s.mca", (chunkX >> 5), (chunkZ >> 5)));
        return regionFile;
    }

    public static File getRegionFile(Minecraft minecraft, int chunkX, int chunkZ)
    {
        File regionDir = new File(FileHandler.getWorldSaveDir(minecraft), "region");
        File regionFile = new File(regionDir, String.format("r.%s.%s.mca", (chunkX >> 5), (chunkZ >> 5)));
        return regionFile;
    }

    public Iterator<RegionCoord> regionIterator()
    {
        return regions.iterator();
    }

    public Stack<RegionCoord> getRegions()
    {
        return regions;
    }

    public int getRegionsFound()
    {
        return regionsFound;
    }

    public boolean isUnderground()
    {
        return mapType.isUnderground();
    }

    public Integer getVSlice()
    {
        return mapType.vSlice;
    }

    Stack<RegionCoord> findRegions(final Minecraft mc, final MapType mapType, boolean all)
    {

        final File mcWorldDir = FileHandler.getMCWorldDir(mc, mapType.dimension);
        final File regionDir = new File(mcWorldDir, "region");
        if (!regionDir.exists() || regionDir.isFile())
        {
            logger.warn("MC world region directory doesn't exist: " + regionDir);
            return null;
        }

        // Flush synchronously so it's done before clearing
        RegionImageCache.instance().flushToDisk(false);
        RegionImageCache.instance().clear();

        final File jmImageWorldDir = FileHandler.getJMWorldDir(mc);
        final Stack<RegionCoord> stack = new Stack<RegionCoord>();

        AnvilChunkLoader anvilChunkLoader = new AnvilChunkLoader(FileHandler.getWorldSaveDir(mc));

        int validFileCount = 0;
        int existingImageCount = 0;
        final File[] anvilFiles = regionDir.listFiles();
        for (File anvilFile : anvilFiles)
        {
            Matcher matcher = anvilPattern.matcher(anvilFile.getName());
            if (!anvilFile.isDirectory() && matcher.matches())
            {
                validFileCount++;
                String x = matcher.group(1);
                String z = matcher.group(2);
                if (x != null && z != null)
                {
                    RegionCoord rc = new RegionCoord(jmImageWorldDir, Integer.parseInt(x), Integer.parseInt(z), mapType.dimension);
                    if (all)
                    {
                        stack.add(rc);
                    }
                    else
                    {
                        if (!RegionImageHandler.getRegionImageFile(rc, mapType, false).exists())
                        {
                            List<ChunkCoordIntPair> chunkCoords = rc.getChunkCoordsInRegion();
                            for (ChunkCoordIntPair coord : chunkCoords)
                            {
                                if (anvilChunkLoader.chunkExists(mc.theWorld, coord.chunkXPos, coord.chunkZPos))
                                {
                                    stack.add(rc);
                                    break;
                                }
                            }
                        }
                        else
                        {
                            existingImageCount++;
                        }
                    }
                }
            }
        }
        if (stack.isEmpty() && (validFileCount != existingImageCount))
        {
            logger.warn("Anvil region files in " + regionDir + ": " + validFileCount + ", matching image files: " + existingImageCount + ", but found nothing to do for mapType " + mapType);
        }

        // Add player's current region
        final RegionCoord playerRc = RegionCoord.fromChunkPos(jmImageWorldDir, mapType, mc.thePlayer.chunkCoordX, mc.thePlayer.chunkCoordZ);
        if (stack.contains(playerRc))
        {
            stack.remove(playerRc);
        }

        Collections.sort(stack, new Comparator<RegionCoord>()
        {
            @Override
            public int compare(RegionCoord o1, RegionCoord o2)
            {
                Float d1 = distanceToPlayer(o1);
                Float d2 = distanceToPlayer(o2);
                int comp = d2.compareTo(d1);
                if (comp == 0)
                {
                    return o2.compareTo(o1);
                }
                return comp;
            }

            float distanceToPlayer(RegionCoord rc)
            {
                float x = rc.regionX - playerRc.regionX;
                float z = rc.regionZ - playerRc.regionZ;
                return (x * x) + (z * z);
            }

        });
        stack.add(playerRc);
        return stack;
    }

    public MapType getMapType()
    {
        return mapType;
    }
}
