/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.fullscreen.layer;

import journeymap.client.JourneymapClient;
import journeymap.client.cartography.RGB;
import journeymap.client.data.DataCache;
import journeymap.client.data.WaypointsData;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.BlockCoordIntPair;
import journeymap.client.model.Waypoint;
import journeymap.client.properties.FullMapProperties;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.draw.DrawWayPointStep;
import journeymap.client.render.map.GridRenderer;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.fullscreen.Fullscreen;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.input.Mouse;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Waypoint selection/creation.
 */
public class WaypointLayer implements LayerDelegate.Layer
{
    private final long hoverDelay = 100;
    private final List<DrawStep> drawStepList = new ArrayList<DrawStep>(1);
    private final BlockOutlineDrawStep clickDrawStep = new BlockOutlineDrawStep(new BlockCoordIntPair(0, 0));
    BlockCoordIntPair lastCoord = null;

    long lastClick = 0;
    long startHover = 0;

    DrawWayPointStep selectedWaypointStep = null;
    Waypoint selected = null;


    public WaypointLayer()
    {
    }

    @Override
    public List<DrawStep> onMouseMove(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord)
    {
        if (!WaypointsData.isManagerEnabled())
        {
            return Collections.EMPTY_LIST;
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

        AxisAlignedBB area = ForgeHelper.INSTANCE.getBoundingBox(blockCoord.x - proximity, -1, blockCoord.z - proximity,
                blockCoord.x + proximity, mc.theWorld.getActualHeight() + 1, blockCoord.z + proximity);

        if (!lastCoord.equals(blockCoord))
        {
            if (!area.isVecInside(ForgeHelper.INSTANCE.newVec3(lastCoord.x, 1, lastCoord.z)))
            {
                selected = null;
                lastCoord = blockCoord;
                startHover = now;
                return Collections.EMPTY_LIST;
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

        if (now - startHover < hoverDelay)
        {
            return Collections.EMPTY_LIST;
        }

        int dimension = mc.thePlayer.dimension;

        // check for existing
        Collection<Waypoint> waypoints = DataCache.instance().getWaypoints(false);
        ArrayList<Waypoint> proximal = new ArrayList<Waypoint>();
        for (Waypoint waypoint : waypoints)
        {
            if (!waypoint.isReadOnly() && waypoint.isEnable() && waypoint.isInPlayerDimension())
            {
                if (area.isVecInside(ForgeHelper.INSTANCE.newVec3(waypoint.getX(), waypoint.getY(), waypoint.getZ())))
                {
                    proximal.add(waypoint);
                }
            }
        }

        if (!proximal.isEmpty())
        {
            if (proximal.size() > 1)
            {
                sortByDistance(proximal, blockCoord, dimension);
            }
            select(proximal.get(0));
        }

        return drawStepList;
    }

    @Override
    public List<DrawStep> onMouseClick(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord)
    {
        if (!WaypointsData.isManagerEnabled())
        {
            return Collections.EMPTY_LIST;
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

    private void sortByDistance(List<Waypoint> waypoints, final BlockCoordIntPair blockCoord, final int dimension)
    {
        Collections.sort(waypoints, new Comparator<Waypoint>()
        {
            @Override
            public int compare(Waypoint o1, Waypoint o2)
            {
                return Double.compare(getDistance(o1), getDistance(o2));
            }

            private double getDistance(Waypoint waypoint)
            {
                double dx = waypoint.getX() - blockCoord.x;
                double dz = waypoint.getZ() - blockCoord.z;
                return (Math.sqrt(dx * dx + dz * dz));
            }
        });
    }

    private void select(Waypoint waypoint)
    {
        selected = waypoint;
        selectedWaypointStep = new DrawWayPointStep(waypoint, waypoint.getColor(), RGB.WHITE_RGB, true);
        drawStepList.add(selectedWaypointStep);
    }

    private int getProximity()
    {
        FullMapProperties fullMapProperties = JourneymapClient.getFullMapProperties();
        int blockSize = (int) Math.max(1, Math.pow(2, fullMapProperties.zoomLevel.get()));
        return Math.max(1, 8 / blockSize);
    }

    private void unclick()
    {
        clickDrawStep.blockCoord = new BlockCoordIntPair(Integer.MAX_VALUE, Integer.MAX_VALUE);
        drawStepList.remove(clickDrawStep);
    }

    class BlockOutlineDrawStep implements DrawStep
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
