/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.multi;

import journeymap.client.JourneymapClient;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.ChunkMD;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.List;

/**
 * Created by Mark on 7/16/2014.
 */
public class TaskBatch implements ITask
{
    final List<ITask> taskList;
    final int timeout;
    protected long startNs;
    protected long elapsedNs;

    public TaskBatch(List<ITask> tasks)
    {
        taskList = tasks;
        int timeout = 0;
        for (ITask task : tasks)
        {
            timeout += task.getMaxRuntime();
        }
        this.timeout = timeout;
    }

    @Override
    public int getMaxRuntime()
    {
        return timeout;
    }

    @Override
    public void performTask(final Minecraft mc, final JourneymapClient jm, final File jmWorldDir, final boolean threadLogging) throws InterruptedException
    {
        if (startNs == 0)
        {
            startNs = System.nanoTime();
        }

        if (threadLogging)
        {
            Journeymap.getLogger().debug("START batching tasks");
        }

        while (!taskList.isEmpty())
        {
            if (Thread.interrupted())
            {
                Journeymap.getLogger().warn("TaskBatch thread interrupted: " + this);
                throw new InterruptedException();
            }

            ITask task = taskList.remove(0);
            try
            {
                if (threadLogging)
                {
                    Journeymap.getLogger().debug("Batching task: " + task);
                }
                task.performTask(mc, jm, jmWorldDir, threadLogging);
            }
            catch (ChunkMD.ChunkMissingException e)
            {
                Journeymap.getLogger().warn(e.getMessage());
            }
            catch (Throwable t)
            {
                Journeymap.getLogger().error(String.format("Unexpected error during task batch: %s", LogFormatter.toString(t)));
            }
        }

        if (threadLogging)
        {
            Journeymap.getLogger().debug("DONE batching tasks");
        }

        elapsedNs = System.nanoTime() - startNs;
    }
}
