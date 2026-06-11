package journeymap.client.ui.fullscreen.context;

import journeymap.client.model.Waypoint;

public class WaypointContextTarget implements FullscreenContextTarget
{
    private final Waypoint waypoint;

    public WaypointContextTarget(Waypoint waypoint)
    {
        this.waypoint = waypoint;
    }

    public Waypoint getWaypoint()
    {
        return waypoint;
    }

    @Override
    public int getX()
    {
        return waypoint.getX();
    }

    @Override
    public int getResolvedY()
    {
        return waypoint.getY();
    }

    @Override
    public Integer getDisplayY()
    {
        return waypoint.getY();
    }

    @Override
    public int getZ()
    {
        return waypoint.getZ();
    }
}
