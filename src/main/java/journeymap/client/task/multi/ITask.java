/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.multi;

import journeymap.client.JourneymapClient;
import net.minecraft.client.Minecraft;

import java.io.File;

public interface ITask
{
    public int getMaxRuntime();

    public void performTask(Minecraft mc, JourneymapClient jm, File jmWorldDir, boolean threadLogging) throws InterruptedException;
}
