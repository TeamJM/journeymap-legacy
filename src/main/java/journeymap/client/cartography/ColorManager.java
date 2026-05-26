/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography;

import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.forge.helper.IColorHelper;
import journeymap.client.forge.helper.IForgeHelper;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.BlockMD;
import journeymap.client.model.BlockSpriteMD;
import journeymap.client.task.multi.MapPlayerTask;
import journeymap.common.Journeymap;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import java.util.Collection;
import java.util.Map;

/**
 * Manages of block colors derived from the current texture pack.
 *
 * @author techbrew
 */
public class ColorManager
{
    private final IForgeHelper forgeHelper = ForgeHelper.INSTANCE;
    private final IColorHelper colorHelper = forgeHelper.getColorHelper();
    private String lastResourcePackNames;
    private String lastModNames;

    public static ColorManager instance()
    {
        return Holder.INSTANCE;
    }

    /**
     * Ensure the colors in the cache match the current resource packs.
     * Must be called on main Minecraft thread in case the blocks texture
     * is stitched.
     */
    public void ensureCurrent()
    {
        try
        {
            if (!Display.isCurrent())
            {
                Journeymap.getLogger().error("ColorManager.ensureCurrent() must be called on main thread!");
            }
        }
        catch (LWJGLException e)
        {
            e.printStackTrace();
            return;
        }

        String currentResourcePackNames = Constants.getResourcePackNames();
        String currentModNames = Constants.getModNames();

        boolean resourcePackSame = false;
        boolean modPackSame = false;

        if (currentResourcePackNames.equals(lastResourcePackNames))
        {
            Journeymap.getLogger().debug("Resource Pack(s) unchanged: {}", currentResourcePackNames);
            resourcePackSame = true;
        }

        if (currentModNames.equals(lastModNames))
        {
            Journeymap.getLogger().debug("Mod Pack(s) unchanged: {}", currentModNames);
            modPackSame = true;
        }

        if (!resourcePackSame || !modPackSame)
        {
            boolean isFirstLoad = lastResourcePackNames == null && lastModNames == null;
            lastResourcePackNames = currentResourcePackNames;
            lastModNames = currentModNames;

            Journeymap.getLogger().info("Loading blocks and textures...");

            boolean isMapping = JourneymapClient.getInstance().isMapping();
            if (isMapping)
            {
                JourneymapClient.getInstance().stopMapping();
            }

            // Reload all BlockMDs and BlockSpriteMD
            BlockMD.reset();
            BlockSpriteMD.reset();

            // Ensure blocks texture initialized
            colorHelper.initBlocksTexture();

            // Init colors
            initBlockColors(!isFirstLoad);

            // Free memory once colors are loaded into BlockMDs and BlockSpriteMD
            colorHelper.clearBlocksTexture();

            if (isMapping)
            {
                JourneymapClient.getInstance().startMapping();
            }
        }
        else
        {
            Journeymap.getLogger().info("Blocks and textures are current");
        }
    }

    /**
     * Load color palette.  Needs to be called on the main thread
     * so the texture atlas can be loaded.
     */
    private static void initBlockColors(boolean currentPaletteInvalid)
    {
        try
        {
            // Start with existing palette colors and set them on the corresponding BlockMDs
            long start1 = System.currentTimeMillis();
            ColorPalette palette = ColorPalette.getActiveColorPalette();
            long elapsed1 = System.currentTimeMillis() - start1;
            if (palette != null)
            {
                Journeymap.getLogger().info("Loaded color palette from file in {}ms: {}", elapsed1, palette.getOrigin());
            }

            boolean standard = true;
            boolean permanent = false;
            if (palette != null)
            {
                standard = palette.isStandard();
                permanent = palette.isPermanent();
                if (currentPaletteInvalid && !permanent)
                {
                    Journeymap.getLogger().info("New color palette will be created");
                    palette = null;
                }
                else
                {
                    try
                    {
                        long start2 = System.currentTimeMillis();
                        for (Map.Entry<BlockMD, Integer> entry : palette.getBasicColorMap().entrySet())
                        {
                            entry.getKey().setColor(entry.getValue());
                        }
                        long elapsed2 = System.currentTimeMillis() - start2;
                        Journeymap.getLogger().info("Loaded {} block colors from color palette in {}ms", palette.size(), elapsed2);
                    }
                    catch (Exception e)
                    {
                        Journeymap.getLogger().warn("Could not load existing color palette, new one will be created: {}", String.valueOf(e));
                        palette = null;
                    }
                }
            }

            // Load textures for the others
            final Collection<BlockMD> allBlocks = BlockMD.getAll();
            int blockCount = 0;
            int spriteCount = 0;
            long start3 = System.currentTimeMillis();
            for (BlockMD blockMD : allBlocks)
            {
                if (blockMD.ensureColor())
                {
                    blockCount++;
                }
            }
            long blocksTime = System.currentTimeMillis() - start3;
            BlockSpriteMD.loadColorsFrom(allBlocks);
            long start4 = System.currentTimeMillis();
            for (BlockSpriteMD spriteMD : BlockSpriteMD.getCached())
            {
                if (spriteMD.ensureColor())
                {
                    spriteCount++;
                }
            }
            long spritesTime = System.currentTimeMillis() - start4;
            Journeymap.getLogger().info("Initialized {} block colors ({}ms) and {} sprite colors ({}ms) from mods and resource packs", blockCount, blocksTime, spriteCount, spritesTime);

            if (blockCount > 0 || palette == null)
            {
                ColorPalette.create(standard, permanent);
            }
            else
            {
                Journeymap.getLogger().info("Color palette was sufficient: {}", palette.getOrigin());
            }

            // Remap around player
            MapPlayerTask.forceNearbyRemap();
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("ColorManager.initBlockColors() encountered an unexpected error: {}", LogFormatter.toPartialString(t));
        }
    }

    /**
     * Singleton
     */
    private static class Holder
    {
        private static final ColorManager INSTANCE = new ColorManager();
    }

}
