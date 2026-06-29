/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.fullscreen.context;

import journeymap.client.Constants;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuContext;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuEntry;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuProvider;
import journeymap.client.command.CmdTeleportWaypoint;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.Waypoint;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.JmUI;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * Supplies JourneyMap's built-in rows through the same provider path used by integrations.
 */
public class JourneyMapFullscreenContextMenuProvider implements FullscreenContextMenuProvider
{
    private static final String EDIT_WAYPOINT_PRIMARY = "journeymap:edit_waypoint_primary";
    private static final String EDIT_WAYPOINT = "journeymap:edit_waypoint";
    private static final String COPY_COORDINATES = "journeymap:copy_coordinates";
    private static final String CREATE_WAYPOINT = "journeymap:create_waypoint";
    private static final String TELEPORT_HERE = "journeymap:teleport_here";
    private static final String OPEN_WAYPOINT_MANAGER = "journeymap:open_waypoint_manager";
    private static final String OPEN_SETTINGS = "journeymap:open_settings";
    private static final String TOGGLE_WAYPOINT = "journeymap:toggle_waypoint";
    private static final String DELETE_WAYPOINT = "journeymap:delete_waypoint";

    private final JmUI returnDisplay;

    public JourneyMapFullscreenContextMenuProvider(JmUI returnDisplay)
    {
        this.returnDisplay = returnDisplay;
    }

    @Override
    public List<FullscreenContextMenuEntry> getMenuItems(FullscreenContextMenuContext context)
    {
        if (context.hasWaypoint())
        {
            return getWaypointItems(context);
        }
        return getLocationItems(context);
    }

    @Override
    public boolean onMenuItemClicked(FullscreenContextMenuContext context, String actionId)
    {
        if (COPY_COORDINATES.equals(actionId))
        {
            copyCoordinates(context);
        }
        else if (CREATE_WAYPOINT.equals(actionId))
        {
            UIManager.getInstance().openWaypointEditor(createWaypoint(context), true, returnDisplay);
        }
        else if (TELEPORT_HERE.equals(actionId))
        {
            teleportTo(context);
        }
        else if (OPEN_WAYPOINT_MANAGER.equals(actionId))
        {
            UIManager.getInstance().openWaypointManager(null, returnDisplay);
        }
        else if (OPEN_SETTINGS.equals(actionId))
        {
            UIManager.getInstance().openOptionsManager();
        }
        else if (EDIT_WAYPOINT.equals(actionId) || EDIT_WAYPOINT_PRIMARY.equals(actionId))
        {
            UIManager.getInstance().openWaypointEditor(context.getWaypoint(), false, returnDisplay);
        }
        else if (TOGGLE_WAYPOINT.equals(actionId))
        {
            toggleWaypoint(context.getWaypoint());
        }
        else if (DELETE_WAYPOINT.equals(actionId))
        {
            deleteWaypoint(context.getWaypoint());
        }
        else
        {
            return false;
        }
        return true;
    }

    public boolean keyTyped(FullscreenContextMenuContext context, int keyCode)
    {
        boolean controlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (!controlDown && matchesKey(Constants.KB_WAYPOINT, keyCode))
        {
            return onMenuItemClicked(context, CREATE_WAYPOINT);
        }
        if (controlDown && matchesKey(Constants.KB_WAYPOINT, keyCode))
        {
            return onMenuItemClicked(context, OPEN_WAYPOINT_MANAGER);
        }
        if (keyCode == Keyboard.KEY_O)
        {
            return onMenuItemClicked(context, OPEN_SETTINGS);
        }
        if (context.hasWaypoint() && matchesKey(Constants.KB_FULLSCREEN_EDIT_WAYPOINT, keyCode))
        {
            return onMenuItemClicked(context, EDIT_WAYPOINT);
        }
        if (context.hasWaypoint() && matchesKey(Constants.KB_FULLSCREEN_TOGGLE_WAYPOINT, keyCode))
        {
            return onMenuItemClicked(context, TOGGLE_WAYPOINT);
        }
        if (context.hasWaypoint() && matchesKey(Constants.KB_FULLSCREEN_DELETE_WAYPOINT, keyCode))
        {
            return onMenuItemClicked(context, DELETE_WAYPOINT);
        }
        if (!context.hasWaypoint() && matchesKey(Constants.KB_FULLSCREEN_TELEPORT, keyCode))
        {
            return onMenuItemClicked(context, TELEPORT_HERE);
        }
        return false;
    }

    private boolean matchesKey(KeyBinding keyBinding, int keyCode)
    {
        return keyBinding != null && keyBinding.getKeyCode() != 0 && keyCode == keyBinding.getKeyCode();
    }

    private List<FullscreenContextMenuEntry> getLocationItems(FullscreenContextMenuContext context)
    {
        List<FullscreenContextMenuEntry> items = new ArrayList<FullscreenContextMenuEntry>();
        items.add(FullscreenContextMenuEntry.info(Constants.getString("jm.fullscreen.context.chunk_coordinates",
                context.getChunkX(), context.getChunkZ())).withOrder(100));
        items.add(FullscreenContextMenuEntry.info(formatCoordinates(context.getX(), context.getDisplayY(), context.getZ())).withOrder(200));
        items.add(FullscreenContextMenuEntry.action(COPY_COORDINATES, Constants.getString("jm.fullscreen.context.copy_coordinates")).withOrder(300));
        items.add(FullscreenContextMenuEntry.action(CREATE_WAYPOINT, Constants.getString("jm.fullscreen.context.create_waypoint"))
                .withShortcut(Constants.getKeyName(Constants.KB_WAYPOINT)).withOrder(400));
        items.add(FullscreenContextMenuEntry.action(TELEPORT_HERE, Constants.getString("jm.fullscreen.context.teleport_here"))
                .withShortcut(Constants.getKeyName(Constants.KB_FULLSCREEN_TELEPORT)).withOrder(600));
        items.add(FullscreenContextMenuEntry.action(OPEN_WAYPOINT_MANAGER, Constants.getString("jm.fullscreen.context.open_waypoint_manager"))
                .withShortcut("Ctrl+" + Constants.getKeyName(Constants.KB_WAYPOINT)).withOrder(800));
        items.add(FullscreenContextMenuEntry.action(OPEN_SETTINGS, Constants.getString("jm.fullscreen.context.open_settings"))
                .withShortcut(Keyboard.getKeyName(Keyboard.KEY_O)).withOrder(900));
        return items;
    }

    private List<FullscreenContextMenuEntry> getWaypointItems(FullscreenContextMenuContext context)
    {
        Waypoint waypoint = context.getWaypoint();
        List<FullscreenContextMenuEntry> items = new ArrayList<FullscreenContextMenuEntry>();
        items.add(FullscreenContextMenuEntry.action(EDIT_WAYPOINT_PRIMARY, waypoint.getName())
                .withBackgroundColor(waypoint.getColor()).withOrder(100));
        items.add(FullscreenContextMenuEntry.info(formatCoordinates(waypoint.getX(), waypoint.getY(), waypoint.getZ())).withOrder(200));
        items.add(FullscreenContextMenuEntry.action(EDIT_WAYPOINT, Constants.getString("jm.waypoint.edit"))
                .withShortcut(Constants.getKeyName(Constants.KB_FULLSCREEN_EDIT_WAYPOINT)).withOrder(300));
        items.add(FullscreenContextMenuEntry.action(COPY_COORDINATES, Constants.getString("jm.fullscreen.context.copy_coordinates")).withOrder(400));
        items.add(FullscreenContextMenuEntry.action(CREATE_WAYPOINT, Constants.getString("jm.fullscreen.context.create_waypoint"))
                .withShortcut(Constants.getKeyName(Constants.KB_WAYPOINT)).withOrder(500));
        items.add(FullscreenContextMenuEntry.action(TOGGLE_WAYPOINT, getToggleLabel(waypoint))
                .withShortcut(Constants.getKeyName(Constants.KB_FULLSCREEN_TOGGLE_WAYPOINT)).withOrder(700));
        items.add(FullscreenContextMenuEntry.action(DELETE_WAYPOINT, Constants.getString("jm.fullscreen.context.delete_waypoint"))
                .withShortcut(Constants.getKeyName(Constants.KB_FULLSCREEN_DELETE_WAYPOINT)).withOrder(800));
        return items;
    }

    private String getToggleLabel(Waypoint waypoint)
    {
        if (waypoint.isEnable())
        {
            return Constants.getString("jm.fullscreen.context.disable_waypoint");
        }
        return Constants.getString("jm.fullscreen.context.enable_waypoint");
    }

    private String formatCoordinates(int x, Integer y, int z)
    {
        if (y == null)
        {
            return Constants.getString("jm.fullscreen.context.coordinates_xz", x, z);
        }
        return Constants.getString("jm.fullscreen.context.coordinates_xyz", x, y, z);
    }

    private void copyCoordinates(FullscreenContextMenuContext context)
    {
        StringSelection selection = new StringSelection(getX(context) + " " + getY(context) + " " + getZ(context));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    private Waypoint createWaypoint(FullscreenContextMenuContext context)
    {
        return Waypoint.at(getX(context), getY(context), getZ(context), Waypoint.Type.Normal, context.getDimension());
    }

    private int getX(FullscreenContextMenuContext context)
    {
        return context.hasWaypoint() ? context.getWaypoint().getX() : context.getX();
    }

    private int getY(FullscreenContextMenuContext context)
    {
        return context.hasWaypoint() ? context.getWaypoint().getY() : context.getResolvedY();
    }

    private int getZ(FullscreenContextMenuContext context)
    {
        return context.hasWaypoint() ? context.getWaypoint().getZ() : context.getZ();
    }

    private void teleportTo(FullscreenContextMenuContext context)
    {
        Minecraft mc = ForgeHelper.INSTANCE.getClient();
        if (CmdTeleportWaypoint.isPermitted(mc))
        {
            new CmdTeleportWaypoint(createWaypoint(context)).run();
            UIManager.getInstance().closeAll();
        }
    }

    private void toggleWaypoint(Waypoint waypoint)
    {
        waypoint.setEnable(!waypoint.isEnable());
        WaypointStore.instance().save(waypoint);
        refreshMapState();
    }

    private void deleteWaypoint(Waypoint waypoint)
    {
        WaypointStore.instance().remove(waypoint);
        refreshMapState();
    }

    private void refreshMapState()
    {
        MiniMap.state().requireRefresh();
        Fullscreen.state().requireRefresh();
    }
}
