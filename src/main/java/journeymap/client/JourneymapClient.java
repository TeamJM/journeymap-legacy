/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.cartography.ChunkRenderController;
import journeymap.client.cartography.ColorManager;
import journeymap.client.data.DataCache;
import journeymap.client.data.WaypointsData;
import journeymap.client.feature.FeatureManager;
import journeymap.client.forge.event.EventHandlerManager;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.io.FileHandler;
import journeymap.client.io.IconSetFileHandler;
import journeymap.client.io.ThemeFileHandler;
import journeymap.client.io.migrate.Migration;
import journeymap.client.log.ChatLog;
import journeymap.client.log.JMLogger;
import journeymap.client.log.LogFormatter;
import journeymap.client.log.StatTimer;
import journeymap.client.model.RegionImageCache;
import journeymap.client.network.WorldInfoHandler;
import journeymap.client.properties.*;
import journeymap.client.render.map.TileDrawStepCache;
import journeymap.client.task.main.IMainThreadTask;
import journeymap.client.task.main.MainTaskController;
import journeymap.client.task.main.MappingMonitorTask;
import journeymap.client.task.multi.ITaskManager;
import journeymap.client.task.multi.TaskController;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.waypoint.WaypointStore;
import journeymap.client.webmap.WebMap;
import journeymap.common.CommonProxy;
import journeymap.common.Journeymap;
import journeymap.common.version.VersionCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;

/**
 * Client-side, strong-side!
 */
@SideOnly(Side.CLIENT)
public class JourneymapClient implements CommonProxy
{
    public static final String FULL_VERSION = Journeymap.FORGE_VERSION + "-" + Journeymap.JM_VERSION;
    public static final String EDITION = getEdition();
    public static final String MOD_NAME = Journeymap.SHORT_MOD_NAME + " " + EDITION;
    private static JourneymapClient instance;

    // Properties & preferences
    private volatile CoreProperties coreProperties;
    private volatile FullMapProperties fullMapProperties;
    private volatile MiniMapProperties miniMapProperties1;
    private volatile MiniMapProperties2 miniMapProperties2;
    private volatile WebMapProperties webMapProperties;
    private volatile WaypointProperties waypointProperties;
    private volatile Boolean initialized = false;
    private volatile String currentWorldId = null;
    private volatile Minecraft mc;
    private Logger logger;
    private boolean threadLogging = false;

    // Task controller for issuing tasks in executor
    private TaskController multithreadTaskController;
    private ChunkRenderController chunkRenderController;

    // Controller for tasks that have to be done on the main thread
    private volatile MainTaskController mainThreadTaskController;

    /**
     * Constructor.
     */
    public JourneymapClient()
    {
        if (instance != null)
        {
            throw new IllegalArgumentException("Use instance() after initialization is complete");
        }
        instance = this;
    }

    /**
     * Get the instance
     *
     * @return
     */
    public static JourneymapClient getInstance()
    {
        return instance;
    }

    /**
     * Build the edition string.
     *
     * @return
     */
    private static String getEdition()
    {
        String ed = null;
        try
        {
            ed = Journeymap.JM_VERSION + " " + FeatureManager.getPolicySetName();
        }
        catch (Throwable t)
        {
            ed = Journeymap.JM_VERSION + " ?";
            t.printStackTrace(System.err);
        }
        return ed;
    }

    /**
     * Get the core properties.
     */
    public static CoreProperties getCoreProperties()
    {
        return instance.coreProperties;
    }

    /**
     * Get the fullmap properties.
     */
    public static FullMapProperties getFullMapProperties()
    {
        return instance.fullMapProperties;
    }

    /**
     * Disable the mod.
     */
    public static void disable()
    {
        instance.initialized = false;
        EventHandlerManager.unregisterAll();
        instance.stopMapping();
        DataCache.instance().purge();
    }

    /**
     * Get the minimap properties for the active minimap.
     */
    public static MiniMapProperties getMiniMapProperties(int which)
    {
        switch (which)
        {
            case 2:
            {
                instance.miniMapProperties2.setActive(true);
                instance.miniMapProperties1.setActive(false);
                return getMiniMapProperties2();
            }
            default:
            {
                instance.miniMapProperties1.setActive(true);
                instance.miniMapProperties2.setActive(false);
                return getMiniMapProperties1();
            }
        }
    }

    /**
     * Get the active minimap id.
     */
    public static int getActiveMinimapId()
    {
        if (instance.miniMapProperties1.isActive())
        {
            return 1;
        }
        else
        {
            return 2;
        }
    }

    /**
     * Get properties for minimap 1.
     */
    public static MiniMapProperties getMiniMapProperties1()
    {
        return instance.miniMapProperties1;
    }

    /**
     * Get properties for minimap 2.
     */
    public static MiniMapProperties getMiniMapProperties2()
    {
        return instance.miniMapProperties2;
    }

    /**
     * Get the webmap properties.
     */
    public static WebMapProperties getWebMapProperties()
    {
        return instance.webMapProperties;
    }

    /**
     * Get the waypoint properties.
     */
    public static WaypointProperties getWaypointProperties()
    {
        return instance.waypointProperties;
    }

    /**
     * Initialize the client.
     *
     * @param event
     * @throws Throwable
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void initialize(FMLInitializationEvent event) throws Throwable
    {
        StatTimer timer = null;
        try
        {
            timer = StatTimer.getDisposable("elapsed").start();

            // Migrate tasks
            boolean migrationOk = new Migration().performTasks();

            // Ensure logger inits
            logger = JMLogger.init();
            logger.info("initialize ENTER");

            if (initialized)
            {
                logger.warn("Already initialized, aborting");
                return;
            }

            // Trigger statics on EntityList (may not be needed anymore?)
            EntityRegistry.instance();

            // Load properties
            loadConfigProperties();

            // Log properties
            JMLogger.logProperties();

            // Logging for thread debugging
            threadLogging = false;

            logger.info("initialize EXIT, " + (timer == null ? "" : timer.getLogReportString()));
        }
        catch (Throwable t)
        {
            if (logger == null)
            {
                logger = LogManager.getLogger(Journeymap.MOD_ID);
            }
            logger.error(LogFormatter.toString(t));
            throw t;
        }
    }

    /**
     * Post-initialize the client.
     *
     * @param event
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void postInitialize(FMLPostInitializationEvent event)
    {
        StatTimer timer = null;
        try
        {
            logger.info("postInitialize ENTER");
            timer = StatTimer.getDisposable("elapsed").start();

            // Get ref to Minecraft
            mc = ForgeHelper.INSTANCE.getClient();

            // Main thread task controller
            mainThreadTaskController = new MainTaskController(mc, this);
            mainThreadTaskController.addTask(new MappingMonitorTask());

            // Register general event handlers
            EventHandlerManager.registerGeneralHandlers();
            EventHandlerManager.registerGuiHandlers();

            // Resets detection results of Voxel/Rei's
            WaypointsData.enableRecheck();

            // Ensure all icons are ready for use.
            IconSetFileHandler.initialize();

            // Ensure all themese are ready for use
            ThemeFileHandler.initialize();

            // Webserver
            if (webMapProperties.enabled.get())
            {
                WebMap.getInstance().start();
            }
            initialized = true;

            VersionCheck.getVersionAvailable();

        }
        catch (Throwable t)
        {
            if (logger == null)
            {
                logger = LogManager.getLogger(Journeymap.MOD_ID);
            }
            logger.error(LogFormatter.toString(t));
        }
        finally
        {
            logger.info("postInitialize EXIT, " + (timer == null ? "" : timer.stopAndReport()));
        }

        JMLogger.setLevelFromProperties();
    }

    /**
     * Accept any modlist on server
     *
     * @param modList
     * @param side
     * @return
     */
    @Override
    public boolean checkModLists(Map<String, String> modList, Side side)
    {
        // TODO: Check for JMServer and enable/disable worldid checking, etc.
        return true;
    }

    @Override
    public boolean isUpdateCheckEnabled()
    {
        return getCoreProperties().checkUpdates.get();
    }

    /**
     * Whether the instance is initialized.
     *
     * @return
     */
    public Boolean isInitialized()
    {
        return initialized;
    }

    /**
     * Whether the client is mapping.
     *
     * @return
     */
    public Boolean isMapping()
    {
        return initialized && multithreadTaskController != null && multithreadTaskController.isMapping();
    }

    /**
     * Whether thread logging is enabled.
     *
     * @return
     */
    public Boolean isThreadLogging()
    {
        return threadLogging;
    }

    /**
     * Initialize the webserver
     *
     * @return
     */
    public WebMap getJmServer()
    {
        return WebMap.getInstance();
    }

    /**
     * Toggles automapping
     *
     * @param enable
     */
    public void toggleTask(Class<? extends ITaskManager> managerClass, boolean enable, Object params)
    {
        if (multithreadTaskController != null)
        {
            multithreadTaskController.toggleTask(managerClass, enable, params);
        }
    }

    /**
     * Checks whether a task manager is managerEnabled.
     *
     * @param managerClass
     * @return
     */
    public boolean isTaskManagerEnabled(Class<? extends ITaskManager> managerClass)
    {
        if (multithreadTaskController != null)
        {
            return multithreadTaskController.isTaskManagerEnabled(managerClass);
        }
        else
        {
            return false;
        }
    }

    /**
     * Starts mapping threads
     */
    public void startMapping()
    {
        synchronized (this)
        {
            if (mc == null || mc.theWorld == null || !initialized || !coreProperties.mappingEnabled.get())
            {
                return;
            }

            File worldDir = FileHandler.getJMWorldDir(mc, currentWorldId);
            if (!worldDir.exists())
            {
                boolean created = worldDir.mkdirs();
                if (!created)
                {
                    JMLogger.logOnce("CANNOT CREATE DATA DIRECTORY FOR WORLD: " + worldDir.getPath(), null);
                    return;
                }
            }

            this.reset();
            ColorManager.instance().ensureCurrent();

            multithreadTaskController = new TaskController();
            multithreadTaskController.enableTasks();

            long totalMB = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            long freeMB = Runtime.getRuntime().freeMemory() / 1024 / 1024;
            String memory = String.format("Memory: %sMB total, %sMB free", totalMB, freeMB);
            logger.info(String.format("Mapping started in %s%sDIM%s. %s ", FileHandler.getJMWorldDir(mc, currentWorldId),
                    File.separator,
                    ForgeHelper.INSTANCE.getDimension(mc.theWorld),
                    memory));
        }
    }

    /**
     * Halts mapping threads, clears caches.
     */
    public void stopMapping()
    {
        synchronized (this)
        {
            if ((isMapping()) && mc != null)
            {
                RegionImageCache.instance().flushToDisk(false);
                logger.info(String.format("Mapping halted in %s%sDIM%s", FileHandler.getJMWorldDir(mc, currentWorldId), File.separator, ForgeHelper.INSTANCE.getDimension(mc.theWorld)));
            }

            if (multithreadTaskController != null)
            {
                multithreadTaskController.disableTasks();
                multithreadTaskController.clear();
                multithreadTaskController = null;
            }
        }
    }

    /**
     * Reset state on everything in the client.
     */
    private void reset()
    {
        if (!mc.isSingleplayer() && currentWorldId == null)
        {
            WorldInfoHandler.requestWorldID();
        }

        loadConfigProperties();
        DataCache.instance().purge();
        chunkRenderController = new ChunkRenderController();
        Fullscreen.state().requireRefresh();
        Fullscreen.state().follow.set(true);
        StatTimer.resetAll();
        TileDrawStepCache.clear();
        UIManager.getInstance().getMiniMap().reset();
        UIManager.getInstance().reset();
        WaypointStore.instance().reset();
    }

    /**
     * Queue a task that has to be run on the main thread.
     *
     * @param task
     */
    public void queueMainThreadTask(IMainThreadTask task)
    {
        mainThreadTaskController.addTask(task);
    }

    /**
     * Perform tasks on the main thread.
     */
    public void performMainThreadTasks()
    {
        try
        {
            mainThreadTaskController.performTasks();
        }
        catch (Throwable t)
        {
            String error = "Error in JourneyMap.performMainThreadTasks(): " + t.getMessage();
            ChatLog.announceError(error);
            logger.error(LogFormatter.toString(t));
        }
    }

    /**
     * Perform tasks on other threads.
     */
    public void performMultithreadTasks()
    {
        try
        {
            if (isMapping())
            {
                multithreadTaskController.performTasks();
            }
        }
        catch (Throwable t)
        {
            String error = "Error in JourneyMap.performMultithreadTasks(): " + t.getMessage();
            ChatLog.announceError(error);
            logger.error(LogFormatter.toString(t));
        }
    }

    /**
     * Get the chunk render controller instance.  May be null.
     *
     * @return
     */
    public ChunkRenderController getChunkRenderController()
    {
        return chunkRenderController;
    }

    /**
     * Load all the properties from their files.
     */
    public void loadConfigProperties()
    {
        coreProperties = PropertiesBase.reload(coreProperties, CoreProperties.class);
        fullMapProperties = PropertiesBase.reload(fullMapProperties, FullMapProperties.class);
        miniMapProperties1 = PropertiesBase.reload(miniMapProperties1, MiniMapProperties.class);
        miniMapProperties2 = PropertiesBase.reload(miniMapProperties2, MiniMapProperties2.class);
        webMapProperties = PropertiesBase.reload(webMapProperties, WebMapProperties.class);
        waypointProperties = PropertiesBase.reload(waypointProperties, WaypointProperties.class);
    }

    /**
     * Handling of the worldIdPacket message.
     *
     * @param worldId
     * @param playerEntity
     */
    @Override
    public void handleWorldIdMessage(String worldId, EntityPlayerMP playerEntity)
    {
        setCurrentWorldId(worldId);
    }

    /**
     * Get the current world id.  May be null.
     *
     * @return
     */
    public String getCurrentWorldId()
    {
        return this.currentWorldId;
    }

    /**
     * Set the current world id.
     *
     * @param worldId
     */
    public void setCurrentWorldId(String worldId)
    {
        synchronized (this)
        {
            File currentWorldDirectory = FileHandler.getJMWorldDirForWorldId(mc, currentWorldId);
            File newWorldDirectory = FileHandler.getJMWorldDir(mc, worldId);

            boolean worldIdUnchanged = Constants.safeEqual(worldId, currentWorldId);
            boolean directoryUnchanged = currentWorldDirectory != null && newWorldDirectory != null && currentWorldDirectory.getPath().equals(newWorldDirectory.getPath());

            if (worldIdUnchanged && directoryUnchanged && worldId != null)
            {
                Journeymap.getLogger().info("World UID hasn't changed: " + worldId);
                return;
            }

            boolean wasMapping = isMapping();
            if (wasMapping)
            {
                stopMapping();
            }

            this.currentWorldId = worldId;
            Journeymap.getLogger().info("World UID is set to: " + worldId);
        }
    }
}
