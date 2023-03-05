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
@Mod(modid = Journeymap.MOD_ID, name = Journeymap.SHORT_MOD_NAME, version = "@JMVERSION@", canBeDeactivated = true, dependencies = "required-after:Forge@[${@FORGEVERSION@},)")
public class Journeymap
{
    public static final String MOD_ID = "journeymap";
    public static final String SHORT_MOD_NAME = "JourneyMap";
    public static final Version JM_VERSION = Version.from("@MAJOR@", "@MINOR@", "@MICRO@", "@PATCH@", new Version(5, 1, 4, "dev"));
    public static final String FORGE_VERSION = "@FORGEVERSION@";
    public static final String WEBSITE_URL = "http://journeymap.info/";
    public static final String DOWNLOAD_URL = "http://minecraft.curseforge.com/projects/journeymap-32274/files/";
    public static final String VERSION_URL = "http://widget.mcf.li/mc-mods/minecraft/journeymap-32274.json";

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