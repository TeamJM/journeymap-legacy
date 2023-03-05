/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.multi;

import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.cartography.ChunkRenderController;
import journeymap.client.data.DataCache;
import journeymap.client.feature.Feature;
import journeymap.client.feature.FeatureManager;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.EntityDTO;
import journeymap.client.model.MapType;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapPlayerTask extends BaseMapTask
{
    private static DecimalFormat decFormat = new DecimalFormat("##.#");
    private static volatile long lastTaskCompleted;
    private static long lastTaskTime;
    private static double lastTaskAvgChunkTime;
    private final int maxRuntime = JourneymapClient.getCoreProperties().renderDelay.get() * 3000;
    private int scheduledChunks = 0;
    private long startNs;
    private long elapsedNs;

    private MapPlayerTask(ChunkRenderController chunkRenderController, World world, MapType mapType, Collection<ChunkCoordIntPair> chunkCoords)
    {
        super(chunkRenderController, world, mapType, chunkCoords, false, true, 10000);
    }

    public static void forceNearbyRemap()
    {
        synchronized (MapPlayerTask.class)
        {
            DataCache.instance().invalidateChunkMDCache();
        }
    }

    public static MapPlayerTaskBatch create(ChunkRenderController chunkRenderController, final EntityDTO player)
    {
        final boolean cavesAllowed = FeatureManager.isAllowed(Feature.MapCaves);
        final EntityLivingBase playerEntity = player.entityLivingRef.get();
        if (playerEntity == null)
        {
            return null;
        }
        final boolean worldHasSky = !ForgeHelper.INSTANCE.hasNoSky(playerEntity);
        boolean underground = ForgeHelper.INSTANCE.hasNoSky(playerEntity) || player.underground;

        if (underground && !cavesAllowed)
        {
            if (worldHasSky)
            {
                underground = false;
            }
            else
            {
                return null;
            }
        }

        MapType mapType;
        if (underground)
        {
            mapType = MapType.underground(player);
        }
        else
        {
            final long time = playerEntity.worldObj.getWorldInfo().getWorldTime() % 24000L;
            mapType = (time < 13800) ? MapType.day(player) : MapType.night(player);
        }

        List<ITask> tasks = new ArrayList<ITask>(2);
        tasks.add(new MapPlayerTask(chunkRenderController, playerEntity.worldObj, mapType, new ArrayList<ChunkCoordIntPair>()));

        if (underground)
        {
            if (worldHasSky && JourneymapClient.getCoreProperties().alwaysMapSurface.get())
            {
                tasks.add(new MapPlayerTask(chunkRenderController, playerEntity.worldObj, MapType.day(player), new ArrayList<ChunkCoordIntPair>()));
            }
        }
        else
        {
            if (cavesAllowed && JourneymapClient.getCoreProperties().alwaysMapCaves.get())
            {
                tasks.add(new MapPlayerTask(chunkRenderController, playerEntity.worldObj, MapType.underground(player), new ArrayList<ChunkCoordIntPair>()));
            }
        }

        return new MapPlayerTaskBatch(tasks);
    }

    public static String[] getDebugStats()
    {
        try
        {
            boolean showLastUnderground = false;
            boolean showLastSurface = false;

            if (DataCache.getPlayer().underground || JourneymapClient.getCoreProperties().alwaysMapCaves.get())
            {
                showLastUnderground = true;
            }

            if (!DataCache.getPlayer().underground || JourneymapClient.getCoreProperties().alwaysMapSurface.get())
            {
                showLastSurface = true;
            }

            if (!showLastSurface && !showLastUnderground)
            {
                return new String[0];
            }

            if (showLastSurface != showLastUnderground)
            {
                if (showLastSurface)
                {
                    return new String[]{RenderSpec.getSurfaceSpec().getDebugStats()};
                }
                return new String[]{RenderSpec.getUndergroundSpec().getDebugStats()};
            }
            else
            {
                return new String[]{RenderSpec.getSurfaceSpec().getDebugStats(), RenderSpec.getUndergroundSpec().getDebugStats()};
            }
        }
        catch (Throwable t)
        {
            logger.error(t);
            return new String[0];
        }
    }

    public static String getSimpleStats()
    {
        int primaryRenderSize = 0;
        int secondaryRenderSize = 0;
        int totalChunks = 0;

        if (DataCache.getPlayer().underground || JourneymapClient.getCoreProperties().alwaysMapCaves.get())
        {
            RenderSpec spec = RenderSpec.getUndergroundSpec();
            if (spec != null)
            {
                primaryRenderSize += spec.getPrimaryRenderSize();
                secondaryRenderSize += spec.getLastSecondaryRenderSize();
                totalChunks += spec.getLastTaskChunks();
            }
        }

        if (!DataCache.getPlayer().underground || JourneymapClient.getCoreProperties().alwaysMapSurface.get())
        {
            RenderSpec spec = RenderSpec.getSurfaceSpec();
            if (spec != null)
            {
                primaryRenderSize += spec.getPrimaryRenderSize();
                secondaryRenderSize += spec.getLastSecondaryRenderSize();
                totalChunks += spec.getLastTaskChunks();
            }
        }

        return Constants.getString("jm.common.renderstats",
                totalChunks,
                primaryRenderSize,
                secondaryRenderSize,
                lastTaskTime,
                decFormat.format(lastTaskAvgChunkTime));
    }

    public static long getlastTaskCompleted()
    {
        return lastTaskCompleted;
    }

    @Override
    public void initTask(Minecraft minecraft, JourneymapClient jm, File jmWorldDir, boolean threadLogging) throws InterruptedException
    {
        startNs = System.nanoTime();

        final RenderSpec renderSpec = mapType.isUnderground() ? RenderSpec.getUndergroundSpec() : RenderSpec.getSurfaceSpec();
        chunkCoords.addAll(renderSpec.getRenderAreaCoords());
        this.scheduledChunks = chunkCoords.size();
    }

    @Override
    protected void complete(int mappedChunks, boolean cancelled, boolean hadError)
    {
        elapsedNs = System.nanoTime() - startNs;
    }

    @Override
    public int getMaxRuntime()
    {
        return maxRuntime;
    }

    /**
     * ITaskManager for MapPlayerTasks
     *
     * @author techbrew
     */
    public static class Manager implements ITaskManager
    {
        final int mapTaskDelay = JourneymapClient.getCoreProperties().renderDelay.get() * 1000;

        boolean enabled;

        @Override
        public Class<? extends BaseMapTask> getTaskClass()
        {
            return MapPlayerTask.class;
        }

        @Override
        public boolean enableTask(Minecraft minecraft, Object params)
        {
            enabled = true;
            return enabled;
        }

        @Override
        public boolean isEnabled(Minecraft minecraft)
        {
            return enabled;
        }

        @Override
        public void disableTask(Minecraft minecraft)
        {
            enabled = false;
        }

        @Override
        public ITask getTask(Minecraft minecraft)
        {
            // Ensure player chunk is loaded
            if (enabled && minecraft.thePlayer.addedToChunk)
            {
                if ((System.currentTimeMillis() - lastTaskCompleted) >= mapTaskDelay)
                {
                    ChunkRenderController chunkRenderController = JourneymapClient.getInstance().getChunkRenderController();
                    return MapPlayerTask.create(chunkRenderController, DataCache.getPlayer());
                }
            }

            return null;
        }

        @Override
        public void taskAccepted(ITask task, boolean accepted)
        {
            // nothing to do
        }

    }

    public static class MapPlayerTaskBatch extends TaskBatch
    {
        public MapPlayerTaskBatch(List<ITask> tasks)
        {
            super(tasks);
        }

        @Override
        public void performTask(Minecraft mc, JourneymapClient jm, File jmWorldDir, boolean threadLogging) throws InterruptedException
        {
            if (mc.thePlayer == null)
            {
                return;
            }

            startNs = System.nanoTime();
            List<ITask> tasks = new ArrayList<ITask>(taskList);
            DataCache.instance().invalidateChunkMDCache();

            super.performTask(mc, jm, jmWorldDir, threadLogging);

            elapsedNs = System.nanoTime() - startNs;
            lastTaskTime = TimeUnit.NANOSECONDS.toMillis(elapsedNs);
            lastTaskCompleted = System.currentTimeMillis();

            // Report on timing
            int chunkCount = 0;
            for (ITask task : tasks)
            {
                if (task instanceof MapPlayerTask)
                {
                    MapPlayerTask mapPlayerTask = (MapPlayerTask) task;
                    chunkCount += mapPlayerTask.scheduledChunks;
                    if (mapPlayerTask.mapType.isUnderground())
                    {
                        RenderSpec.getUndergroundSpec().setLastTaskInfo(mapPlayerTask.scheduledChunks, mapPlayerTask.elapsedNs);
                    }
                    else
                    {
                        RenderSpec.getSurfaceSpec().setLastTaskInfo(mapPlayerTask.scheduledChunks, mapPlayerTask.elapsedNs);
                    }
                }
                else
                {
                    Journeymap.getLogger().warn("Unexpected task in batch: " + task);
                }
            }
            lastTaskAvgChunkTime = elapsedNs / Math.max(1, chunkCount) / 1000000D;
        }
    }

}
