/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2018  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model.mod;

import journeymap.client.cartography.RGB;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.client.model.mod.vanilla.VanillaColorHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static journeymap.client.model.BlockMD.Flag.Crop;
import static journeymap.client.model.BlockMD.Flag.Plant;

/**
 * Special handling required to flag BoP plants and crops.
 */
public class BiomesOPlenty implements ModBlockDelegate.IModBlockHandler
{
    private List<String> plants = Arrays.asList("duckweed", "shortgrass", "mediumgrass", "flaxbottom", "bush", "sprout", "flaxtop", "poisonivy", "berrybush", "shrub", "wheatgrass", "dampgrass", "koru", "cloverpatch", "leafpile", "deadleafpile", "spectralmoss", "smolderinggrass", "origingrass3", "longgrass", "loamy", "sandy", "silty", "flower", "mushroom", "sapling", "plant", "ivy", "waterlily", "moss", "deadgrass", "desertgrass", "desertsprouts", "dunegrass", "spectralfern", "thorn", "wildrice", "cattail", "rivercane", "cattailtop", "cattailbottom", "wildcarrot", "cactus", "witherwart", "reed", "root");
    private List<String> crops = Collections.singletonList("turnip");
    private BoPGrassColorHandler handler = new BoPGrassColorHandler();

    public BiomesOPlenty()
    {
    }

    @Override
    public boolean initialize(BlockMD blockMD)
    {

        boolean initialized = false;
        if (blockMD.getUid().modId.equalsIgnoreCase("BiomesOPlenty"))
        {
            String name = blockMD.getUid().name.toLowerCase();
//            if (name.contains("grass"))
//            {
//                initialized = true;
//                blockMD.addFlags(Grass);
//                blockMD.setModBlockHandler(this);
//                blockMD.setTextureSide(2);
//            }

            for (String plant : plants)
            {
                if (name.contains(plant))
                {
                    blockMD.addFlags(Plant);
                    initialized = true;
                    break;
                }
            }

            for (String crop : crops)
            {
                if (name.contains(crop))
                {
                    blockMD.addFlags(Crop);
                    initialized = true;
                    break;
                }
            }
        }
        return initialized;
    }

    @Override
    public BlockMD handleBlock(ChunkMD chunkMD, BlockMD blockMD, int localX, int y, int localZ)
    {
        return blockMD;
    }

    public static class BoPGrassColorHandler extends VanillaColorHandler
    {
        @Override
        protected Integer loadTextureColor(BlockMD blockMD, int globalX, int y, int globalZ)
        {
            System.out.println("Loading Tex Color: " + blockMD);
            return RGB.RED_RGB;
        }
    }
}
