/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.common;

// 1.7

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Map;

// 1.8
//import net.minecraftforge.fml.common.event.FMLInitializationEvent;
//import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
//import net.minecraftforge.fml.relauncher.Side;

/**
 * Proxy to provide a common interface for initializing client-side or server-side.
 */
public interface CommonProxy
{
    /**
     * Initialize the side.
     *
     * @param event
     * @throws Throwable
     */
    public void initialize(FMLInitializationEvent event) throws Throwable;

    /**
     * Post-initialize the side.
     *
     * @param event
     * @throws Throwable
     */
    public void postInitialize(FMLPostInitializationEvent event) throws Throwable;

    /**
     * Whether this side will accept being connected to the other side.
     * Since we don't care if the other side has JourneyMap or some other mod, always return true.
     */
    public boolean checkModLists(Map<String, String> modList, Side side);

    /**
     * Whether the update check is enabled.
     *
     * @return
     */
    public boolean isUpdateCheckEnabled();

    /**
     * Handles the response when a world ID packet is received.
     *
     * @param message
     * @param playerEntity
     */
    public void handleWorldIdMessage(String message, EntityPlayerMP playerEntity);
}
