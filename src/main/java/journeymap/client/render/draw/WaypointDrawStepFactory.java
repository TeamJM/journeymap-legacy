/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.render.draw;

import journeymap.client.JourneymapClient;
import journeymap.client.data.DataCache;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.Waypoint;
import journeymap.client.render.map.GridRenderer;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Renders waypoints in the MapOverlay.
 *
 * @author techbrew
 */
public class WaypointDrawStepFactory
{
    final List<DrawWayPointStep> drawStepList = new ArrayList<DrawWayPointStep>();

    public List<DrawWayPointStep> prepareSteps(Collection<Waypoint> waypoints, GridRenderer grid, boolean checkDistance, boolean showLabel)
    {
        Minecraft mc = ForgeHelper.INSTANCE.getClient();
        EntityPlayer player = mc.thePlayer;
        int dimension = player.dimension;
        int maxDistance = JourneymapClient.getWaypointProperties().maxDistance.get();
        checkDistance = checkDistance && maxDistance > 0;
        Vec3 playerVec = checkDistance ? ForgeHelper.INSTANCE.getEntityPositionVector(player) : null;
        drawStepList.clear();

        try
        {
            for (Waypoint waypoint : waypoints)
            {
                if (waypoint.isEnable() && waypoint.isInPlayerDimension())
                {
                    if (checkDistance)
                    {
                        // Get view distance from waypoint
                        final double actualDistance = playerVec.distanceTo(waypoint.getPosition());
                        if (actualDistance > maxDistance)
                        {
                            continue;
                        }
                    }

                    DrawWayPointStep wayPointStep = DataCache.instance().getDrawWayPointStep(waypoint);
                    if (wayPointStep != null)
                    {
                        drawStepList.add(wayPointStep);
                        wayPointStep.setShowLabel(showLabel);
                    }
                }
            }
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Error during prepareSteps: " + LogFormatter.toString(t));
        }

        return drawStepList;
    }
}
