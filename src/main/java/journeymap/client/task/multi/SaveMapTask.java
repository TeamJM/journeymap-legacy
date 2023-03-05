/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.multi;

import journeymap.client.JourneymapClient;
import journeymap.client.io.MapSaver;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class SaveMapTask implements ITask
{

    private static final Logger logger = Journeymap.getLogger();

    MapSaver mapSaver;

    private SaveMapTask(MapSaver mapSaver)
    {
        this.mapSaver = mapSaver;
    }

    @Override
    public int getMaxRuntime()
    {
        return 120000;
    }

    @Override
    public void performTask(Minecraft mc, JourneymapClient jm, File jmWorldDir, boolean threadLogging)
    {
        mapSaver.saveMap();
    }

    /**
     * ITaskManager for MapPlayerTasks
     *
     * @author techbrew
     */
    public static class Manager implements ITaskManager
    {

        MapSaver mapSaver;

        @Override
        public Class<? extends ITask> getTaskClass()
        {
            return SaveMapTask.class;
        }

        @Override
        public boolean enableTask(Minecraft minecraft, Object params)
        {
            if (params != null && params instanceof MapSaver)
            {
                mapSaver = (MapSaver) params;
            }
            return isEnabled(minecraft);
        }

        @Override
        public boolean isEnabled(Minecraft minecraft)
        {
            return (mapSaver != null);
        }

        @Override
        public void disableTask(Minecraft minecraft)
        {
            mapSaver = null;
        }

        @Override
        public SaveMapTask getTask(Minecraft minecraft)
        {
            if (mapSaver == null)
            {
                return null;
            }
            return new SaveMapTask(mapSaver);
        }

        @Override
        public void taskAccepted(ITask task, boolean accepted)
        {
            mapSaver = null;
        }

    }
}
