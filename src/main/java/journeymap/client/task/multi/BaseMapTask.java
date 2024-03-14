/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.multi;

import journeymap.client.JourneymapClient;
import journeymap.client.cartography.ChunkRenderController;
import journeymap.client.data.DataCache;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.LogFormatter;
import journeymap.client.log.StatTimer;
import journeymap.client.model.ChunkMD;
import journeymap.client.model.MapType;
import journeymap.client.model.RegionCoord;
import journeymap.client.model.RegionImageCache;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;

public abstract class BaseMapTask implements ITask
{
    static final Logger logger = Journeymap.getLogger();
    protected static ChunkCoordIntPair[] keepAliveOffsets = new ChunkCoordIntPair[]{new ChunkCoordIntPair(0, -1), new ChunkCoordIntPair(-1, 0), new ChunkCoordIntPair(-1, -1)};
    private static BufferedImage blankChunkImage = null;
    private static BufferedImage blankChunkImageUnderground = null;
    final World world;
    final Collection<ChunkCoordIntPair> chunkCoords;
    final boolean flushCacheWhenDone;
    final ChunkRenderController renderController;
    final int elapsedLimit;
    final MapType mapType;
    final boolean asyncFileWrites;

    public BaseMapTask(ChunkRenderController renderController, World world, MapType mapType, Collection<ChunkCoordIntPair> chunkCoords, boolean flushCacheWhenDone, boolean asyncFileWrites, int elapsedLimit)
    {
        this.renderController = renderController;
        this.world = world;
        this.mapType = mapType;
        this.chunkCoords = chunkCoords;
        this.asyncFileWrites = asyncFileWrites;
        this.flushCacheWhenDone = flushCacheWhenDone;
        this.elapsedLimit = elapsedLimit;
    }

    public void initTask(Minecraft mc, JourneymapClient jm, File jmWorldDir, boolean threadLogging) throws InterruptedException
    {

    }


    @Override
    public void performTask(Minecraft mc, JourneymapClient jm, File jmWorldDir, boolean threadLogging) throws InterruptedException
    {
        StatTimer timer = StatTimer.get(getClass().getSimpleName() + ".performTask", 5, elapsedLimit).start();

        initTask(mc, jm, jmWorldDir, threadLogging);

        int count = 0;

        try
        {
            if (mc.theWorld == null)
            {
                this.complete(count, true, false);
                return;
            }

            final Iterator<ChunkCoordIntPair> chunkIter = chunkCoords.iterator();

            // Check the dimension
            int currentDimension = ForgeHelper.INSTANCE.getPlayerDimension();
            if (currentDimension != mapType.dimension)
            {
                if (threadLogging)
                {
                    logger.debug("Dimension changed, map task obsolete."); //$NON-NLS-1$
                }
                timer.cancel();
                this.complete(count, true, false);
                return;
            }

            // Map the chunks
            while (chunkIter.hasNext())
            {
                if (!jm.isMapping())
                {
                    if (threadLogging)
                    {
                        logger.debug("JM isn't mapping, aborting"); //$NON-NLS-1$
                    }
                    timer.cancel();
                    this.complete(count, true, false);
                    return;
                }

                if (Thread.interrupted())
                {
                    throw new InterruptedException();
                }

                ChunkCoordIntPair coord = chunkIter.next();
                ChunkMD chunkMd = DataCache.instance().getChunkMD(ChunkCoordIntPair.chunkXZ2Int(coord.chunkXPos, coord.chunkZPos));
                if (chunkMd != null && chunkMd.hasChunk())
                {
                    try
                    {
                        RegionCoord rCoord = RegionCoord.fromChunkPos(jmWorldDir, mapType, chunkMd.getCoord().chunkXPos, chunkMd.getCoord().chunkZPos);
                        renderController.renderChunk(rCoord, mapType, chunkMd);
                        count++;
                    }
                    catch (Throwable t)
                    {
                        logger.warn("Error rendering chunk " + chunkMd + ": " + t.getMessage());
                    }
                }
            }

            if (!jm.isMapping())
            {
                if (threadLogging)
                {
                    logger.debug("JM isn't mapping, aborting.");  //$NON-NLS-1$
                }
                timer.cancel();
                this.complete(count, true, false);
                return;
            }

            if (Thread.interrupted())
            {
                timer.cancel();
                throw new InterruptedException();
            }

            // Push chunk cache to region cache
            RegionImageCache.instance().updateTextures(flushCacheWhenDone, asyncFileWrites);
            chunkCoords.clear();
            this.complete(count, false, false);
            timer.stop();
        }
        catch (InterruptedException t)
        {
            Journeymap.getLogger().warn("Task thread interrupted: " + this);
            timer.cancel();
            throw t;
        }
        catch (Throwable t)
        {
            String error = "Unexpected error in BaseMapTask: " + (LogFormatter.toString(t));
            Journeymap.getLogger().error(error);
            this.complete(count, false, true);
            timer.cancel();
        }
        finally
        {
            if (threadLogging)
            {
                timer.report();
            }
        }
    }

    protected abstract void complete(int mappedChunks, boolean cancelled, boolean hadError);

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "{" +
                "world=" + world +
                ", mapType=" + mapType +
                ", chunkCoords=" + chunkCoords +
                ", flushCacheWhenDone=" + flushCacheWhenDone +
                '}';
    }
}
