/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.main;

import journeymap.client.JourneymapClient;
import journeymap.client.log.ChatLog;
import journeymap.client.model.RegionImageCache;
import journeymap.client.render.map.GridRenderer;
import journeymap.client.task.multi.MapPlayerTask;
import journeymap.client.task.multi.MapRegionTask;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

public class DeleteMapTask implements IMainThreadTask
{
    private static String NAME = "Tick." + MappingMonitorTask.class.getSimpleName();
    private static Logger LOGGER = Journeymap.getLogger();
    boolean allDims;

    private DeleteMapTask(boolean allDims)
    {
        this.allDims = allDims;
    }

    public static void queue(boolean allDims)
    {
        JourneymapClient.getInstance().queueMainThreadTask(new DeleteMapTask(allDims));
    }

    @Override
    public final IMainThreadTask perform(Minecraft mc, JourneymapClient jm)
    {
        try
        {
            jm.toggleTask(MapPlayerTask.Manager.class, false, false);
            jm.toggleTask(MapRegionTask.Manager.class, false, false);
            GridRenderer.setEnabled(false);

            boolean wasMapping = JourneymapClient.getInstance().isMapping();
            if(wasMapping)
            {
                JourneymapClient.getInstance().stopMapping();
            }

            boolean ok = RegionImageCache.instance().deleteMap(Fullscreen.state(), allDims);
            if (ok)
            {
                ChatLog.announceI18N("jm.common.deletemap_status_done");
            }
            else
            {
                ChatLog.announceI18N("jm.common.deletemap_status_error");
            }

            if(wasMapping)
            {
                JourneymapClient.getInstance().startMapping();
                MapPlayerTask.forceNearbyRemap();
            }

            Fullscreen.state().requireRefresh();
        }
        finally
        {
            GridRenderer.setEnabled(true);
            jm.toggleTask(MapPlayerTask.Manager.class, true, true);
        }
        return null;
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
