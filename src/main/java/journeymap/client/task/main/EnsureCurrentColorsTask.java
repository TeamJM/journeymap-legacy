/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.main;

import journeymap.client.JourneymapClient;
import journeymap.client.cartography.ColorManager;
import net.minecraft.client.Minecraft;

/**
 * Ensures color palette is current.
 */
public class EnsureCurrentColorsTask implements IMainThreadTask
{
    @Override
    public IMainThreadTask perform(Minecraft mc, JourneymapClient jm)
    {
        ColorManager.instance().ensureCurrent();
        return null;
    }

    @Override
    public String getName()
    {
        return "EnsureCurrentColorsTask";
    }
}