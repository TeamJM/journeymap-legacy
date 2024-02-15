/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model.mod;

import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.client.model.mod.vanilla.VanillaColorHandler;

import static journeymap.client.model.BlockMD.Flag.*;

/**
 * Special handling required for TFC to set biome-related flags
 */
public class TerraFirmaCraft
{
    private static final String MODID = "terrafirmacraft";
    private static final String MODID2 = "tfc2";
    private static final String MODID3 = "terrafirmacraftplus";
    private static final int WATER_COLOR = 0x0b1940;

    public static class TfcBlockHandler implements ModBlockDelegate.IModBlockHandler
    {
        private final TfcWaterColorHandler waterColorHandler = new TfcWaterColorHandler();

        @Override
        public boolean initialize(BlockMD blockMD)
        {
            if (blockMD.getUid().modId.equals(MODID) || blockMD.getUid().modId.equals(MODID2) || blockMD.getUid().modId.equals(MODID3))
            {
                String name = blockMD.getUid().name.toLowerCase();
                if (name.equals("looserock") || name.equals("loose_rock") || name.contains("rubble") || name.contains("vegetation"))
                {
                    blockMD.addFlags(HasAir, NoShadow, NoTopo);
                }
                else if (name.contains("seagrass"))
                {
                    blockMD.setTextureSide(2);
                    blockMD.addFlags(Plant);
                    //preloadColor(blockMD);
                }
                else if (name.contains("grass"))
                {
                    blockMD.addFlags(Grass);
                    //preloadColor(blockMD);
                }
                else if (name.contains("dirt"))
                {
                    //preloadColor(blockMD);
                }
                else if (name.contains("water"))
                {
                    blockMD.setAlpha(.3f);
                    blockMD.addFlags(Water, NoShadow);
                    blockMD.setBlockColorHandler(waterColorHandler);
                }
                else if (name.contains("leaves"))
                {
                    blockMD.addFlags(NoTopo, Foliage);
                }
            }

            return true;
        }

        @Override
        public BlockMD handleBlock(ChunkMD chunkMD, BlockMD blockMD, int localX, int y, int localZ)
        {
            return blockMD;
        }

    }

    public static class TfcWaterColorHandler extends VanillaColorHandler
    {
        @Override
        protected Integer loadTextureColor(BlockMD blockMD, int globalX, int y, int globalZ)
        {
            return WATER_COLOR;
        }
    }
}
