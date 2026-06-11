package journeymap.client.model;

import journeymap.client.Constants;
import net.minecraft.util.EnumChatFormatting;

public class WaypointDisplayNameFormatter
{
    public String formatLabel(Waypoint waypoint)
    {
        String label = waypoint.getName();
        if (waypoint.isTemporary())
        {
            return label + " " + EnumChatFormatting.RED + Constants.getString("jm.waypoint.temporary_suffix");
        }
        if (waypoint.isDestination())
        {
            return label + " " + EnumChatFormatting.GOLD + Constants.getString("jm.waypoint.destination_suffix");
        }
        return label;
    }

    public String formatManagerLabel(Waypoint waypoint, boolean waypointVisible)
    {
        String label = formatLabel(waypoint);
        if (!waypointVisible)
        {
            return EnumChatFormatting.STRIKETHROUGH + label;
        }
        return label;
    }
}
