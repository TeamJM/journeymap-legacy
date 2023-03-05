/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography.render;

import com.google.common.cache.RemovalNotification;
import journeymap.client.JourneymapClient;
import journeymap.client.cartography.*;
import journeymap.client.log.LogFormatter;
import journeymap.client.log.StatTimer;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.client.properties.CoreProperties;
import journeymap.common.Journeymap;
import net.minecraft.world.ChunkCoordIntPair;

/**
 * Renders chunk image for caves in the overworld.
 */
public class CaveRenderer extends BaseRenderer implements IChunkRenderer
{
    private final Object chunkLock = new Object();
    private final HeightsCache[] chunkSliceHeights = new HeightsCache[16];
    private final SlopesCache[] chunkSliceSlopes = new SlopesCache[16];
    protected CoreProperties coreProperties;
    protected SurfaceRenderer surfaceRenderer;
    protected StatTimer renderCaveTimer = StatTimer.get("CaveRenderer.render");
    protected Strata strata = new Strata("Cave", 40, 8, true);
    protected float defaultDim = .2f;
    protected boolean mapSurfaceAboveCaves;

    /**
     * Takes an instance of the surface renderer in order to do a prepass when the surface
     * intersects the slice being mapped.
     */
    public CaveRenderer(SurfaceRenderer surfaceRenderer)
    {
        this.surfaceRenderer = surfaceRenderer;
        cachePrefix = "Cave";

        updateOptions();

        // TODO: Put these in properties?
        shadingSlopeMin = 0.2f;
        shadingSlopeMax = 1.1f;
        shadingPrimaryDownslopeMultiplier = .7f;
        shadingPrimaryUpslopeMultiplier = 1.05f;
        shadingSecondaryDownslopeMultiplier = .99f;
        shadingSecondaryUpslopeMultiplier = 1.01f;
    }

    @Override
    protected void updateOptions()
    {
        super.updateOptions();

        mapSurfaceAboveCaves = JourneymapClient.getCoreProperties().mapSurfaceAboveCaves.get();
    }

    /**
     * Render chunk image for caves in the overworld.
     */
    @Override
    public synchronized boolean render(final ChunkPainter g2D, final ChunkMD chunkMd, final Integer vSlice)
    {
        if (vSlice == null)
        {
            Journeymap.getLogger().warn("ChunkOverworldCaveRenderer is for caves. vSlice can't be null");
            return false;
        }

        updateOptions();
        boolean ok = false;

        // Surface prepass
        if (mapSurfaceAboveCaves)
        {
            if (!chunkMd.getHasNoSky() && surfaceRenderer != null)
            {
                ok = surfaceRenderer.render(g2D, null, chunkMd, vSlice, true);
                if (!ok)
                {
                    Journeymap.getLogger().debug("The surface chunk didn't paint: " + chunkMd.toString());
                }
            }
        }

        renderCaveTimer.start();

        try
        {
            // Init heights if needed
            if (chunkSliceHeights[vSlice] == null)
            {
                chunkSliceHeights[vSlice] = new HeightsCache(String.format("%sHeights_%d", cachePrefix, vSlice));
            }
            else
            {
                chunkSliceHeights[vSlice].invalidateAll();
            }

            // Init slopes within slice
            if (chunkSliceSlopes[vSlice] == null)
            {
                chunkSliceSlopes[vSlice] = new SlopesCache(String.format("%sSlopes_%d", cachePrefix, vSlice));
            }
            else
            {
                chunkSliceSlopes[vSlice].invalidateAll();
            }
            populateSlopes(chunkMd, vSlice, chunkSliceHeights[vSlice], chunkSliceSlopes[vSlice]);

            // Render that lovely cave action
            ok = renderUnderground(g2D, chunkMd, vSlice, chunkSliceHeights[vSlice], chunkSliceSlopes[vSlice]);

            if (!ok)
            {
                Journeymap.getLogger().debug("The underground chunk didn't paint: " + chunkMd.toString());
            }
            return ok;
        }
        finally
        {
            renderCaveTimer.stop();
        }
    }

    /**
     * Render blocks in the chunk for underground.
     */
    protected boolean renderUnderground(final ChunkPainter painter, final ChunkMD chunkMd, final int vSlice, HeightsCache chunkHeights, SlopesCache chunkSlopes)
    {
        final int[] sliceBounds = getVSliceBounds(chunkMd, vSlice);
        final int sliceMinY = sliceBounds[0];
        final int sliceMaxY = sliceBounds[1];

        int y;

        boolean chunkOk = false;

        for (int z = 0; z < 16; z++)
        {
            blockLoop:
            for (int x = 0; x < 16; x++)
            {
                strata.reset();

                try
                {
                    final int ceiling = chunkMd.getHasNoSky() ? sliceMaxY : getSliceBlockHeight(chunkMd, x, vSlice, z, sliceMinY, sliceMaxY, chunkHeights);
                    //final int ceiling = getSliceBlockHeight(chunkMd, x, vSlice, z, sliceMinY, sliceMaxY, chunkHeights);

                    // Oh look, a hole in the world.
                    if (ceiling < 0)
                    {
                        chunkOk = true;
                        painter.paintVoidBlock(x, z);
                        continue;
                    }

                    // Nothing even in this slice.
                    if (ceiling < sliceMinY)
                    {
                        if (surfaceRenderer != null && mapSurfaceAboveCaves)
                        {
                            // Should be painted by surface renderer already.
                            painter.paintDimOverlay(x, z, defaultDim);
                        }
                        else
                        {
                            painter.paintBlackBlock(x, z);
                        }
                        chunkOk = true;
                        continue;
                    }
                    else if (ceiling > sliceMaxY)
                    {
                        // Solid stuff above the slice. Shouldn't be painted by surface renderer.
                        y = sliceMaxY;
                    }
                    else
                    {
                        // Ceiling within slice. Should be painted by by surface renderer... should we dim it?
                        y = ceiling;
                    }

                    buildStrata(strata, sliceMinY, chunkMd, x, y, z, chunkHeights, chunkSlopes);

                    // No lit blocks
                    if (strata.isEmpty())
                    {
                        // No surface?
                        if (surfaceRenderer == null)
                        {
                            if (strata.isBlocksFound())
                            {
                                painter.paintBlackBlock(x, z);
                            }
                            else
                            {
                                painter.paintVoidBlock(x, z);
                            }
                        }
                        else if (ceiling > sliceMaxY)
                        {
                            int distance = ceiling - y;
                            if (distance < 16 && mapSurfaceAboveCaves)
                            {
                                // Show dimmed surface above
                                painter.paintDimOverlay(x, z, Math.max(defaultDim, distance / 16));
                            }
                            else
                            {
                                // Or not.
                                painter.paintBlackBlock(x, z);
                            }
                        }
                        else if (mapSurfaceAboveCaves)
                        {
                            painter.paintDimOverlay(x, z, defaultDim);
                        }

                        chunkOk = true;
                    }
                    else
                    {
                        // Paint that action
                        chunkOk = paintStrata(strata, painter, chunkMd, vSlice, x, ceiling, z, chunkHeights, chunkSlopes) || chunkOk;
                    }

                }
                catch (Throwable t)
                {
                    painter.paintBadBlock(x, vSlice, z);
                    String error = "CaveRenderer error at x,vSlice,z = " + x + "," //$NON-NLS-1$ //$NON-NLS-2$
                            + vSlice + "," + z + " : " + LogFormatter.toString(t); //$NON-NLS-1$ //$NON-NLS-2$
                    Journeymap.getLogger().error(error);
                }
            }
        }
        strata.reset();
        return chunkOk;
    }

    /**
     * Create Strata for caves, using first lit blocks found.
     */
    protected void buildStrata(Strata strata, int minY, ChunkMD chunkMd, int x, final int topY, int z, HeightsCache chunkHeights, SlopesCache chunkSlopes)
    {
        BlockMD blockMD;
        BlockMD blockAboveMD;
        BlockMD lavaBlockMD = null;

        try
        {
            int lightLevel;
            int y = getSliceBlockHeight(chunkMd, x, topY >> 4, z, minY, topY, chunkHeights);

            while (y > 0)
            {
                blockMD = BlockMD.getBlockMD(chunkMd, x, y, z);

                if (!blockMD.isAir())
                {
                    strata.setBlocksFound(true);
                    blockAboveMD = BlockMD.getBlockMD(chunkMd, x, y + 1, z);

                    if (blockMD.isLava() && blockAboveMD.isLava())
                    {
                        // Ignores the myriad tiny one-block pockets of lava in the Nether
                        lavaBlockMD = blockMD;
                    }

                    if (blockAboveMD.isAir() || blockAboveMD.hasFlag(BlockMD.Flag.OpenToSky))
                    {
                        if (chunkMd.getHasNoSky() || !chunkMd.canBlockSeeTheSky(x, y + 1, z))
                        {
                            lightLevel = getSliceLightLevel(chunkMd, x, y, z, true);

                            if (lightLevel > 0)
                            {
                                strata.push(chunkMd, blockMD, x, y, z, lightLevel);
                                if (blockMD.getAlpha() == 1f || !mapTransparency)
                                {
                                    break;
                                }
                            }
                            else if (y < minY)
                            {
                                break;
                            }
                        }
                    }
                    else
                    {
                        break;
                    }
                }
                y--;
            }
        }
        finally
        {
            // Corner case where the column has lava but no air in it.
            // This is a nether thing
            if (chunkMd.getHasNoSky() && strata.isEmpty() && lavaBlockMD != null)
            {
                strata.push(chunkMd, lavaBlockMD, x, topY, z, 14);
            }
        }
    }

    /**
     * Paint the image with the color derived from a BlockStack
     */
    protected boolean paintStrata(final Strata strata, final ChunkPainter painter, final ChunkMD chunkMd, final Integer vSlice, final int x, final int y, final int z, HeightsCache chunkHeights, SlopesCache chunkSlopes)
    {
        if (strata.isEmpty())
        {
            painter.paintBadBlock(x, y, z);
            return false;
        }

        try
        {
            Stratum stratum = null;
            BlockMD blockMD = null;

            while (!strata.isEmpty())
            {
                stratum = strata.nextUp(this, true);

                // Simple surface render
                if (strata.getRenderCaveColor() == null)
                {
                    strata.setRenderCaveColor(stratum.getCaveColor());
                }
                else
                {
                    strata.setRenderCaveColor(RGB.blendWith(strata.getRenderCaveColor(), stratum.getCaveColor(), stratum.getBlockMD().getAlpha()));
                }

                blockMD = stratum.getBlockMD();
                strata.release(stratum);

            } // end color stack

            // Shouldn't happen
            if (strata.getRenderCaveColor() == null)
            {
                painter.paintBadBlock(x, y, z);
                return false;
            }

            // Now add bevel for slope
            if (!(blockMD.hasNoShadow()))
            {
                float slope = getSlope(chunkMd, blockMD, x, vSlice, z, chunkHeights, chunkSlopes);
                if (slope != 1f)
                {
                    strata.setRenderCaveColor(RGB.bevelSlope(strata.getRenderCaveColor(), slope));
                }
            }

            // And draw to the actual chunkimage
            painter.paintBlock(x, z, strata.getRenderCaveColor());
        }
        catch (RuntimeException e)
        {
            painter.paintBadBlock(x, y, z);
            throw e;
        }

        return true;
    }

    /**
     * Get block height within slice.
     */
    @Override
    protected Integer getSliceBlockHeight(final ChunkMD chunkMd, final int x, final Integer vSlice, final int z, final int sliceMinY, final int sliceMaxY,
                                          final HeightsCache chunkHeights)
    {
        Integer[][] blockSliceHeights = chunkHeights.getUnchecked(chunkMd.getCoord());
        if (blockSliceHeights == null)
        {
            return null;
        }

        Integer y = blockSliceHeights[x][z];

        if (y != null)
        {
            return y;
        }

        try
        {
            y = sliceMaxY - 1;

            BlockMD blockMD = BlockMD.getBlockMD(chunkMd, x, y, z);
            BlockMD blockMDAbove = BlockMD.getBlockMD(chunkMd, x, y + 1, z);

            boolean inAirPocket = false;

            while (y > 0 && y > sliceMinY)
            {

                if (mapBathymetry && blockMD.isWater())
                {
                    y--;
                }

                inAirPocket = blockMD.isAir();

                if (blockMDAbove.isAir() || blockMDAbove.hasTranparency() || blockMDAbove.hasFlag(BlockMD.Flag.OpenToSky))
                {
                    if (!blockMD.isAir())
                    {
                        break;
                    }
                }

                y--;

                blockMD = BlockMD.getBlockMD(chunkMd, x, y, z);
                blockMDAbove = BlockMD.getBlockMD(chunkMd, x, y + 1, z);

                if (y < sliceMinY && !inAirPocket)
                {
                    break;
                }
            }
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn("Couldn't get safe slice block height at " + x + "," + z + ": " + e);
            y = sliceMaxY;
        }

        y = Math.max(0, y);

        blockSliceHeights[x][z] = y;
        return y;
    }

    /**
     * Get the light level for the block in the slice.  Can be overridden to provide an ambient light minimum.
     */
    protected int getSliceLightLevel(ChunkMD chunkMd, int x, int y, int z, boolean adjusted)
    {
        return mapCaveLighting ? chunkMd.getSavedLightValue(x, y + 1, z) : 15;
    }

    @Override
    public void onRemoval(RemovalNotification<ChunkCoordIntPair, ChunkMD> notification)
    {
        synchronized (chunkLock)
        {
            ChunkCoordIntPair coord = notification.getKey();
            for (HeightsCache heightsCache : chunkSliceHeights)
            {
                if (heightsCache != null)
                {
                    heightsCache.invalidate(coord);
                }
            }

            for (SlopesCache slopesCache : chunkSliceSlopes)
            {
                if (slopesCache != null)
                {
                    slopesCache.invalidate(coord);
                }
            }

            columnPropertiesCache.invalidate(coord);

            //JourneyMap.getLogger().info("Invalidated data related to chunk " + coord);
        }
    }
}
