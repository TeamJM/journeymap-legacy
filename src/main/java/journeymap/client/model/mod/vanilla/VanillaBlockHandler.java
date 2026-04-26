/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model.mod.vanilla;

import journeymap.client.JourneymapClient;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.client.model.mod.ModBlockDelegate;
import journeymap.common.Journeymap;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static journeymap.client.model.BlockMD.Flag.*;

/**
 * Common handler works with vanilla blocks and mod blocks that inherit from them in a normal way.
 */
public final class VanillaBlockHandler implements ModBlockDelegate.IModBlockHandler
{
    private final HashMap<Material, ArrayList<BlockMD.Flag>> materialFlags = new HashMap<Material, ArrayList<BlockMD.Flag>>();
    private final HashMap<Class<? extends Block>, ArrayList<BlockMD.Flag>> blockClassFlags = new LinkedHashMap<Class<? extends Block>, ArrayList<BlockMD.Flag>>();
    private final HashMap<Block, ArrayList<BlockMD.Flag>> blockFlags = new HashMap<Block, ArrayList<BlockMD.Flag>>();

    private final HashMap<Material, Float> materialAlphas = new HashMap<Material, Float>();
    private final HashMap<Block, Float> blockAlphas = new HashMap<Block, Float>();
    private final HashMap<Class<? extends Block>, Float> blockClassAlphas = new HashMap<Class<? extends Block>, Float>();

    private final HashMap<Block, Integer> blockTextureSides = new HashMap<Block, Integer>();
    private final HashMap<Class<? extends Block>, Integer> blockClassTextureSides = new HashMap<Class<? extends Block>, Integer>();

    private final HashMap<Class<? extends Block>, Class<? extends Block>> cachedParentClasses = new HashMap<Class<? extends Block>, Class<? extends Block>>();

    public VanillaBlockHandler()
    {
        preInitialize();
    }

    private void preInitialize()
    {
        // Init flags and alphas to be set according to a Block's material.
        // setFlags(Material.barrier, HasAir, OpenToSky, NoShadow); // 1.8
        setFlags(Material.air, HasAir, OpenToSky, NoShadow);
        setFlags(Material.glass, .4F, TransparentRoof);
        if (JourneymapClient.getCoreProperties().caveIgnoreGlass.get())
        {
            setFlags(Material.glass, OpenToSky);
        }
        setFlags(Material.water, .3F, NoShadow, Water);
        setFlags(Material.lava, NoShadow);
        materialAlphas.put(Material.ice, .8F);
        materialAlphas.put(Material.packedIce, .8F);

        // Init flags and alphas on specific Block instances
        setFlags(Blocks.iron_bars, .4F);
        setFlags(Blocks.fire, NoShadow);
        setTextureSide(Blocks.fire, 2);
        setFlags(Blocks.iron_bars, TransparentRoof);
        setFlags(Blocks.ladder, OpenToSky);
        setFlags(Blocks.snow_layer, NoTopo);
        setFlags(Blocks.tripwire, NoShadow);
        setFlags(Blocks.tripwire_hook, NoShadow);
        setFlags(Blocks.web, OpenToSky);
        setTextureSide(Blocks.web, 2);

        // Init flags and alphas to be set according to a Block's parent class
        setFlags(BlockLog.class, OpenToSky, CustomBiomeColor, NoTopo);
        setFlags(BlockFence.class, .4F, TransparentRoof);
        setFlags(BlockFenceGate.class, .4F, TransparentRoof);
        setFlags(BlockGrass.class, Grass);
        setFlags(BlockTallGrass.class, HasAir, NoTopo);
        setFlags(BlockDoublePlant.class, Plant, NoTopo);
        setTextureSide(BlockDoublePlant.class, 2);
        setFlags(BlockLeavesBase.class, OpenToSky, Foliage, NoTopo);
        setFlags(BlockVine.class, .2F, OpenToSky, CustomBiomeColor, Foliage, NoTopo, NoShadow);
        setFlags(BlockLilyPad.class, CustomBiomeColor, NoTopo);
        setFlags(BlockCrops.class, Crop, NoTopo);
        setTextureSide(BlockCrops.class, 2);
        setFlags(BlockFlower.class, Plant, NoTopo);
        setFlags(BlockBush.class, Plant, NoTopo);
        setTextureSide(BlockBush.class, 2);
        setFlags(BlockCactus.class, Plant, NoTopo);
        setTextureSide(BlockCactus.class, 2);
        setFlags(BlockRailBase.class, NoShadow, NoTopo);
        setFlags(BlockTorch.class, HasAir, NoShadow, NoTopo);
    }

    /**
     * Set flags, alpha, etc. for a BlockMD
     */
    @Override
    public boolean initialize(BlockMD blockMD)
    {
        // Set vanilla color handler
        blockMD.setBlockColorHandler(VanillaColorHandler.INSTANCE);

        if(blockMD.isWater())
        {
            Journeymap.getLogger().info("Water! {}", blockMD);
        }

        // Set flags based on material
        Material material = blockMD.getBlock().getMaterial();
        ArrayList<BlockMD.Flag> materialFlagList = materialFlags.get(material);
        if (materialFlagList != null)
        {
            blockMD.addFlags(materialFlagList);
        }

        // Set alpha based on material
        Float alpha = materialAlphas.get(material);
        if (alpha != null)
        {
            blockMD.setAlpha(alpha);
        }

        // Set flags based on exact block
        Block block = blockMD.getBlock();
        ArrayList<BlockMD.Flag> blockFlagList = blockFlags.get(block);
        if (blockFlagList != null)
        {
            blockMD.addFlags(blockFlagList);
        }

        // Set alpha based on exact block
        alpha = blockAlphas.get(block);
        if (alpha != null)
        {
            blockMD.setAlpha(alpha);
        }

        // Add flags based on block class inheritance
        if (blockMD.getFlags().isEmpty())
        {
            Class<? extends Block> parentClass = getParentClass(block.getClass());
            if (parentClass != null)
            {
                ArrayList<BlockMD.Flag> flags = blockClassFlags.get(parentClass);
                if (flags != null) blockMD.addFlags(flags);

                alpha = blockClassAlphas.get(parentClass);
                if (alpha != null) blockMD.setAlpha(alpha);

                Integer classTextureSide = blockClassTextureSides.get(parentClass);
                if (classTextureSide != null) blockMD.setTextureSide(classTextureSide);
            }
        }

        // Below are the oddball blocks that need extra help


        if (block instanceof BlockHugeMushroom)
        {
            // 1.7.10
            int overrideMeta = 14;
            if (blockMD.getMeta() != overrideMeta)
            {
                blockMD.setOverrideMeta(overrideMeta);
            }
        }

        // Double-tall grass should be treated like BlockTallGrass:  ignored
        if (block == Blocks.double_plant && blockMD.getMeta() == 2)
        {
            blockMD.addFlags(HasAir, NoTopo);
        }

        // Ferns unlike other BlockTallGrass will be treated like plants
        if (block == Blocks.tallgrass && blockMD.getMeta() == 2)
        {
            blockMD.addFlags(Plant, CustomBiomeColor);
        }

        // Toggle plant shadows
        if (blockMD.hasAnyFlag(BlockMD.FlagsPlantAndCrop) && !JourneymapClient.getCoreProperties().mapPlantShadows.get())
        {
            blockMD.addFlags(NoShadow);
        }

        return false;
    }

    @Override
    public BlockMD handleBlock(ChunkMD chunkMD, BlockMD blockMD, int localX, int y, int localZ)
    {
        // Should never be called
        return blockMD;
    }

    private void setFlags(Material material, BlockMD.Flag... flags)
    {
        materialFlags.computeIfAbsent(material, k -> new ArrayList<>())
            .addAll(Arrays.asList(flags));
    }

    private void setFlags(Material material, Float alpha, BlockMD.Flag... flags)
    {
        materialAlphas.put(material, alpha);
        setFlags(material, flags);
    }

    private void setFlags(Class<? extends Block> parentClass, BlockMD.Flag... flags)
    {
        blockClassFlags.computeIfAbsent(parentClass, k -> new ArrayList<>())
            .addAll(Arrays.asList(flags));
    }

    private void setFlags(Class<? extends Block> parentClass, Float alpha, BlockMD.Flag... flags)
    {
        blockClassAlphas.put(parentClass, alpha);
        setFlags(parentClass, flags);
    }

    private void setFlags(Block block, BlockMD.Flag... flags)
    {
        blockFlags.computeIfAbsent(block, k -> new ArrayList<>())
            .addAll(Arrays.asList(flags));
    }

    private void setFlags(Block block, Float alpha, BlockMD.Flag... flags)
    {
        blockAlphas.put(block, alpha);
        setFlags(block, flags);
    }

    private void setTextureSide(Class<? extends Block> parentClass, int side)
    {
        blockClassTextureSides.put(parentClass, side);
    }

    private void setTextureSide(Block block, int side)
    {
        blockTextureSides.put(block, side);
    }

    // Class.isAssignableFrom is slow, so we cache resolved class connections
    private Class<? extends Block> getParentClass(Class<? extends Block> blockClass)
    {
        if (cachedParentClasses.containsKey(blockClass))
        {
            return cachedParentClasses.get(blockClass);
        }

        for (Map.Entry<Class<? extends Block>, ArrayList<BlockMD.Flag>> entry : blockClassFlags.entrySet())
        {
            Class<? extends Block> parentClass = entry.getKey();
            if (parentClass.isAssignableFrom(blockClass))
            {
                cachedParentClasses.put(blockClass, parentClass);
                return parentClass;
            }
        }

        cachedParentClasses.put(blockClass, null);
        return null;
    }
}
