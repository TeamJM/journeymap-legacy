/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.JourneymapClient;
import journeymap.client.log.LogFormatter;
import journeymap.client.task.main.EnsureCurrentColorsTask;
import journeymap.client.task.main.IMainThreadTask;
import journeymap.common.Journeymap;
import net.minecraftforge.client.event.TextureStitchEvent;

import java.util.EnumSet;

/**
 * Handles events related to the TextureAtlas for Blocks.
 */
public class TextureAtlasHandler implements EventHandlerManager.EventHandler
{
    IMainThreadTask task = new EnsureCurrentColorsTask();

    @Override
    public EnumSet<EventHandlerManager.BusType> getBus()
    {
        return EnumSet.of(EventHandlerManager.BusType.MinecraftForgeBus);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent()
    public void onTextureStiched(TextureStitchEvent.Post event)
    {
        try
        {
            // 0==blocks, 1==items
            if(event.map.getTextureType()==0)
            {
                JourneymapClient.getInstance().queueMainThreadTask(task);
            }
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn("Error queuing TextureAtlasHandlerTask: " + LogFormatter.toString(e));
        }
    }
}
