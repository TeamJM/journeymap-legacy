package journeymap.client.forge.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.JourneymapClient;
import journeymap.client.log.ChatLog;
import journeymap.client.model.Waypoint;
import journeymap.client.properties.WaypointProperties;
import journeymap.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import openblocks.api.GraveRetrievedEvent;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Removes the local player's deathpoint once their OpenBlocks grave has been emptied.
 * The event is only posted server-side, so this only works on an integrated server.
 */
@SideOnly(Side.CLIENT)
public class GraveRetrievedHandler implements EventHandlerManager.EventHandler
{
    // OpenBlocks timestamps the grave on the server whereas, Journeymap creates the waypoint when
    // the client player isDead, which is set 20 ticks later, see EntityLivingDeath#onDeathUpdate.
    // So both the timestamp and location may differ.
    private static final long MAX_DEATH_TIME_DIFFERENCE_MS = 3000;
    private static final int MAX_GRAVE_DISTANCE = 8;

    @Override
    public EnumSet<EventHandlerManager.BusType> getBus()
    {
        return EnumSet.of(EventHandlerManager.BusType.MinecraftForgeBus);
    }

    // Event fired on the server thread, so the feature only works on integrated servers
    @SubscribeEvent
    public void onGraveRetrieved(GraveRetrievedEvent event)
    {
        if (event.ownerId != null && event.deathTime != GraveRetrievedEvent.UNKNOWN_DEATH_TIME)
        {
            final UUID ownerID = event.ownerId;
            final int dimension = event.entityPlayer.worldObj.provider.dimensionId;
            final long deathTime = event.deathTime;
            final int x = event.x;
            final int y = event.y;
            final int z = event.z;
            // addScheduledTask
            Minecraft.getMinecraft().func_152344_a(() -> removeMatchingDeathpoint(ownerID, dimension, deathTime, x, y, z));
        }
    }

    private void removeMatchingDeathpoint(UUID ownerID, int dimension, long deathTime, int x, int y, int z)
    {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        WaypointProperties properties = JourneymapClient.getWaypointProperties();
        if (player == null || !properties.managerEnabled.get() || !properties.deleteDeathpointOnGraveRetrieved.get())
        {
            return;
        }
        if (!player.getGameProfile().getId().equals(ownerID))
        {
            return;
        }

        Waypoint closest = null;
        long closestTimeDifference = MAX_DEATH_TIME_DIFFERENCE_MS;
        for (Waypoint waypoint : WaypointStore.instance().getAll())
        {
            if (!waypoint.isDeathPoint() || waypoint.getCreationTime() <= 0 || !waypoint.getDimensions().contains(dimension))
            {
                continue;
            }
            int dx = waypoint.getX() - x;
            int dy = waypoint.getY() - y;
            int dz = waypoint.getZ() - z;
            if (dx * dx + dy * dy + dz * dz > MAX_GRAVE_DISTANCE * MAX_GRAVE_DISTANCE)
            {
                continue;
            }
            // waypoint is created 20 ticks later
            long timeDifference = Math.abs((waypoint.getCreationTime() - 20 * 50) - deathTime);
            if (timeDifference <= closestTimeDifference)
            {
                closest = waypoint;
                closestTimeDifference = timeDifference;
            }
        }

        if (closest != null)
        {
            WaypointStore.instance().remove(closest);
            ChatLog.announceI18N("jm.waypoint.deathpoint_cleared_grave_retrieved", closest.getName());
        }
    }
}
