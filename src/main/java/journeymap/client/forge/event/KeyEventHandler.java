/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.event;

// 1.7.10

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import journeymap.client.Constants;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.ChatLog;
import journeymap.client.model.Waypoint;
import journeymap.client.render.map.Tile;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

// 1.8
//import net.minecraftforge.fml.client.registry.ClientRegistry;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.fml.common.gameevent.InputEvent;

/**
 * Handles keyboard events in-game.  (Not in UIs).
 */
public class KeyEventHandler implements EventHandlerManager.EventHandler
{

    public KeyEventHandler()
    {
    }

    public static void initKeyBindings()
    {
        Minecraft minecraft = Minecraft.getMinecraft();
        HashSet<String> keyDescs = new HashSet<String>();
        Map<String, KeyBinding> existingBindings = new HashMap<String, KeyBinding>();
        for (KeyBinding existing : minecraft.gameSettings.keyBindings)
        {
            try
            {
                if (existing != null && existing.getKeyDescription() != null)
                {
                    keyDescs.add(existing.getKeyDescription());
                    existingBindings.put(existing.getKeyDescription(), existing);
                }
            }
            catch (Throwable t)
            {
                Journeymap.getLogger().error("Unexpected error when checking existing keybinding : {}", existing);
            }
        }

        for (KeyBinding kb : Constants.initKeybindings())
        {
            try
            {
                if (!keyDescs.contains(kb.getKeyDescription()))
                {
                    ClientRegistry.registerKeyBinding(kb);
                }
                else
                {
                    bindExistingKeyBinding(existingBindings.get(kb.getKeyDescription()));
                    Journeymap.getLogger().warn("Avoided duplicate keybinding that was already registered: {}", kb.getKeyDescription());
                }
            }
            catch (Throwable t)
            {
                ChatLog.announceError("Unexpected error when registering keybinding : " + kb);
            }
        }
    }

    private static void bindExistingKeyBinding(KeyBinding existing)
    {
        if (existing == null)
        {
            return;
        }

        String description = existing.getKeyDescription();
        if ("key.journeymap.map_toggle".equals(description))
        {
            Constants.KB_MAP = existing;
        }
        else if ("key.journeymap.zoom_in".equals(description))
        {
            Constants.KB_MAP_ZOOMIN = existing;
        }
        else if ("key.journeymap.zoom_out".equals(description))
        {
            Constants.KB_MAP_ZOOMOUT = existing;
        }
        else if ("key.journeymap.minimap_type".equals(description))
        {
            Constants.KB_MAP_SWITCH_TYPE = existing;
        }
        else if ("key.journeymap.minimap_preset".equals(description))
        {
            Constants.KB_MINIMAP_PRESET = existing;
        }
        else if ("key.journeymap.create_waypoint".equals(description))
        {
            Constants.KB_WAYPOINT = existing;
        }
        else if ("key.journeymap.fullscreen_teleport".equals(description))
        {
            Constants.KB_FULLSCREEN_TELEPORT = existing;
        }
        else if ("key.journeymap.fullscreen_toggle_waypoint".equals(description))
        {
            Constants.KB_FULLSCREEN_TOGGLE_WAYPOINT = existing;
        }
        else if ("key.journeymap.fullscreen_delete_waypoint".equals(description))
        {
            Constants.KB_FULLSCREEN_DELETE_WAYPOINT = existing;
        }
        else if ("key.journeymap.fullscreen_edit_waypoint".equals(description))
        {
            Constants.KB_FULLSCREEN_EDIT_WAYPOINT = existing;
        }
    }

    public static boolean onKeypress(boolean minimapOnly)
    {
        Minecraft minecraft = Minecraft.getMinecraft();
        final int i = Keyboard.getEventKey();

        try
        {
            // This seems to prevent the keycode from "staying"
            boolean controlDown = Keyboard.isKeyDown(29) || Keyboard.isKeyDown(157);
            //GuiScreen.isCtrlKeyDown();

            if (controlDown && Constants.isPressed(Constants.KB_MAP))
            {
                UIManager.getInstance().toggleMinimap();
                return true;
            }
            else if (controlDown && Constants.isPressed(Constants.KB_MAP_ZOOMIN))
            {
                Tile.switchTileRenderType();
                return true;
            }
            else if (controlDown && Constants.isPressed(Constants.KB_MAP_ZOOMOUT))
            {
                Tile.switchTileDisplayQuality();
                return true;
            }
            else if (Constants.isPressed(Constants.KB_MAP_ZOOMIN))
            {
                MiniMap.state().zoomIn();
                return true;
            }
            else if (Constants.isPressed(Constants.KB_MAP_ZOOMOUT))
            {
                MiniMap.state().zoomOut();
                return true;
            }
            else if (Constants.isPressed(Constants.KB_MAP_SWITCH_TYPE))
            {
                MiniMap.state().toggleMapType();
                KeyBinding.unPressAllKeys();
                return true;
            }
            else if (Constants.isPressed(Constants.KB_MINIMAP_PRESET))
            {
                UIManager.getInstance().switchMiniMapPreset();
                return true;
            }
            else if (controlDown && Constants.isPressed(Constants.KB_WAYPOINT))
            {
                KeyBinding.unPressAllKeys();
                UIManager.getInstance().openWaypointManager(null, null);
                return true;
            }

            if (!minimapOnly)
            {
                if (Constants.KB_MAP.isPressed())
                {
                    if (ForgeHelper.INSTANCE.getClient().currentScreen == null)
                    {
                        UIManager.getInstance().openFullscreenMap();
                    }
                    else
                    {
                        if (ForgeHelper.INSTANCE.getClient().currentScreen instanceof Fullscreen)
                        {
                            UIManager.getInstance().closeAll();
                        }
                    }
                    return true;
                }
                else
                {
                    if (Constants.KB_WAYPOINT.isPressed())
                    {
                        if (ForgeHelper.INSTANCE.getClient().currentScreen == null)
                        {
                            Minecraft mc = ForgeHelper.INSTANCE.getClient();
                            Waypoint waypoint = Waypoint.of(mc.thePlayer);
                            UIManager.getInstance().openWaypointEditor(waypoint, true, null);
                        }
                        return true;
                    }
                }
            }
        }
        finally
        {

        }
        return false;
    }

    @Override
    public EnumSet<EventHandlerManager.BusType> getBus()
    {
        return EnumSet.of(EventHandlerManager.BusType.FMLCommonHandlerBus);
    }

    @SubscribeEvent()
    public void onKeyboardEvent(InputEvent.KeyInputEvent event)
    {
        KeyEventHandler.onKeypress(false);
    }
}

