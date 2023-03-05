/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui;

import journeymap.client.JourneymapClient;
import journeymap.client.data.WaypointsData;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.Waypoint;
import journeymap.client.properties.config.Config;
import journeymap.client.ui.component.JmUI;
import journeymap.client.ui.dialog.*;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.minimap.MiniMapHotkeysHelp;
import journeymap.client.ui.waypoint.WaypointEditor;
import journeymap.client.ui.waypoint.WaypointManager;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class UIManager
{
    private final Logger logger = Journeymap.getLogger();
    private final MiniMap miniMap;
    Minecraft minecraft = ForgeHelper.INSTANCE.getClient();

    private UIManager()
    {
        int preset = JourneymapClient.getMiniMapProperties1().isActive() ? 1 : 2;
        miniMap = new MiniMap(JourneymapClient.getMiniMapProperties(preset));
    }

    public static UIManager getInstance()
    {
        return Holder.INSTANCE;
    }

    public void closeAll()
    {
        closeCurrent();
        minecraft.displayGuiScreen(null);
        minecraft.setIngameFocus();
    }

    public void closeCurrent()
    {
        if (minecraft.currentScreen != null && minecraft.currentScreen instanceof JmUI)
        {
            logger.debug("Closing " + minecraft.currentScreen.getClass());
            ((JmUI) minecraft.currentScreen).close();
        }
        KeyBinding.unPressAllKeys();
    }

    public void openInventory()
    {
        logger.debug("Opening inventory");
        closeAll();
        minecraft.displayGuiScreen(new GuiInventory(minecraft.thePlayer)); // displayGuiScreen
    }

    public <T extends JmUI> T open(Class<T> uiClass, JmUI returnDisplay)
    {
        try
        {
            // Try constructor with return display
            return open(uiClass.getConstructor(JmUI.class).newInstance(returnDisplay));
        }
        catch (Throwable e)
        {
            try
            {
                // Try constructor without return display
                return open(uiClass.getConstructor().newInstance());
            }
            catch (Throwable e2)
            {
                logger.log(Level.ERROR, "1st unexpected exception creating UI: " + LogFormatter.toString(e));
                logger.log(Level.ERROR, "2nd unexpected exception creating UI: " + LogFormatter.toString(e2));
                closeCurrent();
                return null;
            }
        }
    }

    public <T extends JmUI> T open(Class<T> uiClass)
    {
        try
        {
            T ui = uiClass.newInstance();
            return open(ui);
        }
        catch (Throwable e)
        {
            logger.log(Level.ERROR, "Unexpected exception creating UI: " + LogFormatter.toString(e)); //$NON-NLS-1$
            closeCurrent();
            return null;
        }
    }

    public <T extends JmUI> T open(T ui)
    {
        closeCurrent();
        logger.debug("Opening UI " + ui.getClass().getSimpleName());
        try
        {
            minecraft.displayGuiScreen(ui);
            //miniMap.setVisible(false);
        }
        catch (Throwable t)
        {
            logger.error(String.format("Unexpected exception opening UI %s: %s", ui.getClass(), LogFormatter.toString(t)));
        }
        return ui;
    }

    public void toggleMinimap()
    {
        setMiniMapEnabled(!isMiniMapEnabled());
    }

    public boolean isMiniMapEnabled()
    {
        return miniMap.getCurrentMinimapProperties().enabled.get();
    }

    public void setMiniMapEnabled(boolean enable)
    {
        miniMap.getCurrentMinimapProperties().enabled.set(enable);
        miniMap.getCurrentMinimapProperties().save();
    }

    public void drawMiniMap()
    {
        try
        {
            if (miniMap.getCurrentMinimapProperties().enabled.get())
            {
                final GuiScreen currentScreen = minecraft.currentScreen;
                final boolean doDraw = currentScreen == null || currentScreen instanceof GuiChat;
                if (doDraw)
                {
                    miniMap.drawMap();
                }
            }
        }
        catch (Throwable e)
        {
            Journeymap.getLogger().error("Error drawing minimap: " + LogFormatter.toString(e));
        }
    }

    public MiniMap getMiniMap()
    {
        return miniMap;
    }

    public void openFullscreenMap()
    {
        KeyBinding.unPressAllKeys();
        open(Fullscreen.class);
    }

    public void openFullscreenMap(Waypoint waypoint)
    {
        try
        {
            if (waypoint.isInPlayerDimension())
            {
                KeyBinding.unPressAllKeys();
                Fullscreen map = open(Fullscreen.class);
                map.centerOn(waypoint);
            }
        }
        catch (Throwable e)
        {
            Journeymap.getLogger().error("Error opening map on waypoint: " + LogFormatter.toString(e));
        }
    }

    public void openMapHotkeyHelp(JmUI returnDisplay)
    {
        open(FullscreenHotkeysHelp.class, returnDisplay);
    }

    public void openMiniMapHotkeyHelp(JmUI returnDisplay)
    {
        open(MiniMapHotkeysHelp.class, returnDisplay);
    }

    public void openOptionsManager()
    {
        open(OptionsManager.class);
    }

    public void openOptionsManager(JmUI returnDisplay, Config.Category... initialCategories)
    {
        try
        {
            open(new OptionsManager(returnDisplay, initialCategories));
        }
        catch (Throwable e)
        {
            logger.log(Level.ERROR, "Unexpected exception creating MasterOptions with return class: " + LogFormatter.toString(e));
        }
    }

    public void openMapActions()
    {
        open(FullscreenActions.class);
    }

    public void openSplash(JmUI returnDisplay)
    {
        open(Splash.class, returnDisplay);
    }

    public void openWaypointManager(Waypoint waypoint, JmUI returnDisplay)
    {
        if (WaypointsData.isManagerEnabled())
        {
            try
            {
                WaypointManager manager = new WaypointManager(waypoint, returnDisplay);
                open(manager);
            }
            catch (Throwable e)
            {
                Journeymap.getLogger().error("Error opening waypoint manager: " + LogFormatter.toString(e));
            }
        }
    }

    public void openWaypointEditor(Waypoint waypoint, boolean isNew, JmUI returnDisplay)
    {
        if (WaypointsData.isManagerEnabled())
        {
            try
            {
                WaypointEditor editor = new WaypointEditor(waypoint, isNew, returnDisplay);
                open(editor);
            }
            catch (Throwable e)
            {
                Journeymap.getLogger().error("Error opening waypoint editor: " + LogFormatter.toString(e));
            }
        }
    }

    public void openGridEditor(JmUI returnDisplay)
    {
        try
        {
            GridEditor editor = new GridEditor(returnDisplay);
            open(editor);
        }
        catch (Throwable e)
        {
            Journeymap.getLogger().error("Error opening grid editor: " + LogFormatter.toString(e));
        }
    }

    public void reset()
    {
        Fullscreen.state().requireRefresh();
        miniMap.reset();
    }

    public void switchMiniMapPreset()
    {
        int currentPreset = miniMap.getCurrentMinimapProperties().getId();
        switchMiniMapPreset(currentPreset == 1 ? 2 : 1);
    }

    public void switchMiniMapPreset(int which)
    {
        miniMap.setMiniMapProperties(JourneymapClient.getMiniMapProperties(which));
    }

    private static class Holder
    {
        private static final UIManager INSTANCE = new UIManager();
    }
}
