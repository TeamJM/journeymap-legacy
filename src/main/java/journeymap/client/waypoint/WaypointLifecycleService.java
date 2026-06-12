package journeymap.client.waypoint;

import journeymap.client.JourneymapClient;
import journeymap.client.model.Waypoint;
import journeymap.client.properties.WaypointProperties;
import net.minecraft.entity.player.EntityPlayer;

public class WaypointLifecycleService
{
    private final WaypointArrivalEvaluator arrivalEvaluator = new WaypointArrivalEvaluator();

    public void removeTemporaryWaypoints()
    {
        for (Waypoint waypoint : WaypointStore.instance().snapshot())
        {
            if (waypoint.isTemporary())
            {
                WaypointStore.instance().remove(waypoint);
            }
        }
    }

    public void removeArrivedWaypoints(EntityPlayer player)
    {
        if (player == null)
        {
            return;
        }

        WaypointProperties waypointProperties = JourneymapClient.getWaypointProperties();
        int horizontalRange = waypointProperties.arrivalHorizontalRange.get();
        int verticalRange = waypointProperties.arrivalVerticalRange.get();
        for (Waypoint waypoint : WaypointStore.instance().snapshot())
        {
            if (shouldDeleteOnArrival(waypoint, waypointProperties)
                    && arrivalEvaluator.hasArrived(player, waypoint, horizontalRange, verticalRange))
            {
                WaypointStore.instance().remove(waypoint);
            }
        }
    }

    private boolean shouldDeleteOnArrival(Waypoint waypoint, WaypointProperties waypointProperties)
    {
        if (waypoint.isDestination())
        {
            return true;
        }

        return waypoint.isDeathPoint() && waypointProperties.deleteDeathpointOnArrival.get();
    }
}
