/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.helper;

import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import net.minecraft.block.Block;

/**
 * Interface used to encapsulate compile-time differences between Minecraft/Forge versions
 * with respect to deriving block colors.
 */
public interface IColorHelper
{
    boolean clearBlocksTexture();

    boolean hasBlocksTexture();

    boolean initBlocksTexture();

    boolean failedFor(BlockMD blockMD);

    Integer getTextureColor(BlockMD blockMD);

    int getColorMultiplier(ChunkMD chunkMD, Block block, int x, int y, int z);

    int getRenderColor(BlockMD blockMD);

    int getMapColor(BlockMD blockMD);
}
