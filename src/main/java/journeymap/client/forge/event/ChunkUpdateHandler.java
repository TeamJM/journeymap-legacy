/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.data.DataCache;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.event.world.ChunkEvent;

import java.util.EnumSet;

// 1.8
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.fml.relauncher.Side;
//import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Listen for events which are likely to need the map to be updated.
 */
public class ChunkUpdateHandler implements EventHandlerManager.EventHandler
{
    public ChunkUpdateHandler()
    {
    }

    @Override
    public EnumSet<EventHandlerManager.BusType> getBus()
    {
        return EnumSet.of(EventHandlerManager.BusType.MinecraftForgeBus);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onChunkEvent(ChunkEvent.Unload event)
    {
        ChunkCoordIntPair coord = event.getChunk().getChunkCoordIntPair();
        DataCache.instance().invalidateChunkMD(coord);
    }
}
