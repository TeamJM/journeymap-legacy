package journeymap.client.ui.fullscreen.context;

import journeymap.client.Constants;
import journeymap.client.model.Waypoint;
import journeymap.client.model.WaypointDisplayNameFormatter;

import java.util.ArrayList;
import java.util.List;

public class FullscreenContextMenuModel
{
    private final WaypointDisplayNameFormatter displayNameFormatter = new WaypointDisplayNameFormatter();

    public List<FullscreenContextMenuItem> build(FullscreenContextTarget target)
    {
        if (target instanceof WaypointContextTarget)
        {
            return buildWaypointMenu((WaypointContextTarget) target);
        }
        return buildLocationMenu((MapLocationContextTarget) target);
    }

    public List<FullscreenContextMenuItem> buildLocationMenu(MapLocationContextTarget target)
    {
        List<FullscreenContextMenuItem> items = new ArrayList<FullscreenContextMenuItem>();
        items.add(new FullscreenContextMenuItem("chunk_coordinates",
                Constants.getString("jm.fullscreen.context.chunk_coordinates", target.getChunkX(), target.getChunkZ()),
                null,
                false,
                false,
                null,
                100));
        items.add(new FullscreenContextMenuItem("display_coordinates",
                formatCoordinates(target.getX(), target.getDisplayY(), target.getZ()),
                null,
                false,
                false,
                null,
                200));
        items.add(new FullscreenContextMenuItem("copy_coordinates", Constants.getString("jm.fullscreen.context.copy_coordinates"), null, true, true, null, 300));
        items.add(new FullscreenContextMenuItem("create_waypoint",
                Constants.getString("jm.fullscreen.context.create_waypoint"),
                null,
                true,
                true,
                null,
                400));
        items.add(new FullscreenContextMenuItem("create_temporary_waypoint", Constants.getString("jm.fullscreen.context.create_temporary_waypoint"), null, true, true, null, 500));
        items.add(new FullscreenContextMenuItem("teleport_here", Constants.getString("jm.fullscreen.context.teleport_here"), null, true, true, null, 600));
        items.add(new FullscreenContextMenuItem("open_waypoint_manager", Constants.getString("jm.fullscreen.context.open_waypoint_manager"), null, true, true, null, 800));
        items.add(new FullscreenContextMenuItem("open_settings", Constants.getString("jm.fullscreen.context.open_settings"), null, true, true, null, 900));
        return items;
    }

    public List<FullscreenContextMenuItem> buildWaypointMenu(WaypointContextTarget target)
    {
        Waypoint waypoint = target.getWaypoint();
        List<FullscreenContextMenuItem> items = new ArrayList<FullscreenContextMenuItem>();
        items.add(new FullscreenContextMenuItem("edit_waypoint_primary",
                displayNameFormatter.formatLabel(waypoint),
                null,
                true,
                true,
                waypoint.getColor(),
                100));
        items.add(new FullscreenContextMenuItem("display_coordinates",
                formatCoordinates(waypoint.getX(), waypoint.getY(), waypoint.getZ()),
                null,
                false,
                false,
                null,
                200));
        items.add(new FullscreenContextMenuItem("edit_waypoint", Constants.getString("jm.waypoint.edit"), null, true, true, null, 300));
        items.add(new FullscreenContextMenuItem("copy_coordinates", Constants.getString("jm.fullscreen.context.copy_coordinates"), null, true, true, null, 400));
        items.add(new FullscreenContextMenuItem("create_waypoint",
                Constants.getString("jm.fullscreen.context.create_waypoint"),
                null,
                true,
                true,
                null,
                500));
        items.add(new FullscreenContextMenuItem(waypoint.isTemporary() ? "restore_waypoint" : "toggle_waypoint_visibility",
                waypoint.isTemporary() ? Constants.getString("jm.waypoint.restore_waypoint") : Constants.getString("jm.waypoint.close_waypoint"),
                null,
                true,
                true,
                null,
                700));
        items.add(new FullscreenContextMenuItem("delete_waypoint", Constants.getString("jm.fullscreen.context.delete_waypoint"), null, true, true, null, 800));
        return items;
    }

    private String formatCoordinates(int x, Integer y, int z)
    {
        if (y == null)
        {
            return Constants.getString("jm.fullscreen.context.coordinates_xz", x, z);
        }
        return Constants.getString("jm.fullscreen.context.coordinates_xyz", x, y, z);
    }
}
