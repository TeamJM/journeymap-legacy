/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.helper.impl;

import journeymap.client.cartography.RGB;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.forge.helper.IColorHelper;
import journeymap.client.log.LogFormatter;
import journeymap.client.log.StatTimer;
import journeymap.client.model.BlockMD;
import journeymap.client.model.BlockSpriteMD;
import journeymap.client.model.ChunkMD;
import journeymap.common.Journeymap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.IIcon;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.HashSet;

/**
 * IColorHelper implementation for 1.7.10.   Formerly IconLoader.
 */
public class ColorHelper_1_7_10 implements IColorHelper
{
    private static volatile ArgbImage blocksTexture;
    private final Logger logger = Journeymap.getLogger();
    private final HashSet<BlockMD> failed = new HashSet<>();

    public ColorHelper_1_7_10()
    {
    }

    @Override
    public void clearBlocksTexture()
    {
        if (blocksTexture == null)
        {
            return;
        }
        blocksTexture = null;
    }

    @Override
    public boolean failedFor(BlockMD blockMD)
    {
        return failed.contains(blockMD);
    }

    @Override
    public int getColorMultiplier(ChunkMD chunkMD, Block block, int x, int y, int z)
    {
        return block.colorMultiplier(ForgeHelper.INSTANCE.getIBlockAccess(), x, y, z);
    }

    @Deprecated
    @Override
    public int getRenderColor(BlockMD blockMD)
    {
        return blockMD.getBlock().getRenderColor(blockMD.getMeta());
    }

    @Override
    public int getMapColor(BlockMD blockMD)
    {
        MapColor mapColor = blockMD.getBlock().getMapColor(blockMD.getMeta());
        if (mapColor != null)
        {
            return mapColor.colorValue;
        }
        else
        {
            return RGB.BLACK_RGB;
        }
    }

    @Override
    public Integer getTextureColor(BlockMD blockMD)
    {

//        if (failed.contains(blockMD))
//        {
//            return null;
//        }

        try
        {
            final Integer color;
            final TextureAtlasSprite blockIcon = getDirectIcon(blockMD);

            if (blockIcon == null)
            {
                if (blockMD.getBlock() instanceof ITileEntityProvider)
                {
                    logger.debug("Ignoring TitleEntity without standard block texture: {}", blockMD);
                    blockMD.addFlags(BlockMD.Flag.TileEntity, BlockMD.Flag.HasAir);
                    return null;
                }
            }

            if (blockIcon == null)
            {
                blockMD.addFlags(BlockMD.Flag.Error);
                failed.add(blockMD);
                return null;
            }

            color = getColorForIcon(blockMD, blockIcon);
            if (color == null)
            {
                failed.add(blockMD);
                return null;
            }

            return color;
        }
        catch (Throwable t)
        {
            failed.add(blockMD);
            if (blockMD.getUid().modId.equals("minecraft"))
            {
                logger.warn("Error getting block color. Please report this exception to the JourneyMap mod author regarding {}: {}", blockMD.getUid(), LogFormatter.toPartialString(t));
            }
            else
            {
                logger.warn("Error getting block color from mod. Please report this exception to the mod author of {}: {}", blockMD.getUid(), LogFormatter.toPartialString(t));
            }

            return null;
        }
    }

    @Override
    public BlockSpriteMD getSprite(BlockMD blockMD)
    {
        return BlockSpriteMD.get(this.getDirectIcon(blockMD));
    }

    private TextureAtlasSprite getDirectIcon(BlockMD blockMD)
    {

        final Block block = blockMD.getBlock();
        final int meta = blockMD.hasOverrideMeta() ? blockMD.getOverrideMeta() : blockMD.getMeta();

        IIcon icon = null;

        // TODO: Verify this works after pulling in 1.8 refactoring
        // Always get the upper portion of a double plant for rendering
        if (block instanceof BlockDoublePlant)
        {
            try
            {
                icon = ((BlockDoublePlant) block).func_149888_a(true, meta & 7);
            }
            catch (Exception e)
            {
                if (blockMD.getUid().modId.equals("minecraft"))
                {
                    logger.warn("Error getting BlockDoublePlant icon. Please report this exception to the JourneyMap mod author regarding {}: {}", blockMD.getUid(), LogFormatter.toPartialString(e));
                }
                else
                {
                    logger.warn("Error getting BlockDoublePlant icon from mod. Please report this exception to the mod author of {}: {}", blockMD.getUid(), LogFormatter.toPartialString(e));
                }
            }
        }
        else
        {
            icon = block.getIcon(blockMD.getTextureSide(), meta);
        }

        if (icon != null)
        {
            if (icon instanceof TextureAtlasSprite)
            {
                return (TextureAtlasSprite) icon;
            }
            else
            {
                return new TempTextureAtlasSprite(icon);
            }
        }
        return null;

    }

    @Override
    public Integer getSpriteColor(BlockSpriteMD spriteMD)
    {
        final ArgbImage blockAtlas = blocksTexture;

        if (blockAtlas == null)
        {
            logger.warn("BlocksTexture not yet loaded");
            return null;
        }

        try
        {

            if (!blockAtlas.isSubImageWithinImage(spriteMD.x, spriteMD.y, spriteMD.width, spriteMD.height))
            {
                logger.warn("Couldn't get texture for {} because of an error matching it within the stitched blocks atlas.", spriteMD);
                return null;
            }

            if (spriteMD.width > 0)
            {
                return blockAtlas.getColorOfSubImage(spriteMD.x, spriteMD.y, spriteMD.width, spriteMD.height);
            }
            else
            {
                logger.warn("Couldn't get texture for {}", spriteMD);
            }

        }
        catch (Throwable e1)
        {
            logger.warn("Error deriving color for {}: {}", spriteMD, LogFormatter.toString(e1));
        }

        return null;
    }

    private Integer getColorForIcon(BlockMD blockMD, TextureAtlasSprite icon)
    {

        try
        {

            final ArgbImage blockAtlas = blocksTexture;
            int color = 0;

            if (blockAtlas == null)
            {
                final BlockSpriteMD spriteMD = BlockSpriteMD.get(icon);
                if (spriteMD.hasColor())
                {
                    color = spriteMD.getColor();
                }
                else
                {
                    logger.warn("Could not retrieve color from BlockSpriteMD cache for block {}", blockMD);
                    return null;
                }
            }
            else
            {
                if (!blockAtlas.isSubImageWithinImage(icon.getOriginX(), icon.getOriginY(), icon.getIconWidth(), icon.getIconHeight()))
                {
                    logger.warn("Couldn't get texture for {} because of an error matching it within the stitched blocks atlas.", icon.getIconName());
                    return null;
                }

                if (icon.getIconWidth() > 0)
                {
                    color = blockAtlas.getColorOfSubImage(icon.getOriginX(), icon.getOriginY(), icon.getIconWidth(), icon.getIconHeight());
                }
                else
                {
                    logger.warn("Couldn't get texture for {}, {}", blockMD, icon);
                }
            }

            final boolean unusable = color == 0;

            if (unusable)
            {
                blockMD.addFlags(BlockMD.Flag.Error);
                logger.debug("Unusable texture for {}, {}", blockMD, icon);
            }

            // extract alpha and set to the max for the color
            final int alpha = (color >> 24) & 0xFF;
            color = color | 0xFF000000;

            // Determine alpha
            Block block = blockMD.getBlock();
            float blockAlpha = 1f;
            if (unusable)
            {
                blockAlpha = 0f;
            }
            else
            {
                if (blockMD.hasFlag(BlockMD.Flag.Transparency))
                {
                    blockAlpha = blockMD.getAlpha();
                }
                else if (block.getRenderType() > 0)
                {
                    // 1.8 check translucent because lava's opacity = 0;
                    if (block.getCanBlockGrass())
                    { // try to use light opacity
                        blockAlpha = block.getLightOpacity() / 255f;
                    }

                    // try to use texture alpha
                    if (blockAlpha == 0 || blockAlpha == 1)
                    {
                        blockAlpha = alpha * 1.0f / 255;
                    }
                }
            }
            //dataCache.getBlockMetadata().setAllAlpha(block, blockAlpha);
            blockMD.setAlpha(blockAlpha);
            blockMD.setIconName(icon.getIconName());

            if (logger.isTraceEnabled())
            {
                logger.debug("Derived color for {}: {}", blockMD, Integer.toHexString(color));
            }

            return color;

        }
        catch (Throwable e1)
        {
            logger.warn("Error deriving color for {}: {}", blockMD, LogFormatter.toString(e1));
        }

        return null;
    }

    @Override
    public void initBlocksTexture()
    {
        StatTimer timer = StatTimer.get("ColorHelper.initBlocksTexture", 0);

        try
        {
            if (!Display.isCurrent())
            {
                return;
            }
            blocksTexture = null;
            timer.start();

            int blocksTexId = ForgeHelper.INSTANCE.getClient().getTextureMapBlocks().getGlTextureId();
            ForgeHelper.INSTANCE.getRenderHelper().glBindTexture(blocksTexId);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

            final int miplevel = 0;
            final int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, miplevel, GL11.GL_TEXTURE_WIDTH);
            final int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, miplevel, GL11.GL_TEXTURE_HEIGHT);
            final IntBuffer intbuffer = BufferUtils.createIntBuffer(width * height);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, miplevel, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, intbuffer);
            final int[] pixels = new int[width * height];
            intbuffer.get(pixels);

            blocksTexture = new ArgbImage(pixels, width, height);

            double time = timer.stop();
            logger.info("Created block atlas copy {}x{} in {}ms", width, height, time);
        }
        catch (Throwable t)
        {
            logger.error("Could not load block atlas copy :{}", String.valueOf(t));
            timer.cancel();
        }
    }

    /**
     * Facade to expose IIcon as a TextureAtlasSprite.
     */
    public static class TempTextureAtlasSprite extends TextureAtlasSprite
    {
        IIcon wrapped;

        protected TempTextureAtlasSprite(IIcon icon)
        {
            super(icon.getIconName());
            this.wrapped = icon;
        }

        @Override
        public int getOriginX()
        {
            return Math.round(((float) wrapped.getIconWidth()) * Math.min(wrapped.getMinU(), wrapped.getMaxU()));
        }

        @Override
        public int getOriginY()
        {
            return Math.round(((float) wrapped.getIconHeight()) * Math.min(wrapped.getMinV(), wrapped.getMaxV()));
        }

        @Override
        public int getIconWidth()
        {
            return wrapped.getIconWidth();
        }

        @Override
        public int getIconHeight()
        {
            return wrapped.getIconHeight();
        }
    }

    /**
     * Minimal ARGB image implementation for fast pixel access.
     * Avoids overhead of {@link BufferedImage#setRGB} since we store and use ARGB pixels only.
     */
    private static final class ArgbImage
    {
        public final int[] pixels;
        public final int width;
        public final int height;

        public ArgbImage(int[] pixels, int width, int height)
        {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
        }

        public boolean isSubImageWithinImage(int x, int y, int w, int h)
        {
            return x >= 0 && y >= 0 && w >= 0 && h >= 0 && x + w <= width && y + h <= height;
        }

        public int getColorOfSubImage(int x, int y, int w, int h)
        {
            final int off = y * width + x;
            int count = 0;
            int a = 0, r = 0, g = 0, b = 0;
            for (int i = 0; i < w; i++)
            {
                for (int j = 0; j < h; j++)
                {
                    final int argb = pixels[off + j * width + i];
                    final int alpha = (argb >> 24) & 0xFF;
                    if (alpha > 0)
                    {
                        count++;
                        a += alpha;
                        r += (argb >> 16) & 0xFF;
                        g += (argb >> 8) & 0xFF;
                        b += (argb) & 0xFF;
                    }
                }
            }

            if (count > 0)
            {
                if (a > 0)
                {
                    a = a / count;
                }
                if (r > 0)
                {
                    r = r / count;
                }
                if (g > 0)
                {
                    g = g / count;
                }
                if (b > 0)
                {
                    b = b / count;
                }
            }
            return RGB.toInteger(a, r, g, b);
        }
    }
}
