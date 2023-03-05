/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.render.draw;

import com.google.common.cache.CacheLoader;
import journeymap.client.cartography.RGB;
import journeymap.client.model.Waypoint;
import journeymap.client.render.map.GridRenderer;
import journeymap.client.render.texture.TextureCache;
import journeymap.client.render.texture.TextureImpl;

import java.awt.geom.Point2D;

/**
 * @author techbrew 12/26/13.
 */
public class DrawWayPointStep implements DrawStep
{
    public final Waypoint waypoint;
    final Integer color;
    final Integer fontColor;
    final TextureImpl texture;
    final boolean isEdit;
    Point2D.Double lastPosition;
    Point2D.Double lastWindowPosition;
    boolean lastOnScreen;
    boolean showLabel;

    /**
     * Draw a waypoint on the map.
     *
     * @param waypoint
     */
    public DrawWayPointStep(Waypoint waypoint)
    {
        this(waypoint, waypoint.getColor(), waypoint.isDeathPoint() ? RGB.RED_RGB : waypoint.getSafeColor(), false);
    }

    /**
     * Draw a waypoint on the map.
     *
     * @param waypoint
     * @param color
     * @param fontColor
     */
    public DrawWayPointStep(Waypoint waypoint, Integer color, Integer fontColor, boolean isEdit)
    {
        this.waypoint = waypoint;
        this.color = color;
        this.fontColor = fontColor;
        this.isEdit = isEdit;
        this.texture = waypoint.getTexture();
    }

    public void setShowLabel(boolean showLabel)
    {
        this.showLabel = showLabel;
    }

    @Override
    public void draw(double xOffset, double yOffset, GridRenderer gridRenderer, float drawScale, double fontScale, double rotation)
    {
        if (!waypoint.isInPlayerDimension())
        {
            return;
        }

        Point2D.Double pixel = getPosition(xOffset, yOffset, gridRenderer, true);
        if (gridRenderer.isOnScreen(pixel))
        {
            if (showLabel)
            {
                Point2D labelPoint = gridRenderer.shiftWindowPosition(pixel.getX(), pixel.getY(), 0, rotation == 0 ? -texture.getHeight() : texture.getHeight());
                DrawUtil.drawLabel(waypoint.getName(), labelPoint.getX(), labelPoint.getY(), DrawUtil.HAlign.Center, DrawUtil.VAlign.Middle, RGB.BLACK_RGB, 180, fontColor, 255, fontScale, false, rotation);
            }
            if (isEdit)
            {
                TextureImpl editTex = TextureCache.instance().getWaypointEdit();
                DrawUtil.drawColoredImage(editTex, 255, color, pixel.getX() - (editTex.getWidth() / 2), pixel.getY() - editTex.getHeight() / 2, -rotation);
            }
            DrawUtil.drawColoredImage(texture, 255, color, pixel.getX() - (texture.getWidth() / 2), pixel.getY() - (texture.getHeight() / 2), -rotation);
        }
        else if (!isEdit)
        {
            gridRenderer.ensureOnScreen(pixel);
            //DrawUtil.drawColoredImage(offscreenTexture, 255, color, pixel.getX() - (offscreenTexture.width / 2), pixel.getY() - (offscreenTexture.height / 2));
            DrawUtil.drawColoredImage(texture, 255, color, pixel.getX() - (texture.getWidth() / 2), pixel.getY() - (texture.getHeight() / 2), -rotation);
        }
    }

    public void drawOffscreen(Point2D pixel, double rotation)
    {
        DrawUtil.drawColoredImage(texture, 255, color, pixel.getX() - (texture.getWidth() / 2), pixel.getY() - (texture.getHeight() / 2), -rotation);
    }

    public Point2D.Double getPosition(double xOffset, double yOffset, GridRenderer gridRenderer, boolean forceUpdate)
    {
        if (!forceUpdate && lastPosition != null)
        {
            return lastPosition;
        }

        double x = waypoint.getX();
        double z = waypoint.getZ();
        double halfBlock = Math.pow(2, gridRenderer.getZoom()) / 2;

        Point2D.Double pixel = gridRenderer.getBlockPixelInGrid(x, z);
        pixel.setLocation(pixel.getX() + halfBlock + xOffset, pixel.getY() + halfBlock + yOffset);
        lastPosition = pixel;
        lastWindowPosition = gridRenderer.getWindowPosition(lastPosition);
        return pixel;
    }

    public Point2D.Double getLastWindowPosition()
    {
        return lastWindowPosition;
    }

    public int getTextureHeight()
    {
        return texture.getHeight();
    }

    public int getTextureSize()
    {
        return Math.max(texture.getHeight(), texture.getWidth());
    }

    public boolean isOnScreen()
    {
        return lastOnScreen;
    }

    public void setOnScreen(boolean lastOnScreen)
    {
        this.lastOnScreen = lastOnScreen;
    }

    public static class SimpleCacheLoader extends CacheLoader<Waypoint, DrawWayPointStep>
    {
        @Override
        public DrawWayPointStep load(Waypoint waypoint) throws Exception
        {
            return new DrawWayPointStep(waypoint);
        }
    }
}
