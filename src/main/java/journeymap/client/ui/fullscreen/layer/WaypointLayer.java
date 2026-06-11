/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.fullscreen.layer;

import journeymap.client.JourneymapClient;
import journeymap.client.cartography.RGB;
import journeymap.client.data.DataCache;
import journeymap.client.data.WaypointsData;
import journeymap.client.model.BlockCoordIntPair;
import journeymap.client.model.Waypoint;
import journeymap.client.properties.WaypointProperties;
import journeymap.client.properties.FullMapProperties;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.draw.DrawWayPointStep;
import journeymap.client.render.map.GridRenderer;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.ui.fullscreen.context.FullscreenContextTarget;
import journeymap.client.ui.fullscreen.context.MapLocationContextTarget;
import journeymap.client.ui.fullscreen.context.WaypointContextTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.input.Mouse;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Waypoint selection/creation.
 */
public class WaypointLayer implements LayerDelegate.Layer
{
    private static final long HOVER_DELAY_MS = 100L;
    private static final List<DrawStep> NO_DRAW_STEPS = Collections.emptyList();
    private final List<DrawStep> drawStepList = new ArrayList<>(1);
    private final BlockOutlineDrawStep clickDrawStep = new BlockOutlineDrawStep(new BlockCoordIntPair(0, 0));
    private BlockCoordIntPair lastCoord = null;
    private BlockCoordIntPair lastResolvedCoord = null;
    private Waypoint lastResolvedWaypoint = null;
    private Integer lastResolvedY = null;

    private long lastClick = 0;
    private long startHover = 0;

    private Waypoint selected = null;
    private DrawWayPointStep selectedWaypointStep = null;


    public WaypointLayer()
    {
    }

    public void clearSelection()
    {
        selected = null;
        selectedWaypointStep = null;
        lastCoord = null;
        lastResolvedCoord = null;
        lastResolvedWaypoint = null;
        lastResolvedY = null;
        drawStepList.clear();
    }

    @Override
    public List<DrawStep> onMouseMove(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord)
    {
        if (!WaypointsData.isManagerEnabled())
        {
            return NO_DRAW_STEPS;
        }

        drawStepList.clear();
        drawStepList.add(clickDrawStep);

        if (lastCoord == null)
        {
            lastCoord = blockCoord;
        }

        long now = Minecraft.getSystemTime();

        // Add click draw step
        if (!blockCoord.equals(clickDrawStep.blockCoord))
        {
            unclick();
        }

        // Get search area
        int proximity = getProximity();

        if (!lastCoord.equals(blockCoord))
        {
            if (!isWithinHorizontalRange(blockCoord, lastCoord.x, lastCoord.z, proximity))
            {
                selected = null;
                selectedWaypointStep = null;
                lastCoord = blockCoord;
                startHover = now;
                return NO_DRAW_STEPS;
            }
        }
        else
        {
            if (selected != null)
            {
                select(selected);
                return drawStepList;
            }
        }

        if (now - startHover < HOVER_DELAY_MS)
        {
            return NO_DRAW_STEPS;
        }

        Waypoint nearestWaypoint = getNearestWaypoint(mc, blockCoord);
        if (nearestWaypoint != null)
        {
            select(nearestWaypoint);
        }
        return drawStepList;
    }

    @Override
    public List<DrawStep> onMouseClick(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord)
    {
        if (!WaypointsData.isManagerEnabled())
        {
            return NO_DRAW_STEPS;
        }

        // check for double-click
        long sysTime = Minecraft.getSystemTime();
        boolean doubleClick = sysTime - this.lastClick < 450L;
        this.lastClick = sysTime;

        if (!drawStepList.contains(clickDrawStep))
        {
            drawStepList.add(clickDrawStep);
        }

        if (!doubleClick || !blockCoord.equals(clickDrawStep.blockCoord))
        {
            clickDrawStep.blockCoord = blockCoord;
            return drawStepList;
        }

        // Edit selected waypoint
        if (selected != null)
        {
            UIManager.getInstance().openWaypointManager(selected, new Fullscreen()); // TODO: This could be a problem
            return drawStepList;
        }

        // Check chunk
        Chunk chunk = mc.theWorld.getChunkFromChunkCoords(blockCoord.x >> 4, blockCoord.z >> 4);
        int y = -1;
        if (!chunk.isEmpty())
        {
            y = Math.max(1, chunk.getPrecipitationHeight(blockCoord.x & 15, blockCoord.z & 15));
        }

        // Create waypoint
        Waypoint waypoint = Waypoint.at(blockCoord.x, y, blockCoord.z, Waypoint.Type.Normal, mc.thePlayer.dimension);
        UIManager.getInstance().openWaypointEditor(waypoint, true, new Fullscreen()); // TODO: This could be a problem

        return drawStepList;
    }

    public FullscreenContextTarget getContextTarget(Minecraft mc, BlockCoordIntPair blockCoord)
    {
        resolveContext(mc, blockCoord);
        Waypoint nearestWaypoint = lastResolvedWaypoint;
        if (nearestWaypoint != null)
        {
            return new WaypointContextTarget(nearestWaypoint);
        }

        Integer y = lastResolvedY;
        int fallbackY = MathHelper.floor_double(mc.thePlayer.posY);
        return new MapLocationContextTarget(blockCoord.x, fallbackY, blockCoord.z, y, mc.thePlayer.dimension);
    }

    private Waypoint getNearestWaypoint(Minecraft mc, BlockCoordIntPair blockCoord)
    {
        resolveContext(mc, blockCoord);
        return lastResolvedWaypoint;
    }

    private void resolveContext(Minecraft mc, BlockCoordIntPair blockCoord)
    {
        if (blockCoord.equals(lastResolvedCoord))
        {
            return;
        }

        lastResolvedCoord = blockCoord;
        lastResolvedWaypoint = resolveNearestWaypoint(mc, blockCoord);
        lastResolvedY = getKnownY(mc, blockCoord);
    }

    private Waypoint resolveNearestWaypoint(Minecraft mc, BlockCoordIntPair blockCoord)
    {
        int proximity = getProximity();
        Collection<Waypoint> waypoints = DataCache.instance().getWaypoints(false);
        Waypoint nearestWaypoint = null;
        long nearestDistanceSquared = Long.MAX_VALUE;
        for (Waypoint waypoint : waypoints)
        {
            if (!waypoint.isReadOnly() && waypoint.isEnable() && waypoint.isInPlayerDimension()
                    && isWithinHorizontalRange(blockCoord, waypoint.getX(), waypoint.getZ(), proximity))
            {
                long distanceSquared = getHorizontalDistanceSquared(blockCoord, waypoint.getX(), waypoint.getZ());
                if (distanceSquared < nearestDistanceSquared)
                {
                    nearestDistanceSquared = distanceSquared;
                    nearestWaypoint = waypoint;
                }
            }
        }
        return nearestWaypoint;
    }

    private Integer getKnownY(Minecraft mc, BlockCoordIntPair blockCoord)
    {
        Chunk chunk = mc.theWorld.getChunkFromChunkCoords(blockCoord.x >> 4, blockCoord.z >> 4);
        if (chunk.isEmpty())
        {
            return null;
        }
        return Math.max(1, chunk.getPrecipitationHeight(blockCoord.x & 15, blockCoord.z & 15));
    }

    private boolean isWithinHorizontalRange(BlockCoordIntPair blockCoord, int x, int z, int proximity)
    {
        return Math.abs(x - blockCoord.x) <= proximity && Math.abs(z - blockCoord.z) <= proximity;
    }

    private long getHorizontalDistanceSquared(BlockCoordIntPair blockCoord, int x, int z)
    {
        long dx = x - blockCoord.x;
        long dz = z - blockCoord.z;
        return (dx * dx) + (dz * dz);
    }

    private void select(Waypoint waypoint)
    {
        if (selectedWaypointStep == null || selected != waypoint)
        {
            selectedWaypointStep = new DrawWayPointStep(waypoint, waypoint.getColor(), RGB.WHITE_RGB, true);
        }
        selected = waypoint;
        drawStepList.add(selectedWaypointStep);
    }

    private int getProximity()
    {
        WaypointProperties waypointProperties = JourneymapClient.getWaypointProperties();
        FullMapProperties fullMapProperties = JourneymapClient.getFullMapProperties();
        int blockSize = (int) Math.max(1, Math.pow(2, fullMapProperties.zoomLevel.get()));
        int dynamicProximity = (int) Math.ceil(20D / blockSize);
        int arrivalProximity = waypointProperties == null ? 0 : waypointProperties.arrivalHorizontalRange.get();
        return Math.max(4, Math.min(12, Math.max(dynamicProximity, arrivalProximity + 2)));
    }

    private void unclick()
    {
        clickDrawStep.blockCoord = new BlockCoordIntPair(Integer.MAX_VALUE, Integer.MAX_VALUE);
        drawStepList.remove(clickDrawStep);
    }

    private class BlockOutlineDrawStep implements DrawStep
    {
        BlockCoordIntPair blockCoord;

        BlockOutlineDrawStep(BlockCoordIntPair blockCoord)
        {
            this.blockCoord = blockCoord;
        }

        @Override
        public void draw(double xOffset, double yOffset, GridRenderer gridRenderer, float drawScale, double fontScale, double rotation)
        {

            if (Mouse.isButtonDown(0))
            {
                return;
            }

            if (xOffset != 0 || yOffset != 0)
            {
                unclick();
                return;
            }

            double x = blockCoord.x;
            double z = blockCoord.z;
            double size = Math.pow(2, gridRenderer.getZoom());
            double thick = gridRenderer.getZoom() < 2 ? 1 : 2;

            Point2D.Double pixel = gridRenderer.getBlockPixelInGrid(x, z);
            pixel.setLocation(pixel.getX() + xOffset, pixel.getY() + yOffset);
            if (gridRenderer.isOnScreen(pixel))
            {
                DrawUtil.drawRectangle(pixel.getX() - (thick * thick), pixel.getY() - (thick * thick), size + (thick * 4), thick, RGB.BLACK_RGB, 150);
                DrawUtil.drawRectangle(pixel.getX() - thick, pixel.getY() - thick, size + (thick * thick), thick, RGB.WHITE_RGB, 255);

                DrawUtil.drawRectangle(pixel.getX() - (thick * thick), pixel.getY() - thick, thick, size + (thick * thick), RGB.BLACK_RGB, 150);
                DrawUtil.drawRectangle(pixel.getX() - thick, pixel.getY(), thick, size, RGB.WHITE_RGB, 255);

                DrawUtil.drawRectangle(pixel.getX() + size, pixel.getY(), thick, size, RGB.WHITE_RGB, 255);
                DrawUtil.drawRectangle(pixel.getX() + size + thick, pixel.getY() - thick, thick, size + (thick * thick), RGB.BLACK_RGB, 150);

                DrawUtil.drawRectangle(pixel.getX() - thick, pixel.getY() + size, size + (thick * thick), thick, RGB.WHITE_RGB, 255);
                DrawUtil.drawRectangle(pixel.getX() - (thick * thick), pixel.getY() + size + thick, size + (thick * 4), thick, RGB.BLACK_RGB, 150);
            }
        }
    }
}
