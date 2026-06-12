package journeymap.client.waypoint;

import journeymap.client.model.Waypoint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

public class WaypointArrivalEvaluator
{
    public boolean hasArrived(EntityPlayer player, Waypoint waypoint, int horizontalRange, int verticalRange)
    {
        if (player == null || waypoint == null)
        {
            return false;
        }

        if (!waypoint.getDimensions().contains(player.dimension))
        {
            return false;
        }

        if (waypoint.getY() < 0)
        {
            return false;
        }

        int playerX = MathHelper.floor_double(player.posX);
        int playerY = MathHelper.floor_double(player.posY);
        int playerZ = MathHelper.floor_double(player.posZ);
        int dx = playerX - waypoint.getX();
        int dz = playerZ - waypoint.getZ();
        int dy = Math.abs(playerY - waypoint.getY());
        int horizontalDistanceSquared = (dx * dx) + (dz * dz);
        return horizontalDistanceSquared <= (horizontalRange * horizontalRange) && dy <= verticalRange;
    }
}
