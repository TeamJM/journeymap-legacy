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
import journeymap.client.task.multi.MapPlayerTask;
import journeymap.common.Journeymap;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import java.util.Map;

/**
 * Manages of block colors derived from the current texture pack.
 *
 * @author techbrew
 */
public class ColorManager
{
    private final IForgeHelper forgeHelper = ForgeHelper.INSTANCE;
    private volatile IColorHelper colorHelper = forgeHelper.getColorHelper();
    private volatile ColorPalette currentPalette;
    private String lastResourcePackNames;
    private String lastModNames;

    public static ColorManager instance()
    {
        return Holder.INSTANCE;
    }

    /**
     * Ensure the colors in the cache match the current resource packs.
     * Must be called on main Minecraft thread in case the blocks texture
     * is stiched.
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
        boolean blocksTextureChanged = false;

        if (currentResourcePackNames.equals(lastResourcePackNames) && colorHelper != null)
        {
            Journeymap.getLogger().debug("Resource Pack(s) unchanged: " + currentResourcePackNames);
            resourcePackSame = true;
        }

        if (currentModNames.equals(lastModNames))
        {
            Journeymap.getLogger().debug("Mod Pack(s) unchanged: " + currentModNames);
            modPackSame = true;
        }

        if (!resourcePackSame || !modPackSame)
        {
            lastResourcePackNames = currentResourcePackNames;
            lastModNames = currentModNames;
            blocksTextureChanged = colorHelper.clearBlocksTexture();
        }

        if (!colorHelper.hasBlocksTexture())
        {
            Journeymap.getLogger().info("Loading blocks and textures...");

            boolean isMapping = JourneymapClient.getInstance().isMapping();
            if(isMapping)
            {
                JourneymapClient.getInstance().stopMapping();
            }

            // Reload all BlockMDs
            BlockMD.reset();

            // Ensure blocks texture initialized
            colorHelper.initBlocksTexture();

            // Init colors
            initBlockColors(blocksTextureChanged);

            if(isMapping)
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
     * Get the current palette.
     *
     * @return
     */
    public ColorPalette getCurrentPalette()
    {
        return currentPalette;
    }

    /**
     * Load color palette.  Needs to be called on the main thread
     * so the texture atlas can be loaded.
     */
    private void initBlockColors(boolean currentPaletteInvalid)
    {
        try
        {
            // Start with existing palette colors and set them on the corresponding BlockMDs
            ColorPalette palette = ColorPalette.getActiveColorPalette();
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
                        long start = System.currentTimeMillis();
                        for (Map.Entry<BlockMD, Integer> entry : palette.getBasicColorMap().entrySet())
                        {
                            entry.getKey().setColor(entry.getValue());
                        }
                        long elapsed = System.currentTimeMillis() - start;
                        Journeymap.getLogger().info(String.format("Loaded %d block colors from color palette file in %dms: %s", palette.size(), elapsed, palette.getOrigin()));
                    }
                    catch (Exception e)
                    {
                        Journeymap.getLogger().warn("Could not load existing color palette, new one will be created: " + e);
                        palette = null;
                    }
                }
            }

            // Load textures for the others
            long start = System.currentTimeMillis();
            int count = 0;
            for (BlockMD blockMD : BlockMD.getAll())
            {
                if (blockMD.ensureColor())
                {
                    count++;
                }
            }
            long elapsed = System.currentTimeMillis() - start;

            if (count > 0 || palette == null)
            {
                Journeymap.getLogger().info(String.format("Initialized %s block colors from mods and resource packs in %sms", count, elapsed));
                this.currentPalette = ColorPalette.create(standard, permanent);
                Journeymap.getLogger().info(String.format("Updated color palette file: %s", this.currentPalette.getOrigin()));
            }
            else
            {
                this.currentPalette = palette;
                Journeymap.getLogger().info(String.format("Color palette was sufficient: %s", this.currentPalette.getOrigin()));
            }

            // Remap around player
            MapPlayerTask.forceNearbyRemap();
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("ColorManager.initBlockColors() encountered an unexpected error: " + LogFormatter.toPartialString(t));
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
