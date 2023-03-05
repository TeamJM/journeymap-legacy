/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model.mod;

import cpw.mods.fml.common.registry.GameRegistry;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.client.model.Waypoint;
import journeymap.client.waypoint.WaypointStore;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static journeymap.client.model.BlockMD.Flag.*;

//import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Special handlers for miscellaneous mods that don't really need their own impl.
 */
public class Miscellaneous
{
    public static class CommonHandler implements ModBlockDelegate.IModBlockHandler
    {
        // Mariculture Kelp
        GameRegistry.UniqueIdentifier maricultureKelpId = new GameRegistry.UniqueIdentifier("Mariculture:kelp");

        // Thaumcraft leaves (greatwood, silverwood)
        GameRegistry.UniqueIdentifier thaumcraftLeavesId = new GameRegistry.UniqueIdentifier("Thaumcraft:blockMagicalLeaves");

        List<GameRegistry.UniqueIdentifier> torches = new ArrayList<GameRegistry.UniqueIdentifier>();

        public CommonHandler()
        {
            torches.add(new GameRegistry.UniqueIdentifier("TConstruct:decoration.stonetorch"));
            torches.add(new GameRegistry.UniqueIdentifier("ExtraUtilities:magnumTorch"));
            torches.add(new GameRegistry.UniqueIdentifier("appliedenergistics2:tile.BlockQuartzTorch"));
            for (int i = 1; i <= 10; i++)
            {
                torches.add(new GameRegistry.UniqueIdentifier("chisel:torch" + i));
            }
        }

        @Override
        public boolean initialize(BlockMD blockMD)
        {
            GameRegistry.UniqueIdentifier uid = blockMD.getUid();
            if (torches.contains(uid))
            {
                blockMD.addFlags(HasAir, NoShadow);
            }
            else if (uid.equals(maricultureKelpId))
            {
                blockMD.addFlags(Plant);
                blockMD.setTextureSide(2);
            }
            else if (uid.equals(thaumcraftLeavesId))
            {
                blockMD.addFlags(NoTopo, Foliage);
            }
            return false;
        }

        @Override
        public BlockMD handleBlock(ChunkMD chunkMD, BlockMD blockMD, int localX, int y, int localZ)
        {
            // Should never be called
            return blockMD;
        }
    }

    /**
     * Create a waypoint for an OpenBlocks.
     * TODO: Buggy, don't use until fixed
     */
    public static class OpenBlocksGraveHandler implements ModBlockDelegate.IModBlockHandler
    {
        public static final GameRegistry.UniqueIdentifier UID = new GameRegistry.UniqueIdentifier("OpenBlocks:grave");
        private static final String TAG_PLAYERNAME = "PlayerName";
        private static final String TAG_PLAYERUUID = "PlayerUUID";

        @Override
        public boolean initialize(BlockMD blockMD)
        {
            if (blockMD.getUid().equals(UID))
            {
                blockMD.setModBlockHandler(this);
                return true;
            }

            return false;
        }

        @Override
        public BlockMD handleBlock(ChunkMD chunkMD, BlockMD blockMD, int localX, int y, int localZ)
        {
            int blockX = chunkMD.toWorldX(localX);
            int blockZ = chunkMD.toWorldZ(localZ);
            //String name = I18n.format("tile.openblocks.grave.name");
            TileEntity tileEntity = ForgeHelper.INSTANCE.getTileEntity(blockX, y, blockZ);

            if (tileEntity != null)
            {
                NBTTagCompound tag = new NBTTagCompound();
                tileEntity.writeToNBT(tag);

                String playerName = null;
                if (tag.hasNoTags())
                {
                    playerName = "?";
                }
                else
                {
                    playerName = tag.getString(TAG_PLAYERNAME);
                }

                if (playerName == null)
                {
                    playerName = "";
                }

                Waypoint waypoint = new Waypoint(playerName + " " + blockMD.getName(), blockX, y, blockZ, Color.red, Waypoint.Type.Death, chunkMD.getDimension());
                WaypointStore.instance().add(waypoint);
            }

            return blockMD;
        }
    }
}
