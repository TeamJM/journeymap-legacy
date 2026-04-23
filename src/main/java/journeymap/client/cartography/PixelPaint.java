/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

/**
 * A simple combo-implementation of Paint and PaintContext designed for frequent reuse.
 * Based on code in java.awt.Color and java.awt.ColorPaintContext.
 */
class PixelPaint implements Paint, PaintContext
{
    final ColorModel colorModel = ColorModel.getRGBdefault();
    WritableRaster intRaster;
    int[] rasterData;
    int rgbColor;

    /**
     * Sets the rgb color to use.
     */
    Paint setColor(int rgbColor)
    {
        this.rgbColor = rgbColor;
        return this;
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints)
    {
        return this; // The hints can all be ignored, including the ColorModel.
    }

    @Override
    public int getTransparency()
    {
        return Transparency.OPAQUE;
    }

    @Override
    public void dispose()
    {
        // Nothing needs to be reclaimed
    }

    @Override
    public ColorModel getColorModel()
    {
        return colorModel;
    }

    @Override
    public Raster getRaster(int x, int y, int w, int h)
    {
        synchronized (this)
        {
            WritableRaster raster = this.intRaster;
            int[] data = this.rasterData;

            if (raster == null || w > raster.getWidth() || h > raster.getHeight())
            {
                raster = getColorModel().createCompatibleWritableRaster(w, h);
                data = ((DataBufferInt) raster.getDataBuffer()).getData();

                // Only reuse if the raster is for a single pixel
                if (w == 1 && h == 1)
                {
                    this.intRaster = raster;
                    this.rasterData = data;
                }
            }

            Arrays.fill(data, this.rgbColor);

            return raster;
        }
    }
}
