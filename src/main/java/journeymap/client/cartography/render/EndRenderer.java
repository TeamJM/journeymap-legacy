/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography.render;


import journeymap.client.cartography.ChunkPainter;
import journeymap.client.cartography.IChunkRenderer;
import journeymap.client.cartography.RGB;
import journeymap.client.cartography.Strata;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;

/**
 * Render a chunk in the End.
 *
 * @author techbrew
 */
public class EndRenderer extends SurfaceRenderer implements IChunkRenderer
{
    private static final int MIN_LIGHT_LEVEL = 2;

    public EndRenderer()
    {
        super("End");
    }

    @Override
    protected void updateOptions()
    {
        super.updateOptions();
        this.ambientColor = RGB.floats(tweakEndAmbientColor);
        this.tweakMoonlightLevel = 5f;
    }

    /**
     * Create Strata.
     */
    @Override
    protected void buildStrata(Strata strata, int minY, ChunkMD chunkMd, int x, final int topY, int z)//, HeightsCache chunkHeights, SlopesCache chunkSlopes)
    {
        super.buildStrata(strata, minY, chunkMd, x, topY, z);//, chunkHeights, chunkSlopes);
    }


    /**
     * Paint the image with the color derived from a BlockStack
     */
    protected boolean paintStrata(final Strata strata, final ChunkPainter dayG2d, final ChunkMD chunkMd, final BlockMD topBlockMd, final Integer vSlice, final int x, final int y, final int z, final boolean cavePrePass)
    {
        return super.paintStrata(strata, dayG2d, null, chunkMd, topBlockMd, vSlice, x, y, z, cavePrePass);
    }

}
