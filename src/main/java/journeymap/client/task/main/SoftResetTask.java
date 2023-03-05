/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.main;

import journeymap.client.JourneymapClient;
import journeymap.client.data.DataCache;
import journeymap.client.forge.event.MiniMapOverlayHandler;
import journeymap.client.io.ThemeFileHandler;
import journeymap.client.log.JMLogger;
import journeymap.client.render.map.TileDrawStepCache;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.waypoint.WaypointStore;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

/**
 * Checks state to start/stop mapping (code formerly in JourneyMap.java)
 */
public class SoftResetTask implements IMainThreadTask
{
    private static String NAME = "Tick." + SoftResetTask.class.getSimpleName();
    Logger logger = Journeymap.getLogger();

    private SoftResetTask()
    {
    }

    public static void queue()
    {
        JourneymapClient.getInstance().queueMainThreadTask(new SoftResetTask());
    }

    @Override
    public IMainThreadTask perform(Minecraft mc, JourneymapClient jm)
    {
        jm.loadConfigProperties();
        JMLogger.setLevelFromProperties();
        DataCache.instance().purge();
        TileDrawStepCache.instance().invalidateAll();
        UIManager.getInstance().reset();
        WaypointStore.instance().reset();
        MiniMapOverlayHandler.checkEventConfig();
        ThemeFileHandler.getCurrentTheme(true);
        MiniMap.state().requireRefresh();
        Fullscreen.state().requireRefresh();
        UIManager.getInstance().getMiniMap().updateDisplayVars(true);
        return null;
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
