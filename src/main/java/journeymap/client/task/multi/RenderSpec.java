/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.multi;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.data.DataCache;
import journeymap.client.properties.CoreProperties;
import journeymap.client.ui.option.KeyedEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.ChunkCoordIntPair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Mark on 2/27/2015.
 */
public class RenderSpec
{
    private static DecimalFormat decFormat = new DecimalFormat("##.#");
    private static volatile RenderSpec lastSurfaceRenderSpec;
    private static volatile RenderSpec lastUndergroundRenderSpec;
    private static Minecraft minecraft = Minecraft.getMinecraft();
    private final EntityPlayer player;
    private final Boolean underground;
    private final int primaryRenderDistance;
    private final int maxSecondaryRenderDistance;
    private final RevealShape revealShape;
    private ListMultimap<Integer, Offset> offsets = null;
    private ArrayList<ChunkCoordIntPair> primaryRenderCoords;
    private Comparator<ChunkCoordIntPair> comparator;
    private int lastSecondaryRenderDistance;
    private ChunkCoordIntPair lastPlayerCoord;
    private long lastTaskTime;
    private int lastTaskChunks;
    private double lastTaskAvgChunkTime;

    private RenderSpec(Minecraft minecraft, boolean underground)
    {
        this.player = minecraft.thePlayer;
        final CoreProperties props = JourneymapClient.getCoreProperties();
        final int gameRenderDistance = Math.max(1, minecraft.gameSettings.renderDistanceChunks - 1);
        final int mapRenderDistanceMin = underground ? props.renderDistanceCaveMin.get() : props.renderDistanceSurfaceMin.get();
        final int mapRenderDistanceMax = underground ? props.renderDistanceCaveMax.get() : props.renderDistanceSurfaceMax.get();

        this.underground = underground;
        int rdMin = Math.min(gameRenderDistance, mapRenderDistanceMin);
        int rdMax = Math.min(gameRenderDistance, Math.max(rdMin, mapRenderDistanceMax));
        if (rdMin + 1 == rdMax)
        {
            rdMin++;
        }

        this.primaryRenderDistance = rdMin;
        this.maxSecondaryRenderDistance = rdMax;
        this.revealShape = JourneymapClient.getCoreProperties().revealShape.get();

        lastPlayerCoord = new ChunkCoordIntPair(minecraft.thePlayer.chunkCoordX, minecraft.thePlayer.chunkCoordZ);
        lastSecondaryRenderDistance = this.primaryRenderDistance;
    }

    private static Double blockDistance(ChunkCoordIntPair playerCoord, ChunkCoordIntPair coord)
    {
        int x = playerCoord.getCenterXPos() - coord.getCenterXPos();
        int z = playerCoord.getCenterZPosition() - coord.getCenterZPosition();
        return Math.sqrt(x * x + z * z);
    }

    private static Double chunkDistance(ChunkCoordIntPair playerCoord, ChunkCoordIntPair coord)
    {
        int x = playerCoord.chunkXPos - coord.chunkXPos;
        int z = playerCoord.chunkZPos - coord.chunkZPos;
        return Math.sqrt(x * x + z * z);
    }

    static boolean inRange(ChunkCoordIntPair playerCoord, ChunkCoordIntPair coord, int renderDistance, RenderSpec.RevealShape revealShape)
    {
        if (revealShape == RenderSpec.RevealShape.Circle)
        {
            double distance = blockDistance(playerCoord, coord);
            double diff = distance - (renderDistance * 16);
            return diff <= 8; // Makes for fuller circles by letting in chunks that are half-way in the perimeter
        }
        else
        {
            float x = Math.abs(playerCoord.chunkXPos - coord.chunkXPos);
            float z = Math.abs(playerCoord.chunkZPos - coord.chunkZPos);
            return x <= renderDistance && z <= renderDistance;
        }
    }

    private static ListMultimap<Integer, Offset> calculateOffsets(int minOffset, int maxOffset, RevealShape revealShape)
    {
        ListMultimap<Integer, Offset> multimap = ArrayListMultimap.create();

        int offset = maxOffset;
        final int baseX = 0;
        final int baseZ = 0;
        final ChunkCoordIntPair baseCoord = new ChunkCoordIntPair(baseX, baseZ);
        while (offset >= minOffset)
        {
            for (int x = (baseX - offset); x <= (baseX + offset); x++)
            {
                for (int z = (baseZ - offset); z <= (baseZ + offset); z++)
                {
                    ChunkCoordIntPair coord = new ChunkCoordIntPair(x, z);
                    if (revealShape == RevealShape.Square || inRange(baseCoord, coord, offset, revealShape))
                    {
                        multimap.put(offset, new Offset(coord.chunkXPos, coord.chunkZPos));
                    }
                }
            }

            if (offset < maxOffset)
            {
                List<Offset> oneUp = multimap.get(offset + 1);
                oneUp.removeAll(multimap.get(offset));
            }

            offset--;
        }

        return new ImmutableListMultimap.Builder<Integer, Offset>().putAll(multimap).build();
    }

    public static RenderSpec getSurfaceSpec()
    {
        if (lastSurfaceRenderSpec == null
                || lastSurfaceRenderSpec.lastPlayerCoord.chunkXPos != minecraft.thePlayer.chunkCoordX
                || lastSurfaceRenderSpec.lastPlayerCoord.chunkZPos != minecraft.thePlayer.chunkCoordZ)
        {
            RenderSpec newSpec = new RenderSpec(minecraft, false);
            newSpec.copyLastStatsFrom(lastSurfaceRenderSpec);
            lastSurfaceRenderSpec = newSpec;
        }
        return lastSurfaceRenderSpec;
    }

    public static RenderSpec getUndergroundSpec()
    {
        if (lastUndergroundRenderSpec == null
                || lastUndergroundRenderSpec.lastPlayerCoord.chunkXPos != minecraft.thePlayer.chunkCoordX
                || lastUndergroundRenderSpec.lastPlayerCoord.chunkZPos != minecraft.thePlayer.chunkCoordZ)
        {
            RenderSpec newSpec = new RenderSpec(minecraft, true);
            newSpec.copyLastStatsFrom(lastUndergroundRenderSpec);
            lastUndergroundRenderSpec = newSpec;
        }
        return lastUndergroundRenderSpec;
    }

    public static void resetRenderSpecs()
    {
        lastUndergroundRenderSpec = null;
        lastSurfaceRenderSpec = null;
    }

    protected Collection<ChunkCoordIntPair> getRenderAreaCoords()
    {
        // Lazy init offsets on first use
        if (offsets == null)
        {
            offsets = calculateOffsets(primaryRenderDistance, maxSecondaryRenderDistance, revealShape);
        }

        DataCache dataCache = DataCache.instance();

        // Reset coords if player moved
        if (lastPlayerCoord == null || lastPlayerCoord.chunkXPos != player.chunkCoordX || lastPlayerCoord.chunkZPos != player.chunkCoordZ)
        {
            primaryRenderCoords = null;
            lastSecondaryRenderDistance = primaryRenderDistance;
        }
        lastPlayerCoord = new ChunkCoordIntPair(minecraft.thePlayer.chunkCoordX, minecraft.thePlayer.chunkCoordZ);

        // Add min distance coords around player
        if (primaryRenderCoords == null || primaryRenderCoords.isEmpty())
        {
            List<Offset> primaryOffsets = offsets.get(primaryRenderDistance);
            primaryRenderCoords = new ArrayList<ChunkCoordIntPair>(primaryOffsets.size());
            for (Offset offset : primaryOffsets)
            {
                ChunkCoordIntPair primaryCoord = offset.from(lastPlayerCoord);
                primaryRenderCoords.add(primaryCoord);
                dataCache.getChunkMD(primaryCoord);
            }
        }

        if (maxSecondaryRenderDistance == primaryRenderDistance)
        {
            // Someday it may be necessary to return an immutable list if these will be consumed elsewhere
            return primaryRenderCoords;
        }
        else
        {
            if (lastSecondaryRenderDistance == maxSecondaryRenderDistance)
            {
                lastSecondaryRenderDistance = primaryRenderDistance;
            }
            lastSecondaryRenderDistance++;

            List<Offset> secondaryOffsets = offsets.get(lastSecondaryRenderDistance);

            ArrayList<ChunkCoordIntPair> renderCoords = new ArrayList<ChunkCoordIntPair>(primaryRenderCoords.size() + secondaryOffsets.size());
            renderCoords.addAll(primaryRenderCoords);

            for (Offset offset : secondaryOffsets)
            {
                ChunkCoordIntPair secondaryCoord = offset.from(lastPlayerCoord);
                renderCoords.add(secondaryCoord);
                dataCache.getChunkMD(secondaryCoord);
            }

            return renderCoords;
        }
    }

    public Boolean getUnderground()
    {
        return underground;
    }

    public int getPrimaryRenderDistance()
    {
        return primaryRenderDistance;
    }

    public int getMaxSecondaryRenderDistance()
    {
        return maxSecondaryRenderDistance;
    }

    public int getLastSecondaryRenderDistance()
    {
        return lastSecondaryRenderDistance;
    }

    public RevealShape getRevealShape()
    {
        return revealShape;
    }

    public int getLastSecondaryRenderSize()
    {
        if (primaryRenderDistance == maxSecondaryRenderDistance)
        {
            return 0;
        }
        return offsets == null ? 0 : offsets.get(lastSecondaryRenderDistance).size();
    }

    public int getPrimaryRenderSize()
    {
        return offsets == null ? 0 : offsets.get(primaryRenderDistance).size();
    }

    public void setLastTaskInfo(int chunks, long elapsedNs)
    {
        lastTaskChunks = chunks;
        lastTaskTime = TimeUnit.NANOSECONDS.toMillis(elapsedNs);
        lastTaskAvgChunkTime = elapsedNs / Math.max(1, chunks) / 1000000D;
    }

    public int getLastTaskChunks()
    {
        return lastTaskChunks;
    }

    public void copyLastStatsFrom(RenderSpec other)
    {
        if (other != null)
        {
            lastTaskChunks = other.lastTaskChunks;
            lastTaskTime = other.lastTaskTime;
            lastTaskAvgChunkTime = other.lastTaskAvgChunkTime;
        }
    }

    public String getDebugStats()
    {
        String debugString;

        if (primaryRenderDistance != maxSecondaryRenderDistance)
        {
            // Caves: %1$s (%2$s) + %3$s (%4$s) = %5$s chunks in %6$sms (avg %7$sms)
            debugString = underground ? "jm.common.renderstats_debug_cave" : "jm.common.renderstats_debug_surface";

            String avg = decFormat.format(lastTaskAvgChunkTime);
            if (lastTaskAvgChunkTime >= 10)
            {
                avg += "!";
            }

            return Constants.getString(debugString,
                    primaryRenderDistance, getPrimaryRenderSize(),
                    getLastSecondaryRenderDistance(), getLastSecondaryRenderSize(),
                    lastTaskChunks,
                    lastTaskTime,
                    avg);
        }
        else
        {
            // Caves: %1$s = %2$s chunks in %3$sms (avg %4$sms)
            debugString = underground ? "jm.common.renderstats_debug_cave_simple" : "jm.common.renderstats_debug_surface_simple";
            return Constants.getString(debugString,
                    primaryRenderDistance,
                    lastTaskChunks,
                    lastTaskTime,
                    decFormat.format(lastTaskAvgChunkTime));
        }
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

        RenderSpec that = (RenderSpec) o;

        if (maxSecondaryRenderDistance != that.maxSecondaryRenderDistance)
        {
            return false;
        }
        if (primaryRenderDistance != that.primaryRenderDistance)
        {
            return false;
        }
        if (revealShape != that.revealShape)
        {
            return false;
        }
        if (!underground.equals(that.underground))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = underground.hashCode();
        result = 31 * result + primaryRenderDistance;
        result = 31 * result + maxSecondaryRenderDistance;
        result = 31 * result + revealShape.hashCode();
        return result;
    }

    public enum RevealShape implements KeyedEnum
    {
        Square("jm.minimap.shape_square"),
        Circle("jm.minimap.shape_circle");
        public final String key;

        RevealShape(String key)
        {
            this.key = key;
        }

        @Override
        public String getKey()
        {
            return key;
        }

        @Override
        public String toString()
        {
            return Constants.getString(this.key);
        }
    }

    private static class Offset
    {
        final int x;
        final int z;

        private Offset(int x, int z)
        {
            this.x = x;
            this.z = z;
        }

        ChunkCoordIntPair from(ChunkCoordIntPair coord)
        {
            return new ChunkCoordIntPair(coord.chunkXPos + x, coord.chunkZPos + z);
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

            Offset offset = (Offset) o;

            if (x != offset.x)
            {
                return false;
            }
            if (z != offset.z)
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = x;
            result = 31 * result + z;
            return result;
        }
    }
}
