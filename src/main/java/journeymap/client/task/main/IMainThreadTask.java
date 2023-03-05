/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.main;

import journeymap.client.JourneymapClient;
import net.minecraft.client.Minecraft;

/**
 * Created by Mark on 3/21/2015.
 */
public interface IMainThreadTask
{
    public IMainThreadTask perform(Minecraft mc, JourneymapClient jm);

    public String getName();
}
