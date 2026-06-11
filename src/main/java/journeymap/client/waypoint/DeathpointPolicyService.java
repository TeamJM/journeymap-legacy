package journeymap.client.waypoint;

import journeymap.client.model.Waypoint;

public class DeathpointPolicyService
{
    public void removePreviousDeathpoints(Waypoint latestDeathpoint)
    {
        for (Waypoint waypoint : WaypointStore.instance().snapshot())
        {
            if (waypoint.isDeathPoint() && !waypoint.getId().equals(latestDeathpoint.getId()))
            {
                WaypointStore.instance().remove(waypoint);
            }
        }
    }
}
