package journeymap.client.waypoint;

import journeymap.client.Constants;
import journeymap.client.model.Waypoint;

public class WaypointChatFormatter
{
    public String formatClipboard(int x, int y, int z)
    {
        return String.format("%s %s %s", x, y, z);
    }

    public String formatLocationMessage(int x, Integer y, int z)
    {
        if (y == null)
        {
            return Constants.getString("jm.fullscreen.context.coordinates_xz", x, z);
        }
        return Constants.getString("jm.fullscreen.context.coordinates_xyz", x, y, z);
    }

    public String formatWaypointMessage(Waypoint waypoint)
    {
        return formatLocationMessage(waypoint.getX(), waypoint.getY(), waypoint.getZ());
    }
}
