/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.common.CommonProxy;
import journeymap.common.Journeymap;
import journeymap.common.network.PacketHandler;
import journeymap.server.nbt.WorldNbtIDSaveHandler;
import journeymap.server.oldservercode.chat.ChatHandler;
import journeymap.server.oldservercode.events.ForgeEvents;
import journeymap.server.oldservercode.network.ForgePacketHandler;
import journeymap.server.oldservercode.network.PacketManager;
import journeymap.server.oldservercode.reference.Controller;
import journeymap.server.oldservercode.util.ForgeChat;
import journeymap.server.oldservercode.util.ForgePlayerUtil;
import journeymap.server.oldservercode.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

import java.util.Map;

// 1.8
//import net.minecraftforge.fml.common.FMLCommonHandler;
//import net.minecraftforge.fml.common.event.FMLInitializationEvent;
//import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
//import net.minecraftforge.fml.relauncher.Side;
//import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * Coming soon to a codebase near you.
 */
@SideOnly(Side.SERVER)
public class JourneymapServer implements CommonProxy
{
    public static String WORLD_NAME;

    private Logger logger;

    /**
     * Constructor.
     */
    public JourneymapServer()
    {
        logger = Journeymap.getLogger();
    }

    public static String getWorldName()
    {
        return WORLD_NAME;
    }

    public static void setWorldName(String worldName)
    {
        WORLD_NAME = worldName;
    }

    /**
     * Initialize the server.
     *
     * @param event
     */
    @SideOnly(Side.SERVER)
    @Override
    public void initialize(FMLInitializationEvent event)
    {
//        PacketHandler packetHandler = new PacketHandler();
//        packetHandler.init(Side.SERVER);
        Controller.setController(Controller.FORGE);
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
        //FMLCommonHandler.instance().bus().register(new FMLEvents());
        PacketManager.init(new ForgePacketHandler());
        PlayerUtil.init(new ForgePlayerUtil());
        ChatHandler.init(new ForgeChat());
    }

    /**
     * Post-initialize the server
     *
     * @param event
     */
    @SideOnly(Side.SERVER)
    @Override
    public void postInitialize(FMLPostInitializationEvent event)
    {

    }

    /**
     * Accept any modlist on client
     *
     * @param modList
     * @param side
     * @return
     */
    @Override
    public boolean checkModLists(Map<String, String> modList, Side side)
    {

        logger.info(side.toString());

        for (String s : modList.keySet())
        {
            //logger.info("MOD Key: " + s + " MOD Value: " + modList.get(s));
        }
        // TODO: Check for JM client and enable/disable worldid checking, etc.
        return true;
    }

    /**
     * Whether the update check is enabled.
     *
     * @return
     */
    @Override
    public boolean isUpdateCheckEnabled()
    {
        // TODO: Make this configurable
        return false;
    }

    @Override
    public void handleWorldIdMessage(String message, EntityPlayerMP playerEntity)
    {
        WorldNbtIDSaveHandler nbt = new WorldNbtIDSaveHandler();
        PacketHandler.sendPlayerWorldID(nbt.getWorldID(), playerEntity);
    }
}
