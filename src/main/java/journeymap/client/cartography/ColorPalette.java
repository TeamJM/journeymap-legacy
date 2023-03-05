/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Since;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameData;
import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.io.FileHandler;
import journeymap.client.log.ChatLog;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.BlockMD;
import journeymap.common.Journeymap;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Provides serialization of cache colors to/from file.
 */
public class ColorPalette
{
    public static final String HELP_PAGE = "http://journeymap.info/help/wiki/Color_Palette";
    public static final String SAMPLE_STANDARD_PATH = ".minecraft/journeymap/";
    public static final String SAMPLE_WORLD_PATH = SAMPLE_STANDARD_PATH + "data/*/worldname/";
    public static final String JSON_FILENAME = "colorpalette.json";
    public static final String HTML_FILENAME = "colorpalette.html";
    public static final String VARIABLE = "var colorpalette=";
    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static final int VERSION = 3;
    public static final Gson GSON = new GsonBuilder().setVersion(VERSION).setPrettyPrinting().create();

    @Since(3)
    int version;

    @Since(1)
    String name;

    @Since(1)
    String generated;

    @Since(1)
    String[] description;

    @Since(1)
    boolean permanent;

    @Since(1)
    String resourcePacks;

    @Since(2)
    String modNames;

    @Since(1)
    ArrayList<BlockColor> basicColors = new ArrayList<BlockColor>(0);

    private transient File origin;

    /**
     * Default constructor for GSON.
     */
    ColorPalette()
    {
    }

    /**
     * Constructor invoked by static create() method/
     * @param resourcePacks
     * @param modNames
     * @param basicColorMap
     */
    private ColorPalette(String resourcePacks, String modNames, HashMap<BlockMD, Integer> basicColorMap)
    {
        this.version = VERSION;
        this.name = Constants.getString("jm.colorpalette.file_title");
        this.generated = String.format("Generated using %s for %s on %s", JourneymapClient.MOD_NAME, Loader.MC_VERSION, new Date());
        this.resourcePacks = resourcePacks;
        this.modNames = modNames;

        ArrayList<String> lines = new ArrayList<String>();
        lines.add(Constants.getString("jm.colorpalette.file_header_1"));
        lines.add(Constants.getString("jm.colorpalette.file_header_2", HTML_FILENAME));
        lines.add(Constants.getString("jm.colorpalette.file_header_3", JSON_FILENAME, SAMPLE_WORLD_PATH));
        lines.add(Constants.getString("jm.colorpalette.file_header_4", JSON_FILENAME, SAMPLE_STANDARD_PATH));
        lines.add(Constants.getString("jm.config.file_header_5", HELP_PAGE));
        this.description = lines.toArray(new String[4]);

        this.basicColors = toList(basicColorMap);
    }

    /**
     * Returns the active pallete.
     * @return
     */
    public static ColorPalette getActiveColorPalette()
    {
        String resourcePacks = Constants.getResourcePackNames();
        String modNames = Constants.getModNames();

        File worldPaletteFile = ColorPalette.getWorldPaletteFile();
        if (worldPaletteFile.canRead())
        {
            ColorPalette palette = ColorPalette.loadFromFile(worldPaletteFile);
            if (palette != null)
            {
                if (palette.version < VERSION)
                {
                    Journeymap.getLogger().warn(String.format("Existing world color palette is obsolete. Required version: %s.  Found version: %s", VERSION, palette.version));
                }
                else
                {
                    return palette;
                }
            }
        }

        File standardPaletteFile = ColorPalette.getStandardPaletteFile();
        if (standardPaletteFile.canRead())
        {
            ColorPalette palette = ColorPalette.loadFromFile(standardPaletteFile);
            if (palette != null && palette.version < VERSION)
            {
                Journeymap.getLogger().warn(String.format("Existing color palette is obsolete. Required version: %s.  Found version: %s", VERSION, palette.version));
                palette = null;
            }

            if (palette != null)
            {
                if (palette.isPermanent())
                {
                    Journeymap.getLogger().info("Existing color palette is set to be permanent.");
                    return palette;
                }

                if (resourcePacks.equals(palette.resourcePacks))
                {
                    if (modNames.equals(palette.modNames))
                    {
                        Journeymap.getLogger().info("Existing color palette's resource packs and mod names match current loadout.");
                        return palette;
                    }
                    else
                    {
                        Journeymap.getLogger().warn("Existing color palette's mods no longer match current loadout.");
                        Journeymap.getLogger().info(String.format("WAS: %s\nNOW: %s", palette.modNames, modNames));
                    }
                }
                else
                {
                    Journeymap.getLogger().warn("Existing color palette's resource packs no longer match current loadout.");
                    Journeymap.getLogger().info(String.format("WAS: %s\nNOW: %s", palette.resourcePacks, resourcePacks));
                }
            }
        }

        return null;
    }

    /**
     * Create a color palette based on current block colors and write it to file.
     */
    public static ColorPalette create(boolean standard, boolean permanent)
    {
        long start = System.currentTimeMillis();

        ColorPalette palette = null;
        try
        {
            String resourcePackNames = Constants.getResourcePackNames();
            String modPackNames = Constants.getModNames();

            HashMap<BlockMD, Integer> baseColors = new HashMap<BlockMD, Integer>();
            for (BlockMD blockMD : BlockMD.getAll())
            {
                Integer baseColor = blockMD.getColor();
                if (baseColor != null)
                {
                    baseColors.put(blockMD, baseColor);
                }
            }

            palette = new ColorPalette(resourcePackNames, modPackNames, baseColors);
            palette.setPermanent(permanent);
            palette.writeToFile(standard);
            long elapsed = System.currentTimeMillis() - start;
            Journeymap.getLogger().info(String.format("Color palette file generated with %d colors in %dms for: %s", palette.size(), elapsed, palette.getOrigin()));
            return palette;
        }
        catch (Exception e)
        {
            Journeymap.getLogger().error("Couldn't create ColorPalette: " + LogFormatter.toString(e));
        }
        return null;
    }

    private static File getWorldPaletteFile()
    {
        Minecraft mc = ForgeHelper.INSTANCE.getClient();
        return new File(FileHandler.getJMWorldDir(mc), JSON_FILENAME);
    }

    private static File getStandardPaletteFile()
    {
        return new File(FileHandler.getJourneyMapDir(), JSON_FILENAME);
    }

    private static ColorPalette loadFromFile(File file)
    {
        String jsonString = null;
        try
        {
            jsonString = Files.toString(file, UTF8).replaceFirst(VARIABLE, "");
            ColorPalette palette = GSON.fromJson(jsonString, ColorPalette.class);
            palette.origin = file;

            // Ensure current HTML file accompanies the data
            palette.getOriginHtml(true, true);
            return palette;
        }
        catch (Throwable e)
        {
            ChatLog.announceError(Constants.getString("jm.colorpalette.file_error", file.getPath()));
            try
            {
                file.renameTo(new File(file.getParentFile(), file.getName() + ".bad"));
            }
            catch (Exception e2)
            {
                Journeymap.getLogger().error("Couldn't rename bad palette file: " + e2);
            }
            return null;
        }
    }

    private String substituteValueInContents(String contents, String key, Object... params)
    {
        String token = String.format("\\$%s\\$", key);
        return contents.replaceAll(token, Matcher.quoteReplacement(Constants.getString(key, params)));
    }

    private ArrayList<BlockColor> toList(HashMap<BlockMD, Integer> map)
    {
        ArrayList<BlockColor> list = new ArrayList<BlockColor>(map.size());
        for (Map.Entry<BlockMD, Integer> entry : map.entrySet())
        {
            BlockMD blockMD = entry.getKey();
            Integer color = entry.getValue();
            if (blockMD == null || color == null)
            {
                continue;
            }
            if (blockMD.hasFlag(BlockMD.Flag.Error))
            {
                Journeymap.getLogger().warn("Block with Error flag won't be saved to color palette: " + entry.getKey());
            }
            else
            {
                list.add(new BlockColor(blockMD, color));
            }
        }
        Collections.sort(list);
        return list;
    }

    private boolean writeToFile(boolean standard)
    {
        File palleteFile = null;
        try
        {
            // Write JSON
            palleteFile = standard ? getStandardPaletteFile() : getWorldPaletteFile();
            Files.write(VARIABLE + GSON.toJson(this), palleteFile, UTF8);
            this.origin = palleteFile;

            // Write HTML
            getOriginHtml(true, true);
            return true;
        }
        catch (Exception e)
        {
            Journeymap.getLogger().error(String.format("Can't save color pallete file %s: %s", palleteFile, LogFormatter.toString(e)));
            return false;
        }
    }


    private HashMap<BlockMD, Integer> listToMap(ArrayList<BlockColor> list)
    {
        HashMap<BlockMD, Integer> map = new HashMap<BlockMD, Integer>(list.size());
        for (BlockColor blockColor : list)
        {
            // 1.7.10 and 1.8
            Block block = GameData.getBlockRegistry().getObject(blockColor.uid);

            // 1.8.8
            //Block block = GameData.getBlockRegistry().getObject(new ResourceLocation(blockColor.uid));
            if (block == null)
            {
                Journeymap.getLogger().warn("Block referenced in Color Palette is not registered: " + blockColor.uid);
                continue;
            }
            BlockMD blockMD = BlockMD.get(block, blockColor.meta);
            if (blockMD.hasFlag(BlockMD.Flag.Transparency))
            {
                Float alpha = blockColor.alpha;
                blockMD.setAlpha((alpha != null) ? alpha : 1f);
            }
            int color = RGB.ALPHA_OPAQUE | Integer.parseInt(blockColor.color.replaceFirst("#", ""), 16);
            map.put(blockMD, color);
        }
        return map;
    }

    public HashMap<BlockMD, Integer> getBasicColorMap()
    {
        return listToMap(this.basicColors);
    }

    public File getOrigin() throws IOException
    {
        return origin.getCanonicalFile();
    }

    public File getOriginHtml(boolean createIfMissing, boolean overwriteExisting)
    {
        try
        {
            if (origin == null)
            {
                return null;
            }

            File htmlFile = new File(origin.getParentFile(), HTML_FILENAME);
            if ((!htmlFile.exists() && createIfMissing) || overwriteExisting)
            {
                // Copy HTML file
                htmlFile = FileHandler.copyColorPaletteHtmlFile(origin.getParentFile(), HTML_FILENAME);
                String htmlString = Files.toString(htmlFile, UTF8);

                // Substitutions in HTML file
                htmlString = substituteValueInContents(htmlString, "jm.colorpalette.file_title");
                htmlString = substituteValueInContents(htmlString, "jm.colorpalette.file_missing_data", JSON_FILENAME);
                htmlString = substituteValueInContents(htmlString, "jm.colorpalette.resource_packs");
                htmlString = substituteValueInContents(htmlString, "jm.colorpalette.mods");
                htmlString = substituteValueInContents(htmlString, "jm.colorpalette.basic_colors");
                htmlString = substituteValueInContents(htmlString, "jm.colorpalette.biome_colors");
                Files.write(htmlString, htmlFile, UTF8);

            }
            return htmlFile;
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Can't get colorpalette.html: " + t);
        }
        return null;
    }

    public boolean isPermanent()
    {
        return permanent;
    }

    public void setPermanent(boolean permanent)
    {
        this.permanent = permanent;
    }

    public boolean isStandard()
    {
        return origin != null && origin.getParentFile().getAbsoluteFile().equals(FileHandler.getJourneyMapDir().getAbsoluteFile());
    }

    public int size()
    {
        return basicColors.size();
    }

    @Override
    public String toString()
    {
        return "ColorPalette[" + resourcePacks + "]";
    }


    class BlockColor implements Comparable<BlockColor>
    {
        @Since(1)
        String name;

        @Since(1)
        String uid;

        @Since(1)
        int meta;

        @Since(1)
        String color;

        @Since(1)
        Float alpha;

        BlockColor(BlockMD blockMD, Integer intColor)
        {
            this.name = blockMD.getName();
            // 1.8 needs the cast
            this.uid = GameData.getBlockRegistry().getNameForObject(blockMD.getBlock()).toString();
            this.meta = blockMD.getMeta();

            Color awtColor = new Color(intColor);
            this.color = String.format("#%02x%02x%02x", awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
            if (blockMD.getAlpha() < 1f)
            {
                this.alpha = blockMD.getAlpha();
            }
        }

        @Override
        public int compareTo(BlockColor o)
        {
            int result = this.name.compareTo(o.name);
            if (result != 0)
            {
                return result;
            }
            result = this.uid.compareTo(o.uid);
            if (result != 0)
            {
                return result;
            }
            return Integer.compare(this.meta, o.meta);
        }
    }
}
