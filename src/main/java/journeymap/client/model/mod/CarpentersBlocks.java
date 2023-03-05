/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model.mod;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import static journeymap.client.model.BlockMD.Flag.*;

/**
 * Special handling for CarpentersBlocks to get the proper color of the wrapped block, etc.
 */
public class CarpentersBlocks
{
    public static class CommonHandler implements ModBlockDelegate.IModBlockHandler
    {
        private static final String MODID = "CarpentersBlocks";
        private static final String TAG_ATTR_LIST = "cbAttrList";
        private static final String TAG_ID = "id";
        private static final String TAG_DAMAGE = "Damage";

        /**
         * Sets special handling for all CarpentersBlocks except torches, which are
         * treated like vanilla torches and not mapped.
         */
        @Override
        public boolean initialize(BlockMD blockMD)
        {

            GameRegistry.UniqueIdentifier uid = blockMD.getUid();
            if (uid.modId.equals(MODID))
            {
                if (uid.name.equals("blockCarpentersTorch"))
                {
                    blockMD.addFlags(HasAir, NoShadow);
                }
                else if (uid.name.equals("blockCarpentersLadder"))
                {
                    blockMD.addFlags(OpenToSky);
                    blockMD.setModBlockHandler(this);
                    return true;
                }
                else
                {
                    blockMD.setModBlockHandler(this);
                    return true;
                }
            }

            return false;
        }

        /**
         * Get the block flagged with used to color the carpenter's block.
         *
         * @param chunkMD Containing chunk
         * @param blockMD CarpentersBlock flagged with SpecialHandling
         * @param localX  x local to chunk
         * @param y       y
         * @param localZ  z local to chunk
         * @return block used to provide color
         */
        @Override
        public BlockMD handleBlock(ChunkMD chunkMD, BlockMD blockMD, int localX, int y, int localZ)
        {
            final int blockX = chunkMD.toWorldX(localX);
            final int blockZ = chunkMD.toWorldZ(localZ);
            final TileEntity tileEntity = ForgeHelper.INSTANCE.getTileEntity(blockX, y, blockZ);
            if (tileEntity != null)
            {
                final NBTTagCompound tag = new NBTTagCompound();
                tileEntity.writeToNBT(tag);

                if (!tag.hasNoTags())
                {
                    int id;
                    int meta = 0;
                    NBTTagList attrs = tag.getTagList(TAG_ATTR_LIST, 10);
                    String idString = attrs.getCompoundTagAt(0).getString(TAG_ID);

                    if (idString.length() > 0)
                    {
                        id = Integer.parseInt(idString.substring(0, idString.length() - 1));
                        String idMeta = attrs.getCompoundTagAt(0).getString(TAG_DAMAGE);
                        if (idMeta.length() > 0)
                        {
                            meta = Integer.parseInt(idMeta.substring(0, idMeta.length() - 1));
                        }
                        Block block = GameData.getBlockRegistry().getObjectById(id);
                        blockMD = BlockMD.get(block, meta);
                    }
                }
            }
            return blockMD;
        }
    }
}
