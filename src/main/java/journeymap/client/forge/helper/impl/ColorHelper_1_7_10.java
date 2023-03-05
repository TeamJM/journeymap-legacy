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
    private static volatile BufferedImage blocksTexture;
    Logger logger = Journeymap.getLogger();
    HashSet<BlockMD> failed = new HashSet<BlockMD>();

    public ColorHelper_1_7_10()
    {
    }

    @Override
    public boolean hasBlocksTexture()
    {
        return blocksTexture != null;
    }

    @Override
    public boolean clearBlocksTexture()
    {
        if(blocksTexture==null)
        {
            return false;
        }
        blocksTexture = null;
        return true;
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

        Integer color = null;

        boolean ok = blocksTexture != null || initBlocksTexture();
        if (!ok)
        {
            logger.warn("BlocksTexture not yet loaded");
            return null;
        }

//        if (failed.contains(blockMD))
//        {
//            return null;
//        }

        try
        {

            TextureAtlasSprite blockIcon = getDirectIcon(blockMD);

            if (blockIcon == null)
            {
                if (blockMD.getBlock() instanceof ITileEntityProvider)
                {
                    logger.debug("Ignoring TitleEntity without standard block texture: " + blockMD);
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
                logger.warn("Error getting block color. Plese report this exception to the JourneyMap mod author regarding " + blockMD.getUid() + ": " + LogFormatter.toPartialString(t));
            }
            else
            {
                logger.warn("Error getting block color from mod. Plese report this exception to the mod author of " + blockMD.getUid() + ": " + LogFormatter.toPartialString(t));
            }

            return null;
        }
    }

    private TextureAtlasSprite getDirectIcon(BlockMD blockMD)
    {
        boolean ok = blocksTexture != null || initBlocksTexture();
        if (!ok)
        {
            logger.warn("BlocksTexture not yet loaded");
            return null;
        }

        Block block = blockMD.getBlock();
        Integer overrideMeta = null;
        if (blockMD.hasOverrideMeta())
        {
            overrideMeta = blockMD.getOverrideMeta();
        }
        int meta = overrideMeta != null ? overrideMeta : blockMD.getMeta();

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
                    logger.warn("Error getting BlockDoublePlant icon. Plese report this exception to the JourneyMap mod author regarding "
                            + blockMD.getUid() + ": " + LogFormatter.toPartialString(e));
                }
                else
                {
                    logger.warn("Error getting BlockDoublePlant icon from mod. Plese report this exception to the mod author of "
                            + blockMD.getUid() + ": " + LogFormatter.toPartialString(e));
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

    Integer getColorForIcon(BlockMD blockMD, TextureAtlasSprite icon)
    {
        boolean ok = blocksTexture != null || initBlocksTexture();
        if (!ok)
        {
            logger.warn("BlocksTexture not yet loaded");
            return null;
        }

        Integer color = null;

        try
        {
            int count = 0;
            int argb, alpha;
            int a = 0, r = 0, g = 0, b = 0;
            int x = 0, y = 0;

            int xStart, yStart, xStop, yStop;
            if(icon.getIconWidth() + icon.getOriginX() > blocksTexture.getWidth() || icon.getIconHeight() + icon.getOriginY() > blocksTexture.getHeight())
            {
                logger.warn("Couldn't get texture for " + icon.getIconName() + " because of an error matching it within the stitched blocks atlas.");
                return null;
            }
            BufferedImage textureImg = blocksTexture.getSubimage(icon.getOriginX(), icon.getOriginY(), icon.getIconWidth(), icon.getIconHeight());
            xStart = yStart = 0;
            xStop = textureImg.getWidth();
            yStop = textureImg.getHeight();

            boolean unusable = true;
            if (textureImg != null && textureImg.getWidth() > 0)
            {
                outer:
                for (x = xStart; x < xStop; x++)
                {
                    inner:
                    for (y = yStart; y < yStop; y++)
                    {
                        try
                        {
                            argb = textureImg.getRGB(x, y);
                        }
                        catch (ArrayIndexOutOfBoundsException e)
                        {
                            logger.warn("Bad index at " + x + "," + y + " for " + blockMD + ": " + e.getMessage());
                            continue; // Bugfix for some texturepacks that may not be reporting correct size?
                        }
                        catch (Throwable e)
                        {
                            logger.warn("Couldn't get RGB from BlocksTexture at " + x + "," + y + " for " + blockMD + ": " + e.getMessage());
                            break outer;
                        }
                        alpha = (argb >> 24) & 0xFF;
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
                    unusable = false;
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
            }
            else
            {
                logger.warn("Couldn't get texture for " + icon.getIconName() + " using blockid ");
            }

            if (unusable)
            {
                blockMD.addFlags(BlockMD.Flag.Error);
                String pattern = "Unusable texture for %s, icon=%s,texture coords %s,%s - %s,%s";
                logger.debug(String.format(pattern, blockMD, icon.getIconName(), xStart, yStart, xStop, yStop));
                r = g = b = 0;
            }


            // Set color
            color = RGB.toInteger(r, g, b);

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
                    if (block.isTranslucent())
                    { // try to use light opacity
                        blockAlpha = block.getLightOpacity() / 255f;
                    }

                    // try to use texture alpha
                    if (blockAlpha == 0 || blockAlpha == 1)
                    {
                        blockAlpha = a * 1.0f / 255;
                    }
                }
            }
            //dataCache.getBlockMetadata().setAllAlpha(block, blockAlpha);
            blockMD.setAlpha(blockAlpha);
            blockMD.setIconName(icon.getIconName());
        }
        catch (Throwable e1)
        {
            logger.warn("Error deriving color for " + blockMD + ": " + LogFormatter.toString(e1));
        }

        if (color != null)
        {
            if (logger.isTraceEnabled())
            {
                logger.debug("Derived color for " + blockMD + ": " + Integer.toHexString(color));
            }
        }

        return color;
    }

    @Override
    public boolean initBlocksTexture()
    {
        StatTimer timer = StatTimer.get("ColorHelper.initBlocksTexture", 0);

        try
        {
            if (!Display.isCurrent())
            {
                return false;
            }
            blocksTexture = null;
            timer.start();

            int blocksTexId = ForgeHelper.INSTANCE.getClient().getTextureMapBlocks().getGlTextureId();
            ForgeHelper.INSTANCE.getRenderHelper().glBindTexture(blocksTexId);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

            int miplevel = 0;
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, miplevel, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, miplevel, GL11.GL_TEXTURE_HEIGHT);
            IntBuffer intbuffer = BufferUtils.createIntBuffer(width * height);
            int[] aint = new int[width * height];
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, miplevel, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, intbuffer);
            intbuffer.get(aint);
            BufferedImage bufferedimage = new BufferedImage(width, height, 2);
            bufferedimage.setRGB(0, 0, width, height, aint, 0, width);
            blocksTexture = bufferedimage;

            double time = timer.stop();
            Journeymap.getLogger().info(String.format("initBlocksTexture: %sx%s loaded in %sms", width, height, time));

            return true;
        }
        catch (Throwable t)
        {
            logger.error("Could not load blocksTexture :" + t);
            timer.cancel();
            return false;
        }
    }

    /**
     * Facade to expose IIcon as a TextureAtlasSprite.
     */
    class TempTextureAtlasSprite extends TextureAtlasSprite
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
}
