/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.io.migrate;

import journeymap.client.log.LogFormatter;
import journeymap.common.Journeymap;
import journeymap.common.version.Version;

import java.util.concurrent.Callable;

/**
 * Run migration tasks based on current version
 */
public class Migration
{
    Task[] tasks = new Task[]{new Migrate5_0_0()};

    public boolean performTasks()
    {
        boolean success = true;
        try
        {
            for (Task task : tasks)
            {
                success = task.call() && success;
            }
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().fatal(LogFormatter.toString(t));
            success = false;
        }

        if (!success)
        {
            Journeymap.getLogger().fatal("Migration failed! JourneyMap is likely to experience significant errors.");
        }

        return success;
    }

    public interface Task extends Callable<Boolean>
    {
        public Version getRequiredVersion();
    }
}
