/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.main;

import journeymap.client.JourneymapClient;
import journeymap.client.log.ChatLog;
import journeymap.client.log.LogFormatter;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import org.apache.logging.log4j.Logger;

/**
 * Checks state to start/stop mapping (code formerly in JourneyMap.java)
 */
public class MappingMonitorTask implements IMainThreadTask
{
    private static String NAME = "Tick." + MappingMonitorTask.class.getSimpleName();
    Logger logger = Journeymap.getLogger();
    private int lastDimension = 0;

    @Override
    public IMainThreadTask perform(Minecraft mc, JourneymapClient jm)
    {
        //long start = System.nanoTime();
        try
        {
            if (!jm.isInitialized())
            {
                return this;
            }

            final boolean isDead = mc.currentScreen != null && mc.currentScreen instanceof GuiGameOver;

            if (mc.theWorld == null)
            {
                if (jm.isMapping())
                {
                    jm.stopMapping();
                }

                GuiScreen guiScreen = mc.currentScreen;
                if (guiScreen instanceof GuiMainMenu ||
                        guiScreen instanceof GuiSelectWorld ||
                        guiScreen instanceof GuiMultiplayer)
                {
                    if (jm.getCurrentWorldId() != null)
                    {
                        logger.info("World ID has been reset.");
                        jm.setCurrentWorldId(null);
                    }
                }

                return this;
            }
            else if (lastDimension != mc.thePlayer.dimension)
            {
                lastDimension = mc.thePlayer.dimension;
                if (jm.isMapping())
                {
                    jm.stopMapping();
                }
            }
            else
            {
                if (!jm.isMapping() && !isDead && JourneymapClient.getCoreProperties().mappingEnabled.get())
                {
                    jm.startMapping();
                }
            }

            final boolean isGamePaused = mc.currentScreen != null && !(mc.currentScreen instanceof Fullscreen);
            if (isGamePaused)
            {
                if (!jm.isMapping())
                {
                    return this;
                }
            }

            // Show announcements
            if (!isGamePaused)
            {
                ChatLog.showChatAnnouncements(mc);
            }

            // Start Mapping
            if (!jm.isMapping() && JourneymapClient.getCoreProperties().mappingEnabled.get())
            {
                jm.startMapping();
            }
        }
        catch (Throwable t)
        {
            logger.error("Error in JourneyMap.performMainThreadTasks(): " + LogFormatter.toString(t));
        }
        return this;
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
