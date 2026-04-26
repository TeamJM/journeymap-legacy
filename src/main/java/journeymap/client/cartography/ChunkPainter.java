/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography;

import journeymap.common.Journeymap;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
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

    private final Graphics2D g2D;
    private final BufferedImage img;
    private final int[] pixels;

    public ChunkPainter(BufferedImage buffer, Graphics2D g2D)
    {
        this.g2D = g2D;
        this.g2D.setComposite(ALPHA_OPAQUE);
        this.img = buffer;
        this.pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        Arrays.fill(this.pixels, 0);
    }

    /**
     * Darken the existing color.
     */
    public void paintDimOverlay(int x, int z, float alpha)
    {
        final int color = pixels[z * 16 + x];
        if (color != 0)
        {
            paintBlock(x, z, RGB.adjustBrightness(color, alpha));
        }
    }

    /**
     * Paint the block.
     */
    public void paintBlock(final int x, final int z, final int color)
    {
        pixels[z * 16 + x] = 0xFF000000 | color;
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
            Journeymap.getLogger().warn("Bad block at {},{},{}. Total bad blocks: {}", x, y, z, count);
        }
    }

    /**
     * Paint the blocks.  ChunkPainter can't be used after calling.
     */
    public void finishPainting()
    {
        try
        {
            g2D.drawImage(img, 0, 0, null);
        } finally
        {
            g2D.dispose();
        }
    }

}
