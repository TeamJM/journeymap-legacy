/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography.render;

import com.google.common.cache.RemovalNotification;
import journeymap.client.cartography.*;
import journeymap.client.data.DataCache;
import journeymap.client.log.LogFormatter;
import journeymap.client.log.StatTimer;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.common.Journeymap;
import net.minecraft.world.ChunkCoordIntPair;
import org.apache.logging.log4j.Level;

public class SurfaceRenderer extends BaseRenderer implements IChunkRenderer
{
    protected final Object chunkLock = new Object();
    protected final HeightsCache chunkSurfaceHeights;
    protected final SlopesCache chunkSurfaceSlopes;
    protected StatTimer renderSurfaceTimer = StatTimer.get("SurfaceRenderer.renderSurface");
    protected StatTimer renderSurfacePrepassTimer = StatTimer.get("SurfaceRenderer.renderSurface.CavePrepass");
    protected Strata strata = new Strata("Surface", 40, 8, false);
    protected float maxDepth = 8;

    public SurfaceRenderer()
    {
        this("Surface");
    }

    protected SurfaceRenderer(String cachePrefix)
    {
        // TODO: Write the caches to disk and we'll have some useful data available.
        this.cachePrefix = cachePrefix;
        columnPropertiesCache = new BlockColumnPropertiesCache(cachePrefix + "ColumnProps");
        chunkSurfaceHeights = new HeightsCache(cachePrefix + "Heights");
        chunkSurfaceSlopes = new SlopesCache(cachePrefix + "Slopes");
        DataCache.instance().addChunkMDListener(this);
    }

    @Override
    protected void updateOptions()
    {
        super.updateOptions();
        this.ambientColor = RGB.floats(tweakSurfaceAmbientColor);
    }

    /**
     * Render blocks in the chunk for the standard world, day only
     */
    @Override
    public boolean render(final ChunkPainter dayG2d, final ChunkMD chunkMd, final Integer ignored)
    {
        return render(dayG2d, null, chunkMd, null, false);
    }

    /**
     * Render blocks in the chunk for the standard world
     */
    public boolean render(final ChunkPainter dayG2d, final ChunkPainter nightG2d, final ChunkMD chunkMd)
    {
        return render(dayG2d, nightG2d, chunkMd, null, false);
    }

    /**
     * Render blocks in the chunk for the standard world.
     */
    public synchronized boolean render(final ChunkPainter dayG2d, final ChunkPainter nightG2d, final ChunkMD chunkMd, final Integer vSlice, final boolean cavePrePass)
    {
        StatTimer timer = cavePrePass ? renderSurfacePrepassTimer : renderSurfaceTimer;

        try
        {
            timer.start();

            updateOptions();

            // Initialize ChunkSub slopes if needed
            if (chunkSurfaceSlopes.getIfPresent(chunkMd.getCoord()) == null)
            {
                populateSlopes(chunkMd, null, chunkSurfaceHeights, chunkSurfaceSlopes);
            }

            // Render the chunk image
            return renderSurface(dayG2d, nightG2d, chunkMd, vSlice, cavePrePass);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            return false;
        }
        finally
        {
            strata.reset();
            timer.stop();
        }
    }


    /**
     * Render blocks in the chunk for the surface.
     */
    protected boolean renderSurface(final ChunkPainter dayG2d, final ChunkPainter nightG2d, final ChunkMD chunkMd, final Integer vSlice, final boolean cavePrePass)
    {
        boolean chunkOk = false;

        try
        {
            int sliceMaxY = 0;

            if (cavePrePass)
            {
                int[] sliceBounds = getVSliceBounds(chunkMd, vSlice);
                sliceMaxY = sliceBounds[1];
            }

            for (int x = 0; x < 16; x++)
            {
                blockLoop:
                for (int z = 0; z < 16; z++)
                {
                    strata.reset();
                    BlockMD topBlockMd = null;

                    int standardY = Math.max(0, getSurfaceBlockHeight(chunkMd, x, z, chunkSurfaceHeights));

                    // Should be painted only by cave renderer
                    if (cavePrePass && (standardY > sliceMaxY && (standardY - sliceMaxY) > maxDepth))
                    {
                        chunkOk = true;
                        dayG2d.paintBlackBlock(x, z);
                        continue;
                    }

                    int roofY = 0;
                    int y = standardY;

                    // TODO: Re-evaluate whether this section is necessary now that
                    // precipHeight is always used
                    roofY = Math.max(0, chunkMd.getPrecipitationHeight(x, z));
                    if (standardY < roofY)
                    {
                        // Is transparent roof above standard height?
                        int checkY = roofY;
                        while (checkY > standardY)
                        {
                            topBlockMd = BlockMD.getBlockMD(chunkMd, x, checkY, z);
                            if (topBlockMd.isTransparentRoof())
                            {
                                y = Math.max(standardY, checkY);
                                break;
                            }
                            else
                            {
                                checkY--;
                            }
                        }
                    }

                    if (roofY == 0 || standardY == 0)
                    {
                        dayG2d.paintVoidBlock(x, z);
                        if (!cavePrePass && nightG2d != null)
                        {
                            nightG2d.paintVoidBlock(x, z);
                        }
                        chunkOk = true;
                        continue blockLoop;
                    }

                    // Bathymetry - need to use water height instead of standardY, so we get the color blend
                    if (mapBathymetry)
                    {
                        standardY = getColumnProperty(PROP_WATER_HEIGHT, standardY, chunkMd, x, z);
                    }

                    topBlockMd = chunkMd.getTopBlockMD(x, standardY, z);

                    if (topBlockMd == null)
                    {
                        dayG2d.paintBadBlock(x, standardY, z);
                        nightG2d.paintBadBlock(x, standardY, z);
                        continue blockLoop;
                    }

                    // Plants/crops/double-tall need to check one or two blocks up
                    if (mapPlants || mapCrops)
                    {
                        BlockMD temp = chunkMd.getTopBlockMD(x, standardY + 1, z);
                        if ((mapPlants && temp.hasFlag(BlockMD.Flag.Plant)) || (mapCrops && temp.hasFlag(BlockMD.Flag.Crop)))
                        {
                            standardY += 1;
                        }
                    }

                    // Start using BlockColors stack
                    buildStrata(strata, roofY, chunkMd, x, standardY, z);

                    chunkOk = paintStrata(strata, dayG2d, nightG2d, chunkMd, topBlockMd, vSlice, x, y, z, cavePrePass) || chunkOk;
                }
            }
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().log(Level.WARN, LogFormatter.toString(t));
        }
        finally
        {
            strata.reset();
        }
        return chunkOk;
    }

    /**
     * Create a BlockStack.
     */
    protected void buildStrata(Strata strata, int roofY, ChunkMD chunkMd, int x, int y, int z)
    {
        BlockMD blockMD;

        // If under glass, add to color stack
        if (roofY > y)
        {
            while (roofY > y)
            {
                blockMD = BlockMD.getBlockMD(chunkMd, x, roofY, z);
                if (!blockMD.isAir())
                {
                    if (blockMD.isTransparentRoof())
                    {
                        strata.push(chunkMd, blockMD, x, roofY, z);
                        if (!mapTransparency)
                        {
                            break;
                        }
                    }
                }
                roofY--;
            }
        }

        if (mapTransparency || strata.isEmpty())
        {
            while (y >= 0)
            {
                blockMD = BlockMD.getBlockMD(chunkMd, x, y, z);

                if (!blockMD.isAir())
                {
                    strata.push(chunkMd, blockMD, x, y, z);

                    if (blockMD.getAlpha() == 1f || !mapTransparency)
                    {
                        break;
                    }
                }
                y--;
            }
        }
    }

    /**
     * Paint the image with the color derived from a BlockStack
     */
    protected boolean paintStrata(final Strata strata, final ChunkPainter dayG2d, final ChunkPainter nightG2d, final ChunkMD chunkMd, final BlockMD topBlockMd, final Integer vSlice, final int x, final int y, final int z, final boolean cavePrePass)
    {
        if (strata.isEmpty())
        {
            dayG2d.paintBadBlock(x, y, z);
            nightG2d.paintBadBlock(x, y, z);
            return false;
        }

        try
        {
            Stratum stratum;
            while (!strata.isEmpty())
            {
                stratum = strata.nextUp(this, true);
                if (strata.getRenderDayColor() == null || strata.getRenderNightColor() == null)
                {
                    strata.setRenderDayColor(stratum.getDayColor());
                    if (!cavePrePass)
                    {
                        strata.setRenderNightColor(stratum.getNightColor());
                    }
                }
                else
                {
                    strata.setRenderDayColor(RGB.blendWith(strata.getRenderDayColor(), stratum.getDayColor(), stratum.getBlockMD().getAlpha()));
                    if (!cavePrePass)
                    {
                        strata.setRenderNightColor(RGB.blendWith(strata.getRenderNightColor(), stratum.getNightColor(), stratum.getBlockMD().getAlpha()));
                    }
                }

                strata.release(stratum);

            } // end color stack

            // Shouldn't happen
            if (strata.getRenderDayColor() == null)
            {
                dayG2d.paintBadBlock(x, y, z);
                nightG2d.paintBadBlock(x, y, z);
                return false;
            }

            if (nightG2d != null)
            {
                // Shouldn't happen
                if (strata.getRenderNightColor() == null)
                {
                    nightG2d.paintBadBlock(x, y, z);
                    return false;
                }
            }

            // Now add bevel for slope
            if ((topBlockMd.isWater() && mapBathymetry) || !topBlockMd.hasNoShadow())
            {
                float slope = getSlope(chunkMd, topBlockMd, x, null, z, chunkSurfaceHeights, chunkSurfaceSlopes);
                if (slope != 1f)
                {
                    strata.setRenderDayColor(RGB.bevelSlope(strata.getRenderDayColor(), slope));
                    if (!cavePrePass)
                    {
                        strata.setRenderNightColor(RGB.bevelSlope(strata.getRenderNightColor(), slope));
                    }
                }
            }

            if (chunkMd.getHasNoSky())
            {
                // End: Only use night color
                dayG2d.paintBlock(x, z, strata.getRenderNightColor());
            }
            else
            {
                dayG2d.paintBlock(x, z, strata.getRenderDayColor());
                if (nightG2d != null)
                {
                    nightG2d.paintBlock(x, z, strata.getRenderNightColor());
                }
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }

        return true;
    }

    @Override
    public void onRemoval(RemovalNotification<Long, ChunkMD> notification)
    {
        synchronized (chunkLock)
        {
            long coord = notification.getKey();
            chunkSurfaceHeights.invalidate(coord);
            chunkSurfaceSlopes.invalidate(coord);
            columnPropertiesCache.invalidate(coord);
        }
    }
}
