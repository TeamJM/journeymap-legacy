/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.common;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.common.version.Version;
import journeymap.server.JourneymapServer;
import journeymap.server.oldservercode.command.CommandJMServerForge;
import journeymap.server.oldservercode.config.ConfigHandler;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;

/**
 * Forge Mod entry point
 */
@Mod(modid = Journeymap.MOD_ID, name = Journeymap.SHORT_MOD_NAME, version = BuildInfo.JM_VERSION, canBeDeactivated = true, dependencies = "required-after:Forge@[" + BuildInfo.FORGE_VERSION + ",)")
public class Journeymap
{
    public static final String MOD_ID = "journeymap";
    public static final String SHORT_MOD_NAME = "JourneyMap";
    public static final Version JM_VERSION = Version.from(BuildInfo.MAJOR, BuildInfo.MINOR, BuildInfo.MICRO, BuildInfo.PATCH, new Version(5, 1, 4, "dev"));
    public static final String FORGE_VERSION = BuildInfo.FORGE_VERSION;
    public static final String WEBSITE_URL = "https://teamjm.github.io/journeymap-docs/";
    public static final String DOWNLOAD_URL = "https://www.curseforge.com/minecraft/mc-mods/journeymap/files/all?page=1&pageSize=20&version=1.7.10";
    public static final String VERSION_URL = "https://api.cfwidget.com/minecraft/mc-mods/journeymap";

    @Mod.Instance(Journeymap.MOD_ID)
    public static Journeymap instance;

    @SidedProxy(clientSide = "journeymap.client.JourneymapClient", serverSide = "journeymap.server.JourneymapServer")
    public static CommonProxy proxy;

    /**
     * Get the common logger.
     */
    public static Logger getLogger()
    {
        return LogManager.getLogger(MOD_ID);
    }

    /**
     * Whether this side will accept being connected to the other side.
     */
    @NetworkCheckHandler
    public boolean checkModLists(Map<String, String> modList, Side side)
    {
        if (proxy == null)
        {
            return true;
        }
        else
        {
            return proxy.checkModLists(modList, side);
        }
    }

    /**
     * Initialize the sided proxy.
     *
     * @param event
     * @throws Throwable
     */
    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) throws Throwable
    {
        proxy.initialize(event);
    }

    /**
     * Post-initialize the sided proxy.
     *
     * @param event
     * @throws Throwable
     */
    @Mod.EventHandler
    public void postInitialize(FMLPostInitializationEvent event) throws Throwable
    {
        proxy.postInitialize(event);
    }

    @SideOnly(Side.SERVER)
    @Mod.EventHandler
    public void preInitEvent(FMLPreInitializationEvent event)
    {
        ConfigHandler.init(new File(event.getModConfigurationDirectory() + "/JourneyMapServer/"));
    }

    @SideOnly(Side.SERVER)
    @Mod.EventHandler
    public void serverStartingEvent(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandJMServerForge());
    }

    @SideOnly(Side.SERVER)
    @Mod.EventHandler
    public void serverStartedEvent(FMLServerStartedEvent event)
    {
        MinecraftServer server = MinecraftServer.getServer();
        JourneymapServer.setWorldName(server.getEntityWorld().getWorldInfo().getWorldName());
        getLogger().info("World ID: " + ConfigHandler.getConfigByWorldName(JourneymapServer.getWorldName()).getWorldID());
    }
}
