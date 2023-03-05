/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.io;


import journeymap.client.Constants;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.MapType;
import journeymap.client.model.RegionCoord;
import journeymap.client.model.RegionImageCache;
import journeymap.client.render.map.TileDrawStep;
import journeymap.client.render.map.TileDrawStepCache;
import journeymap.common.Journeymap;
import net.minecraft.world.ChunkCoordIntPair;
import org.apache.logging.log4j.Level;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegionImageHandler
{
    private RegionImageHandler()
    {
    }

    // Get singleton instance.  Concurrency-safe.
    public static RegionImageHandler getInstance()
    {
        return Holder.INSTANCE;
    }

    public static File getImageDir(RegionCoord rCoord, MapType mapType)
    {
        File dimDir = rCoord.dimDir.toFile();
        File subDir = null;
        if (mapType.isUnderground())
        {
            subDir = new File(dimDir, Integer.toString(mapType.vSlice));
        }
        else
        {
            subDir = new File(dimDir, mapType.name());
        }
        if (!subDir.exists())
        {
            subDir.mkdirs();
        }
        return subDir;
    }

    @Deprecated
    public static File getDimensionDir(File worldDir, int dimension)
    {
        File dimDir = new File(worldDir, "DIM" + dimension); //$NON-NLS-1$
        if (!dimDir.exists())
        {
            dimDir.mkdirs();
        }
        return dimDir;
    }

    public static File getRegionImageFile(RegionCoord rCoord, MapType mapType, boolean allowLegacy)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(rCoord.regionX).append(",").append(rCoord.regionZ).append(".png"); //$NON-NLS-1$ //$NON-NLS-2$
        File regionFile = new File(getImageDir(rCoord, mapType), sb.toString());

        return regionFile;
    }

    public static BufferedImage createBlankImage(int width, int height)
    {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = img.createGraphics();
        graphics2D.setFont(new Font("Arial", Font.BOLD, 18));
        graphics2D.setBackground(Color.black);
        graphics2D.setColor(Color.yellow);
        graphics2D.drawString("BLANK", 0, 0);
        return img;
    }

    public static BufferedImage readRegionImage(File regionFile, boolean returnNull)
    {
        BufferedImage image = null;
        if (regionFile.canRead())
        {
            try
            {
                image = ImageIO.read(regionFile);
            }
            catch (Exception e)
            {
                String error = "Region file produced error: " + regionFile + ": " + LogFormatter.toString(e);
                Journeymap.getLogger().error(error);
            }
        }

        return image;
    }

    public static BufferedImage getImage(File file)
    {
        try
        {
            return ImageIO.read(file);
        }
        catch (IOException e)
        {
            String error = "Could not get image from file: " + file + ": " + (e.getMessage());
            Journeymap.getLogger().error(error);
            return null;
        }
    }

    /**
     * Used by MapOverlay to let the image dimensions be directly specified (as a power of 2).
     */
    public static synchronized BufferedImage getMergedChunks(final File worldDir, final ChunkCoordIntPair startCoord, final ChunkCoordIntPair endCoord, final MapType mapType,
                                                             final Boolean useCache, BufferedImage image, final Integer imageWidth, final Integer imageHeight,
                                                             final boolean allowNullImage, boolean showGrid)
    {
        long start = 0, stop = 0;
        start = System.currentTimeMillis();

        final int initialWidth = (endCoord.chunkXPos - startCoord.chunkXPos + 1) * 16;
        final int initialHeight = (endCoord.chunkZPos - startCoord.chunkZPos + 1) * 16;

        if (image == null || image.getWidth() != initialWidth || imageHeight != initialHeight)
        {
            image = new BufferedImage(initialWidth, initialHeight, BufferedImage.TYPE_INT_ARGB);
        }
        final Graphics2D g2D = initRenderingHints(image.createGraphics());
        g2D.clearRect(0, 0, imageWidth, imageHeight);

        final RegionImageCache cache = RegionImageCache.instance();

        RegionCoord rc = null;
        BufferedImage regionImage = null;

        final int rx1 = RegionCoord.getRegionPos(startCoord.chunkXPos);
        final int rx2 = RegionCoord.getRegionPos(endCoord.chunkXPos);
        final int rz1 = RegionCoord.getRegionPos(startCoord.chunkZPos);
        final int rz2 = RegionCoord.getRegionPos(endCoord.chunkZPos);

        int rminCx, rminCz, rmaxCx, rmaxCz, sx1, sy1, sx2, sy2, dx1, dx2, dy1, dy2;

        boolean imageDrawn = false;
        for (int rx = rx1; rx <= rx2; rx++)
        {
            for (int rz = rz1; rz <= rz2; rz++)
            {
                rc = new RegionCoord(worldDir, rx, rz, mapType.dimension);
                regionImage = cache.getRegionImageSet(rc).getImage(mapType);

                if (regionImage == null)
                {
                    continue;
                }

                rminCx = Math.max(rc.getMinChunkX(), startCoord.chunkXPos);
                rminCz = Math.max(rc.getMinChunkZ(), startCoord.chunkZPos);
                rmaxCx = Math.min(rc.getMaxChunkX(), endCoord.chunkXPos);
                rmaxCz = Math.min(rc.getMaxChunkZ(), endCoord.chunkZPos);

                int xoffset = rc.getMinChunkX() * 16;
                int yoffset = rc.getMinChunkZ() * 16;
                sx1 = (rminCx * 16) - xoffset;
                sy1 = (rminCz * 16) - yoffset;
                sx2 = sx1 + ((rmaxCx - rminCx + 1) * 16);
                sy2 = sy1 + ((rmaxCz - rminCz + 1) * 16);

                xoffset = startCoord.chunkXPos * 16;
                yoffset = startCoord.chunkZPos * 16;
                dx1 = (startCoord.chunkXPos * 16) - xoffset;
                dy1 = (startCoord.chunkZPos * 16) - yoffset;
                dx2 = dx1 + ((endCoord.chunkXPos - startCoord.chunkXPos + 1) * 16);
                dy2 = dy1 + ((endCoord.chunkZPos - startCoord.chunkZPos + 1) * 16);

                g2D.drawImage(regionImage, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
                imageDrawn = true;
            }
        }

        // Show chunk grid
        if (imageDrawn)
        {
            if (showGrid)
            {

                if (mapType.isDay())
                {
                    g2D.setColor(Color.black);
                    g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.25F));
                }
                else
                {
                    g2D.setColor(Color.gray);
                    g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.1F));
                }

                for (int x = 0; x <= initialWidth; x += 16)
                {
                    g2D.drawLine(x, 0, x, initialHeight);
                }

                for (int z = 0; z <= initialHeight; z += 16)
                {
                    g2D.drawLine(0, z, initialWidth, z);
                }
            }
        }

        g2D.dispose();

        if (Journeymap.getLogger().isEnabled(Level.DEBUG))
        {
            stop = System.currentTimeMillis();
            Journeymap.getLogger().debug("getMergedChunks time: " + (stop - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (allowNullImage && !imageDrawn)
        {
            return null;
        }

        // Scale if needed
        if (imageHeight != null && imageWidth != null && (initialHeight != imageHeight || initialWidth != imageWidth))
        {
            final BufferedImage scaledImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = initRenderingHints(scaledImage.createGraphics());
            g.drawImage(image, 0, 0, imageWidth, imageHeight, null);
            g.dispose();
            return scaledImage;
        }
        else
        {
            return image;
        }
    }

    /**
     * Used by MapOverlay to let the image dimensions be directly specified (as a power of 2).
     */
    public static synchronized List<TileDrawStep> getTileDrawSteps(final File worldDir, final ChunkCoordIntPair startCoord,
                                                                   final ChunkCoordIntPair endCoord, final MapType mapType,
                                                                   Integer zoom, boolean highQuality)
    {
        boolean isUnderground = mapType.isUnderground();

        final int rx1 = RegionCoord.getRegionPos(startCoord.chunkXPos);
        final int rx2 = RegionCoord.getRegionPos(endCoord.chunkXPos);
        final int rz1 = RegionCoord.getRegionPos(startCoord.chunkZPos);
        final int rz2 = RegionCoord.getRegionPos(endCoord.chunkZPos);

        List<TileDrawStep> drawSteps = new ArrayList<TileDrawStep>();

        RegionCoord rc;
        int rminCx, rminCz, rmaxCx, rmaxCz, sx1, sy1, sx2, sy2;

        for (int rx = rx1; rx <= rx2; rx++)
        {
            for (int rz = rz1; rz <= rz2; rz++)
            {
                rc = new RegionCoord(worldDir, rx, rz, mapType.dimension);
                rminCx = Math.max(rc.getMinChunkX(), startCoord.chunkXPos);
                rminCz = Math.max(rc.getMinChunkZ(), startCoord.chunkZPos);
                rmaxCx = Math.min(rc.getMaxChunkX(), endCoord.chunkXPos);
                rmaxCz = Math.min(rc.getMaxChunkZ(), endCoord.chunkZPos);

                int xoffset = rc.getMinChunkX() * 16;
                int yoffset = rc.getMinChunkZ() * 16;
                sx1 = (rminCx * 16) - xoffset;
                sy1 = (rminCz * 16) - yoffset;
                sx2 = sx1 + ((rmaxCx - rminCx + 1) * 16);
                sy2 = sy1 + ((rmaxCz - rminCz + 1) * 16);

                drawSteps.add(TileDrawStepCache.getOrCreate(mapType, rc, zoom, highQuality, sx1, sy1, sx2, sy2));
            }
        }

        return drawSteps;
    }

    public static File getBlank512x512ImageFile()
    {
        final File dataDir = new File(FileHandler.MinecraftDirectory, Constants.DATA_DIR);
        final File tmpFile = new File(dataDir, "blank512x512.png");
        if (!tmpFile.canRead())
        {
            BufferedImage image;
            image = createBlankImage(512, 512);
            try
            {
                dataDir.mkdirs();
                ImageIO.write(image, "png", tmpFile);
                tmpFile.setReadOnly();
                tmpFile.deleteOnExit();
            }
            catch (IOException e)
            {
                Journeymap.getLogger().error("Could not create blank temp file " + tmpFile + ": " + LogFormatter.toString(e));
            }
        }
        return tmpFile;
    }

    public static Graphics2D initRenderingHints(Graphics2D g)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    // On-demand-holder for instance
    private static class Holder
    {
        private static final RegionImageHandler INSTANCE = new RegionImageHandler();
    }

}
