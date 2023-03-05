/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography;

import journeymap.common.Journeymap;

import java.awt.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps arrays used to set colors and alphas for a chunk image,
 * does the actual update on Graphics2D in a single method to
 * try to be as efficient as possible.
 */
public class ChunkPainter
{
    public static final AlphaComposite ALPHA_OPAQUE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1F);
    public static final int COLOR_BLACK = Color.black.getRGB();
    public static final int COLOR_VOID = RGB.toInteger(17, 12, 25);
    protected static volatile AtomicLong badBlockCount = new AtomicLong(0);

    Integer[][] colors = new Integer[16][16];
    Graphics2D g2D;

    public ChunkPainter(Graphics2D g2D)
    {
        this.g2D = g2D;
        g2D.setComposite(ALPHA_OPAQUE);
    }

    /**
     * Darken the existing color.
     */
    public void paintDimOverlay(int x, int z, float alpha)
    {
        Integer color = colors[x][z];
        if (color != null)
        {
            paintBlock(x, z, RGB.adjustBrightness(color, alpha));
        }
    }

    /**
     * Paint the block.
     */
    public void paintBlock(final int x, final int z, final int color)
    {
        colors[x][z] = color;
    }

    /**
     * Paint the void.
     */
    public void paintVoidBlock(final int x, final int z)
    {
        paintBlock(x, z, COLOR_VOID);
    }

    /**
     * Paint the void.
     */
    public void paintBlackBlock(final int x, final int z)
    {
        paintBlock(x, z, COLOR_BLACK);
    }

    /**
     * It's a problem
     */
    public void paintBadBlock(final int x, final int y, final int z)
    {
        long count = badBlockCount.incrementAndGet();
        if (count == 1 || count % 10240 == 0)
        {
            Journeymap.getLogger().warn(
                    "Bad block at " + x + "," + y + "," + z + ". Total bad blocks: " + count
            );
        }
    }

    /**
     * Paint the blocks.  ChunkPainter can't be used after calling.
     */
    public void finishPainting()
    {
        Integer color;
        int lastColor = -1;

        try
        {
            for (int z = 0; z < 16; z++)
            {
                for (int x = 0; x < 16; x++)
                {
                    color = colors[x][z];
                    if (color == null)
                    {
                        continue;
                    }

                    // Update color
                    if (color != lastColor)
                    {
                        lastColor = color;
                        g2D.setPaint(RGB.paintOf(color));
                    }

                    g2D.fillRect(x, z, 1, 1);
                }
            }
        }
        finally
        {
            g2D.dispose();
            g2D = null;
            colors = null;
        }
    }
}
