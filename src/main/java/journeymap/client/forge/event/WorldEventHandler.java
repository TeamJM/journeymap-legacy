/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.event;

// 1.7.10

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.JourneymapClient;
import journeymap.client.feature.FeatureManager;
import net.minecraftforge.event.world.WorldEvent;

import java.util.EnumSet;

// 1.8
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.fml.relauncher.Side;
//import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author techbrew 1/29/14.
 */
@SideOnly(Side.CLIENT)
public class WorldEventHandler implements EventHandlerManager.EventHandler
{

    String playerName;

    @Override
    public EnumSet<EventHandlerManager.BusType> getBus()
    {
        return EnumSet.of(EventHandlerManager.BusType.MinecraftForgeBus);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void invoke(WorldEvent.Unload event)
    {
        JourneymapClient.getInstance().stopMapping();
        FeatureManager.instance().reset();
    }
}
