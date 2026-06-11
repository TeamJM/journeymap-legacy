package journeymap.client.ui.fullscreen.context;

import journeymap.client.command.CmdTeleportWaypoint;
import journeymap.client.model.Waypoint;
import journeymap.client.model.WaypointLifecycle;
import journeymap.client.model.WaypointVisibility;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.JmUI;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.waypoint.WaypointStore;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class FullscreenContextActionService
{
    public void copyCoordinates(FullscreenContextTarget target)
    {
        StringSelection selection = new StringSelection(target.getX() + " " + target.getResolvedY() + " " + target.getZ());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    public Waypoint createWaypoint(MapLocationContextTarget target, WaypointLifecycle lifecycle)
    {
        Waypoint waypoint = Waypoint.at(target.getX(), target.getResolvedY(), target.getZ(), Waypoint.Type.Normal, target.getDimension());
        waypoint.setLifecycle(lifecycle);
        return waypoint;
    }

    public void openWaypointEditor(MapLocationContextTarget target, WaypointLifecycle lifecycle, JmUI returnDisplay)
    {
        UIManager.getInstance().openWaypointEditor(createWaypoint(target, lifecycle), true, returnDisplay);
    }

    public Waypoint quickCreateWaypoint(MapLocationContextTarget target, WaypointLifecycle lifecycle)
    {
        Waypoint waypoint = createWaypoint(target, lifecycle);
        WaypointStore.instance().save(waypoint);
        refreshMapState();
        return waypoint;
    }

    public void openWaypointEditor(Waypoint waypoint, JmUI returnDisplay)
    {
        UIManager.getInstance().openWaypointEditor(waypoint, false, returnDisplay);
    }

    public void teleportTo(FullscreenContextTarget target)
    {
        new CmdTeleportWaypoint(asTeleportWaypoint(target)).run();
    }

    public void toggleWaypointVisibility(Waypoint waypoint)
    {
        waypoint.setVisibility(waypoint.getVisibility() == WaypointVisibility.ENABLED ? WaypointVisibility.DISABLED : WaypointVisibility.ENABLED);
        WaypointStore.instance().save(waypoint);
        refreshMapState();
    }

    public void restoreWaypoint(Waypoint waypoint)
    {
        waypoint.setVisibility(WaypointVisibility.ENABLED);
        waypoint.setLifecycle(WaypointLifecycle.PERSISTENT);
        WaypointStore.instance().save(waypoint);
        refreshMapState();
    }

    public void deleteWaypoint(Waypoint waypoint)
    {
        WaypointStore.instance().remove(waypoint);
        refreshMapState();
    }

    private Waypoint asTeleportWaypoint(FullscreenContextTarget target)
    {
        if (target instanceof WaypointContextTarget)
        {
            return ((WaypointContextTarget) target).getWaypoint();
        }

        MapLocationContextTarget locationTarget = (MapLocationContextTarget) target;
        return createWaypoint(locationTarget, WaypointLifecycle.PERSISTENT);
    }

    private void refreshMapState()
    {
        MiniMap.state().requireRefresh();
        Fullscreen.state().requireRefresh();
    }
}
