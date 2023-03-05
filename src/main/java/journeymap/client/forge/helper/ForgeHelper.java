/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.helper;

import journeymap.client.forge.helper.impl.ForgeHelper_1_7_10;

/**
 * Delegates to a version-specific implementation of IForgeHelper.INSTANCE.
 */
public class ForgeHelper
{
    public static final IForgeHelper INSTANCE = new ForgeHelper_1_7_10();
}
