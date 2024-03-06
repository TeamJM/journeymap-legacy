/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography.render;


import com.google.common.cache.*;
import journeymap.client.JourneymapClient;
import journeymap.client.cartography.IChunkRenderer;
import journeymap.client.cartography.MutableChunkCoordIntPair;
import journeymap.client.cartography.RGB;
import journeymap.client.cartography.Stratum;
import journeymap.client.data.DataCache;
import journeymap.client.model.BlockCoordIntPair;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.client.properties.CoreProperties;
import journeymap.common.Journeymap;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCoordIntPair;
import org.apache.commons.lang3.NotImplementedException;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Base class for methods reusable across renderers.
 *
 * @author techbrew
 */
public abstract class BaseRenderer implements IChunkRenderer, RemovalListener<ChunkCoordIntPair, ChunkMD>
{
    public static final String PROP_WATER_HEIGHT = "waterHeight";
    private static final MutableChunkCoordIntPair coordinates = new MutableChunkCoordIntPair(0, 0);
    protected static final AlphaComposite ALPHA_OPAQUE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1F);
    protected static final int COLOR_BLACK = Color.black.getRGB();
    protected static final int COLOR_VOID = RGB.toInteger(17, 12, 25);
    protected static final float[] DEFAULT_FOG = new float[]{0, 0, .1f};
    protected final DataCache dataCache = DataCache.instance();
    protected CoreProperties coreProperties;
    protected BlockColumnPropertiesCache columnPropertiesCache = null;
    // Updated in updateOptions()
    protected boolean mapBathymetry;
    protected boolean mapTransparency;
    protected boolean mapCaveLighting;
    protected boolean mapAntialiasing;
    protected boolean mapCrops;
    protected boolean mapPlants;
    protected boolean mapPlantShadows;
    protected float[] ambientColor;


    protected ArrayList<BlockCoordIntPair> primarySlopeOffsets = new ArrayList<BlockCoordIntPair>(3);
    protected ArrayList<BlockCoordIntPair> secondarySlopeOffsets = new ArrayList<BlockCoordIntPair>(4);
    protected String cachePrefix = "";

    // Need to go in properties
    protected float shadingSlopeMin; // Range: 0-1
    protected float shadingSlopeMax; // Range: 0-?
    protected float shadingPrimaryDownslopeMultiplier; // Range: 0-1
    protected float shadingPrimaryUpslopeMultiplier; // Range: 1-?
    protected float shadingSecondaryDownslopeMultiplier; // Range: 0-1
    protected float shadingSecondaryUpslopeMultiplier; // Range: 1-?

    protected float tweakMoonlightLevel; // Range: 0 - 15
    protected float tweakBrightenDaylightDiff; // Range: 0-1
    protected float tweakBrightenLightsourceBlock; // Range: 0 - 1.5
    protected float tweakBlendShallowWater; // Range: 0 - 1
    protected float tweakMinimumDarkenNightWater; // Range: 0 - 1
    protected float tweakWaterColorBlend; // Range 0-1
    protected int tweakDarkenWaterColorMultiplier; // Range: int rg
    protected int tweakSurfaceAmbientColor; // Range: int rgb
    protected int tweakCaveAmbientColor; // Range: int rgb
    protected int tweakNetherAmbientColor; // Range: int rgb
    protected int tweakEndAmbientColor; // Range: int rgb

    public BaseRenderer()
    {
        updateOptions();

        // TODO: Put in properties
        this.shadingSlopeMin = 0.2f;
        this.shadingSlopeMax = 1.7f;
        this.shadingPrimaryDownslopeMultiplier = .65f;
        this.shadingPrimaryUpslopeMultiplier = 1.20f;
        this.shadingSecondaryDownslopeMultiplier = .95f;
        this.shadingSecondaryUpslopeMultiplier = 1.05f;

        this.tweakMoonlightLevel = 3.5f;
        this.tweakBrightenDaylightDiff = 0.06f;
        this.tweakBrightenLightsourceBlock = 1.2f;
        this.tweakBlendShallowWater = .15f;
        this.tweakMinimumDarkenNightWater = .25f;
        this.tweakWaterColorBlend = .66f;
        this.tweakDarkenWaterColorMultiplier = 0x7A90BF;
        this.tweakSurfaceAmbientColor = 0x00001A;
        this.tweakCaveAmbientColor = 0x000000;
        this.tweakNetherAmbientColor = 0x330808;
        this.tweakEndAmbientColor = 0x00001A;

        // Slope offsets
        primarySlopeOffsets.add(new BlockCoordIntPair(0, -1)); // North
        primarySlopeOffsets.add(new BlockCoordIntPair(-1, -1)); // NorthWest
        primarySlopeOffsets.add(new BlockCoordIntPair(-1, 0)); // West

        secondarySlopeOffsets.add(new BlockCoordIntPair(-1, -2)); // North of NorthWest
        secondarySlopeOffsets.add(new BlockCoordIntPair(-2, -1)); // West of NorthWest
        secondarySlopeOffsets.add(new BlockCoordIntPair(-2, -2)); // NorthWest of NorthWest
        secondarySlopeOffsets.add(new BlockCoordIntPair(-2, 0)); // SouthWest of NorthWest
        secondarySlopeOffsets.add(new BlockCoordIntPair(0, -2)); // West of West
    }

    /**
     * Ensures mapping options are up-to-date.
     */
    protected void updateOptions()
    {
        coreProperties = JourneymapClient.getCoreProperties();
        mapBathymetry = coreProperties.mapBathymetry.get();
        mapTransparency = coreProperties.mapTransparency.get();
        mapAntialiasing = coreProperties.mapAntialiasing.get();
        mapCaveLighting = coreProperties.mapCaveLighting.get();
        mapPlants = coreProperties.mapPlants.get();
        mapPlantShadows = coreProperties.mapPlantShadows.get();
        mapCrops = coreProperties.mapCrops.get();

        // Subclasses should override
        this.ambientColor = new float[]{0, 0, 0};
    }

    @Override
    public float[] getAmbientColor()
    {
        return DEFAULT_FOG;
    }

    @Override
    public void setStratumColors(Stratum stratum, int lightAttenuation, Integer waterColor, boolean waterAbove, boolean underground, boolean mapCaveLighting)
    {
        if (stratum.isUninitialized())
        {
            throw new IllegalStateException("Stratum wasn't initialized for setStratumColors");
        }

        // Daylight is the greater of sun light (15) attenuated through the stack and the stratum's inherent light level
        float daylightDiff = Math.max(1, Math.max(stratum.getLightLevel(), 15 - lightAttenuation)) / 15f;
        daylightDiff += tweakBrightenDaylightDiff;

        // Nightlight is the greater of moon light (4) attenuated through the stack and the stratum's inherent light level
        float nightLightDiff = Math.max(tweakMoonlightLevel, Math.max(stratum.getLightLevel(), tweakMoonlightLevel - lightAttenuation)) / 15f;

        int basicColor;
        if (stratum.isWater())
        {
            basicColor = waterColor;
        }
        else
        {
            ChunkMD chunkMD = stratum.getChunkMd();
            basicColor = stratum.getBlockMD().getColor(chunkMD, chunkMD.toWorldX(stratum.getX()), stratum.getY(), chunkMD.toWorldZ(stratum.getZ()));
        }

        if (stratum.getBlockMD().getBlock() == Blocks.glowstone || stratum.getBlockMD().getBlock() == Blocks.lit_redstone_lamp)
        {
            basicColor = RGB.adjustBrightness(basicColor, tweakBrightenLightsourceBlock); // magic #
        }

        if ((waterAbove) && waterColor != null)
        {
            // Blend day color with watercolor above, adjustBrightness for daylight filtered down
            int adjustedWaterColor = RGB.multiply(waterColor, tweakDarkenWaterColorMultiplier);
            int adjustedBasicColor = RGB.adjustBrightness(basicColor, Math.max(daylightDiff, nightLightDiff));
            stratum.setDayColor(RGB.blendWith(adjustedBasicColor, adjustedWaterColor, tweakWaterColorBlend));

            // Darken for night light and blend with watercolor above
            stratum.setNightColor(RGB.adjustBrightness(stratum.getDayColor(), Math.max(nightLightDiff, tweakMinimumDarkenNightWater)));
        }
        else
        {
            // Just adjustBrightness based on light levels
            stratum.setDayColor(RGB.adjustBrightness(basicColor, daylightDiff));
            stratum.setNightColor(RGB.darkenAmbient(basicColor, nightLightDiff, getAmbientColor()));
        }

        if (underground)
        {
            stratum.setCaveColor(mapCaveLighting ? stratum.getNightColor() : stratum.getDayColor());
        }
    }


    /**
     * Initialize surface slopes in chunk.  This is the black magic
     * that serves as the stand-in for true bump-mapping.
     */
    protected Float[][] populateSlopes(final ChunkMD chunkMd, Integer vSlice,
                                       final HeightsCache chunkHeights,
                                       final SlopesCache chunkSlopes)
    {

        Float[][] slopes = chunkSlopes.getUnchecked(chunkMd.getCoord());
        int y = 0, sliceMinY = 0, sliceMaxY = 0;
        boolean isSurface = (vSlice == null);
        Float slope, primarySlope, secondarySlope;
        BlockMD blockMD;

        if (!isSurface)
        {
            int[] sliceBounds = getVSliceBounds(chunkMd, vSlice);
            sliceMinY = sliceBounds[0];
            sliceMaxY = sliceBounds[1];
        }

        for (int z = 0; z < 16; z++)
        {
            for (int x = 0; x < 16; x++)
            {
                // Get block height
                if (isSurface)
                {
                    y = getSurfaceBlockHeight(chunkMd, x, z, chunkHeights);
                }
                else
                {
                    y = getSliceBlockHeight(chunkMd, x, vSlice, z, sliceMinY, sliceMaxY, chunkHeights);
                }

                // Calculate primary slope
                primarySlope = calculateSlope(chunkMd, primarySlopeOffsets, x, y, z, isSurface, vSlice, sliceMinY, sliceMaxY, chunkHeights);

                // Exaggerate primary slope for normal shading
                slope = primarySlope;
                if (slope < 1)
                {
                    slope *= shadingPrimaryDownslopeMultiplier;
                }
                else if (slope > 1)
                {
                    slope *= shadingPrimaryUpslopeMultiplier;
                }

                // Calculate secondary slope
                if (mapAntialiasing && primarySlope == 1f)
                {
                    secondarySlope = calculateSlope(chunkMd, secondarySlopeOffsets, x, y, z, isSurface, vSlice, sliceMinY, sliceMaxY, chunkHeights);

                    if (secondarySlope > primarySlope)
                    {
                        slope *= shadingSecondaryUpslopeMultiplier;
                    }
                    else if (secondarySlope < primarySlope)
                    {
                        slope *= shadingSecondaryDownslopeMultiplier;
                    }
                }

                // Set that slope.  Set it good.  Aw yeah.
                if (slope.isNaN())
                {
                    slope = 1f;
                }

                slopes[x][z] = Math.min(shadingSlopeMax, Math.max(shadingSlopeMin, slope));
            }
        }

        return slopes;

    }

    /**
     * Get block height within slice.  Should lazy-populate sliceHeights. Can return null.
     */
    protected Integer getSliceBlockHeight(final ChunkMD chunkMd, final int x, final Integer vSlice, final int z, final int sliceMinY, final int sliceMaxY,
                                          final HeightsCache chunkHeights)
    {
        throw new NotImplementedException("getSliceBlockHeight");
    }

    /**
     * Get the height of the block at the coordinates + offsets.  Uses chunkMd.slopes.
     */
    protected int getSliceBlockHeight(final ChunkMD chunkMd, final int x, final Integer vSlice, final int z, final int sliceMinY,
                                      final int sliceMaxY, BlockCoordIntPair offset, int defaultVal,
                                      final HeightsCache chunkHeights)
    {
        final int blockX = (chunkMd.getCoord().chunkXPos << 4) + (x + offset.x);
        final int blockZ = (chunkMd.getCoord().chunkZPos << 4) + (z + offset.z);
        ChunkMD targetChunkMd;

        if (blockX >> 4 == chunkMd.getCoord().chunkXPos && blockZ >> 4 == chunkMd.getCoord().chunkZPos)
        {
            targetChunkMd = chunkMd;
        }
        else
        {
            targetChunkMd = dataCache.getChunkMD(coordinates.setChunkXPos(blockX >> 4).setChunkZPos(blockZ >> 4));
        }

        if (targetChunkMd != null)
        {
            return getSliceBlockHeight(targetChunkMd, blockX & 15, vSlice, blockZ & 15, sliceMinY, sliceMaxY, chunkHeights);
        }
        else
        {
            return defaultVal;
        }
    }

    protected float calculateSlope(final ChunkMD chunkMd, final Collection<BlockCoordIntPair> offsets, final int x, final int y, final int z, boolean isSurface,
                                   Integer vSlice, int sliceMinY, int sliceMaxY,
                                   final HeightsCache chunkHeights)
    {
        if (y <= 0)
        {
            // Flat
            return 1f;
        }

        // Calculate slope by dividing height by neighbors' heights
        float slopeSum = 0;
        int defaultHeight = y;

        float offsetHeight;
        for (BlockCoordIntPair offset : offsets)
        {
            if (isSurface)
            {
                offsetHeight = getSurfaceBlockHeight(chunkMd, x, z, offset, defaultHeight, chunkHeights);
            }
            else
            {
                offsetHeight = getSliceBlockHeight(chunkMd, x, vSlice, z, sliceMinY, sliceMaxY, offset, defaultHeight, chunkHeights);
            }
            slopeSum += ((y * 1f) / offsetHeight);
        }
        Float slope = slopeSum / offsets.size();
        if (slope.isNaN())
        {
            slope = 1f;
        }

        return slope;
    }


    protected int[] getVSliceBounds(final ChunkMD chunkMd, final Integer vSlice)
    {
        if (vSlice == null)
        {
            return null;
        }

        final int sliceMinY = Math.max((vSlice << 4), 0);
        final int hardSliceMaxY = ((vSlice + 1) << 4) - 1;
        int sliceMaxY = Math.min(hardSliceMaxY, chunkMd.getWorld().getActualHeight());
        if (sliceMinY >= sliceMaxY)
        {
            sliceMaxY = sliceMinY + 2;
        }

        return new int[]{sliceMinY, sliceMaxY};
    }

    protected float getSlope(final ChunkMD chunkMd, final BlockMD blockMD, int x, Integer vSlice, int z,
                             final HeightsCache chunkHeights,
                             final SlopesCache chunkSlopes)
    {
        Float[][] slopes = chunkSlopes.getIfPresent(chunkMd.getCoord());

        if (slopes == null || slopes[x][z] == null)
        {
            slopes = populateSlopes(chunkMd, vSlice, chunkHeights, chunkSlopes);
        }

        Float slope = slopes[x][z];
        if (slope == null || slope.isNaN())
        {
            Journeymap.getLogger().warn(String.format("Bad slope for %s at %s,%s: %s", chunkMd, x, z, slope));
            slope = 1f;
        }
        return slope;
    }

//    /**
//     * Get the height of the block at the x, z coordinate in the chunk, optionally ignoring NoShadow blocks.
//     * Ignoring NoShadow blocks won't change the saved surfaceHeights.
//     */
//    public int getSurfaceBlockHeight(final ChunkMD chunkMd, int x, int z, boolean ignoreNoShadowBlocks)
//    {
//        int y = getSurfaceBlockHeight(chunkMd, x, z, ignoreWater);
//        if(ignoreNoShadowBlocks)
//        {
//            BlockMD blockMD = dataCache.getBlockMD(chunkMd, x, y, z);
//            while (y > 0 && blockMD.hasFlag(BlockMD.Flag.NoShadow) || blockMD.isAir() || (ignoreWater && (blockMD.isWater())))
//            {
//                y--;
//                blockMD = dataCache.getBlockMD(chunkMd, x, y, z);
//            }
//        }
//        return y;
//    }

    /**
     * Added because getHeight() sometimes returns an air block.
     * Returns the value in the height map at this x, z coordinate in the chunk, disregarding
     * blocks that shouldn't be used as the top block.
     */
    public Integer getSurfaceBlockHeight(final ChunkMD chunkMd, int x, int z, final HeightsCache chunkHeights)
    {
        Integer[][] heights = chunkHeights.getUnchecked(chunkMd.getCoord());
        if (heights == null)
        {
            // Not in cache anymore
            return null;
        }
        Integer y;

        y = heights[x][z];

        if (y != null)
        {
            // Already set
            return y;
        }

        // Find the height.
        y = Math.max(0, chunkMd.getPrecipitationHeight(x, z));

        try
        {
            BlockMD blockMD = BlockMD.getBlockMD(chunkMd, x, y, z);
            boolean propUnsetWaterHeight = true;

            while (y > 0)
            {
                if (blockMD.isWater())
                {
                    if (!mapBathymetry)
                    {
                        break;
                    }
                    else if (propUnsetWaterHeight)
                    {
                        setColumnProperty(PROP_WATER_HEIGHT, y, chunkMd, x, z);
                        propUnsetWaterHeight = false;
                    }
                }
                else if (!blockMD.isAir())// && !blockMD.hasFlag(BlockMD.Flag.NoShadow))
                {
                    if (mapPlants && blockMD.hasFlag(BlockMD.Flag.Plant))
                    {
                        if (!mapPlantShadows)
                        {
                            y--;
                        }
                    }
                    else if (mapCrops && blockMD.hasFlag(BlockMD.Flag.Crop))
                    {
                        if (!mapPlantShadows)
                        {
                            y--;
                        }
                    }
                    else if (!blockMD.isLava() && blockMD.hasNoShadow())
                    {
                        y--;
                    }
                    break;
                }
                y--;
                blockMD = BlockMD.getBlockMD(chunkMd, x, y, z);
            }
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn("Couldn't get safe surface block height at " + x + "," + z + ": " + e);
        }

        //why is height 4 set on a chunk to the left?
        y = Math.max(0, y);

        heights[x][z] = y;

        return y;
    }


    /**
     * Get the height of the block at the coordinates + offsets.  Uses chunkMd.slopes.
     */
    public int getSurfaceBlockHeight(final ChunkMD chunkMd, int x, int z, BlockCoordIntPair offset, int defaultVal,
                                     final HeightsCache chunkHeights)
    {
        final int blockX = (chunkMd.getCoord().chunkXPos << 4) + (x + offset.x);
        final int blockZ = (chunkMd.getCoord().chunkZPos << 4) + (z + offset.z);
        ChunkMD targetChunkMd;

        if (blockX >> 4 == chunkMd.getCoord().chunkXPos && blockZ >> 4 == chunkMd.getCoord().chunkXPos)
        {
            targetChunkMd = chunkMd;
        }
        else
        {
            targetChunkMd = dataCache.getChunkMD(coordinates.setChunkXPos(blockX >> 4).setChunkZPos(blockZ >> 4));
        }

        if (targetChunkMd != null)
        {
            Integer height = getSurfaceBlockHeight(targetChunkMd, blockX & 15, blockZ & 15, chunkHeights);
            if (height == null)
            {
                return defaultVal;
            }
            else
            {
                return height;
            }
        }
        else
        {
            return defaultVal;
        }
    }

    /**
     * Get a CacheBuilder with common configuration already set.
     *
     * @return
     */
    private CacheBuilder<Object, Object> getCacheBuilder()
    {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        if (JourneymapClient.getCoreProperties().recordCacheStats.get())
        {
            builder.recordStats();
        }
        return builder;
    }

    protected void setColumnProperty(String name, Serializable value, ChunkMD chunkMD, int x, int z)
    {
        getColumnProperties(chunkMD, x, z, true).put(name, value);
    }

    protected <V extends Serializable> V getColumnProperty(String name, V defaultValue, ChunkMD chunkMD, int x, int z)
    {
        HashMap<String, Serializable> props = getColumnProperties(chunkMD, x, z);
        V value = null;
        if (props != null)
        {
            value = (V) props.get(name);
        }
        return (value != null) ? value : defaultValue;
    }

    protected HashMap<String, Serializable> getColumnProperties(ChunkMD chunkMD, int x, int z)
    {
        return getColumnProperties(chunkMD, x, z, false);
    }

    protected HashMap<String, Serializable> getColumnProperties(ChunkMD chunkMD, int x, int z, boolean forceCreation)
    {
        try
        {
            HashMap<String, Serializable>[][] propArr = columnPropertiesCache.get(chunkMD.getCoord());
            HashMap<String, Serializable> props = propArr[x][z];
            if (props == null && forceCreation)
            {
                props = new HashMap<String, Serializable>();
                propArr[x][z] = props;
            }
            return props;
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn(e.getMessage());
            return null;
        }
    }

    /**
     * Cache for storing block heights in a 2-dimensional array, keyed to chunk coordinates.
     */
    public class HeightsCache extends ForwardingLoadingCache<ChunkCoordIntPair, Integer[][]>
    {
        final LoadingCache<ChunkCoordIntPair, Integer[][]> internal;

        protected HeightsCache(String name)
        {
            this.internal = getCacheBuilder().build(new CacheLoader<ChunkCoordIntPair, Integer[][]>()
            {
                @Override
                public Integer[][] load(ChunkCoordIntPair key) throws Exception
                {
                    //JourneyMap.getLogger().info("Initialized heights for chunk " + key);
                    return new Integer[16][16];
                }
            });
            DataCache.instance().addPrivateCache(name, this);
        }

        @Override
        protected LoadingCache<ChunkCoordIntPair, Integer[][]> delegate()
        {
            return internal;
        }
    }

    /**
     * Cache for storing block slopes in a 2-dimensional array, keyed to chunk coordinates.
     */
    public class SlopesCache extends ForwardingLoadingCache<ChunkCoordIntPair, Float[][]>
    {
        final LoadingCache<ChunkCoordIntPair, Float[][]> internal;

        protected SlopesCache(String name)
        {
            this.internal = getCacheBuilder().build(new CacheLoader<ChunkCoordIntPair, Float[][]>()
            {
                @Override
                public Float[][] load(ChunkCoordIntPair key) throws Exception
                {
                    //JourneyMap.getLogger().info("Initialized slopes for chunk " + key);
                    return new Float[16][16];
                }
            });
            DataCache.instance().addPrivateCache(name, this);
        }

        @Override
        protected LoadingCache<ChunkCoordIntPair, Float[][]> delegate()
        {
            return internal;
        }
    }

    /**
     * Cache for storing block properties in a 2-dimensional array, keyed to chunk coordinates.
     */
    public class BlockColumnPropertiesCache extends ForwardingLoadingCache<ChunkCoordIntPair, HashMap<String, Serializable>[][]>
    {
        final LoadingCache<ChunkCoordIntPair, HashMap<String, Serializable>[][]> internal;

        protected BlockColumnPropertiesCache(String name)
        {
            this.internal = getCacheBuilder().build(new CacheLoader<ChunkCoordIntPair, HashMap<String, Serializable>[][]>()
            {
                @Override
                public HashMap<String, Serializable>[][] load(ChunkCoordIntPair key) throws Exception
                {
                    //JourneyMap.getLogger().info("Initialized column properties for chunk " + key);
                    return new HashMap[16][16];
                }
            });
            DataCache.instance().addPrivateCache(name, this);
        }

        @Override
        protected LoadingCache<ChunkCoordIntPair, HashMap<String, Serializable>[][]> delegate()
        {
            return internal;
        }
    }

}
