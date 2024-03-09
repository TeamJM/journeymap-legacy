/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.helper.impl;

import com.google.common.base.Strings;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.McoServer;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.ReflectionHelper;
import journeymap.client.data.DataCache;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.forge.helper.IColorHelper;
import journeymap.client.forge.helper.IForgeHelper;
import journeymap.client.forge.helper.IRenderHelper;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.common.Journeymap;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenRealmsProxy;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Implementation to encapsulate uses of methods/fields that differ from 1.8 / 1.8.8
 */
public class ForgeHelper_1_7_10 implements IForgeHelper
{
    private IRenderHelper renderHelper = new RenderHelper_1_7_10();
    private IBlockAccess blockAccess = new JmBlockAccess();

    private static Minecraft mc = Minecraft.getMinecraft();

    @Override
    public IRenderHelper getRenderHelper()
    {
        return renderHelper;
    }

    @Override
    public IBlockAccess getIBlockAccess()
    {
        return blockAccess;
    }

    @Override
    public IColorHelper getColorHelper()
    {
        return new ColorHelper_1_7_10();
    }

    @Override
    public Minecraft getClient()
    {
        return FMLClientHandler.instance().getClient();
    }

    @Override
    public ScaledResolution getScaledResolution()
    {
        Minecraft mc = getClient();

        // 1.7.10, 1.8
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);

        // 1.8.8
        // return new ScaledResolution(mc);
    }

    @Override
    public EnumSkyBlock getSkyBlock()
    {
        return EnumSkyBlock.Block;
    }

    @Override
    public FontRenderer getFontRenderer()
    {
        return getClient().fontRenderer;
    }

    @Override
    public int getPlayerDimension()
    {
        return getClient().thePlayer.worldObj.provider.dimensionId;
    }

    @Override
    public boolean hasNoSky(World world)
    {
        return world.provider.hasNoSky;
    }

    @Override
    public World getWorld()
    {
        return getClient().theWorld;
    }

    @Override
    public World getWorld(Chunk chunk)
    {
        return chunk.worldObj;
    }

    @Override
    public int getLightOpacity(BlockMD blockMD, int x, int y, int z)
    {

        return blockMD.getBlock().getLightOpacity(blockAccess, x & 15, y, z & 15);
    }

    @Override
    public int getDimension(World world)
    {
        return world.provider.dimensionId;
    }

    @Override
    public int getDimension(WorldProvider worldProvider)
    {
        return worldProvider.dimensionId;
    }

    @Override
    public int getSavedLightValue(Chunk chunk, int localX, int y, int localZ)
    {
        try
        {
            return chunk.getSavedLightValue(getSkyBlock(), localX & 15, y, localZ & 15);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            // Encountered on a 1.8 custom-gen world where the value was 16
            return 1; // At least let it show up
        }
    }

    @Override
    public RenderManager getRenderManager()
    {
        return RenderManager.instance;
    }

    /**
     * Gets the entity's name (player name) / command sender name.
     */
    @Override
    public String getEntityName(Entity entity)
    {
        return entity.getCommandSenderName();
    }

    @Override
    public boolean hasCustomName(Entity entity)
    {
        return ((EntityLiving) entity).hasCustomNameTag();
    }

    @Override
    public AxisAlignedBB getBoundingBox(int x1, int y1, int z1, int x2, int y2, int z2)
    {

        return AxisAlignedBB.getBoundingBox(x1, y1, z1, x2, y2, z2);
    }

    @Override
    public AxisAlignedBB getBoundingBox(EntityPlayer player, double lateralDistance, double verticalDistance)
    {

        return AxisAlignedBB.getBoundingBox(player.posX, player.posY, player.posZ, player.posX, player.posY, player.posZ).expand(lateralDistance, verticalDistance, lateralDistance);
    }

    @Override
    public Vec3 newVec3(double x, double y, double z)
    {

        return Vec3.createVectorHelper(x, y, z);
    }

    /**
     * Gets the entity's bounding box.
     */
    @Override
    public AxisAlignedBB getEntityBoundingBox(EntityLivingBase entity)
    {

        return entity.boundingBox;
    }

    /**
     * Gets the server name.
     */
    @Override
    public String getRealmsServerName()
    {
        String serverName = null;
        Minecraft mc = ForgeHelper.INSTANCE.getClient();
        if (!mc.isSingleplayer())
        {
            try
            {
                NetHandlerPlayClient netHandler = mc.getNetHandler();
                GuiScreen netHandlerGui = ReflectionHelper.getPrivateValue(NetHandlerPlayClient.class, netHandler, "field_147307_j", "guiScreenServer");

                if (netHandlerGui instanceof GuiScreenRealmsProxy)
                {
                    RealmsScreen realmsScreen = ((GuiScreenRealmsProxy) netHandlerGui).func_154321_a();
                    if (realmsScreen instanceof RealmsMainScreen)
                    {
                        RealmsMainScreen mainScreen = (RealmsMainScreen) realmsScreen;
                        long selectedServerId = ReflectionHelper.getPrivateValue(RealmsMainScreen.class, mainScreen, "selectedServerId");
                        List<McoServer> mcoServers = ReflectionHelper.getPrivateValue(RealmsMainScreen.class, mainScreen, "mcoServers");
                        for (McoServer mcoServer : mcoServers)
                        {
                            if (mcoServer.id == selectedServerId)
                            {
                                serverName = mcoServer.name;
                                break;
                            }
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                Journeymap.getLogger().error("Unable to get Realms server name: " + LogFormatter.toString(t));
            }
        }

        if (serverName != null)
        {
            return serverName;
        }
        else
        {
            // mc.getCurrentServerData();
            ServerData serverData = mc.func_147104_D();

            if (serverData != null)
            {
                serverName = serverData.serverName;
                if (serverName != null)
                {
                    serverName = serverName.replaceAll("\\W+", "~").trim();

                    if (Strings.isNullOrEmpty(serverName.replaceAll("~", "")))
                    {
                        serverName = serverData.serverIP;
                    }
                    return serverName;
                }
            }
        }

        if (serverName != null)
        {
            return serverName;
        }
        else
        {
            mc = ForgeHelper.INSTANCE.getClient();
            // mc.getCurrentServerData();
            ServerData serverData = mc.func_147104_D();

            if (serverData != null)
            {
                serverName = serverData.serverName;
                if (serverName != null)
                {
                    serverName = serverName.replaceAll("\\W+", "~").trim();

                    if (Strings.isNullOrEmpty(serverName.replaceAll("~", "")))
                    {
                        serverName = serverData.serverIP;
                    }
                    return serverName;
                }
            }
        }
        return null;
    }

    @Override
    public Vec3 getEntityPositionVector(Entity entity)
    {
        return Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ);
    }

    @Override
    public Tessellator getTessellator()
    {
        return Tessellator.instance;
    }

    @Override
    public boolean canBlockSeeTheSky(Chunk chunk, int x, int y, int z)
    {
        return chunk.canBlockSeeTheSky(x, y, z);
    }

    @Override
    public int getHeight(Chunk chunk, int x, int z)
    {
        return chunk.getHeightValue(x, z);
    }

    @Override
    public int getPrecipitationHeight(Chunk chunk, int x, int z)
    {
        return chunk.getPrecipitationHeight(x, z);
    }

    @Override
    public int getLightOpacity(Chunk chunk, Block block, int localX, int y, int localZ)
    {
        return block.getLightOpacity(chunk.worldObj, (chunk.xPosition << 4) + localX, y, (chunk.zPosition << 4) + localZ);
    }

    @Override
    public TileEntity getTileEntity(int localX, int y, int localZ)
    {
        return blockAccess.getTileEntity(localX, y, localZ);
    }

    @Override
    public String getBlockName(Block block, int meta)
    {
        // Gotta love this.
        Item item = Item.getItemFromBlock(block);
        if (item == null)
        {
            item = block.getItemDropped(0, new Random(), 0);
        }
        if (item != null)
        {
            ItemStack stack = new ItemStack(item, 1, block.damageDropped(meta));

            String displayName = stack.getDisplayName();
            if (!Strings.isNullOrEmpty(displayName))
            {
                return displayName;
            }
        }
        return null;
    }

    @Override
    public BiomeGenBase getBiome(ChunkMD chunkMD, int x, int y, int z)
    {
        try
        {
            return mc.theWorld.getBiomeGenForCoords(x,z);
        }
        catch (Throwable throwable)
        {
            Journeymap.getLogger().error("Error in getBiome(): " + throwable);
            return ForgeHelper.INSTANCE.getWorld().getBiomeGenForCoords(x, z);
        }
    }

    @Override
    public BiomeGenBase getBiome(int x, int y, int z)
    {
        ChunkMD chunkMD = DataCache.instance().getChunkMD(new ChunkCoordIntPair(x >> 4, z >> 4));
        return getBiome(chunkMD, x, y, z);
    }

    @Override
    public int getBlockMeta(Chunk chunk, final int x, int y, final int z)
    {
        return chunk.getBlockMetadata(x, y, z);
    }

    @Override
    public boolean hasNoSky(Entity entity)
    {
        return hasNoSky(entity.worldObj);
    }

    @Override
    public boolean hasChunkData(Chunk chunk)
    {
        return (chunk.isChunkLoaded && !chunk.isEmpty());
    }

    @Override
    public Iterator<Block> getRegisteredBlocks()
    {
        return GameData.getBlockRegistry().iterator();
    }

    @Override
    public SocketAddress getSocketAddress(NetworkManager netManager)
    {
        return netManager.getSocketAddress();
    }

    @Override
    public String getFPS()
    {
        String fps = Minecraft.getMinecraft().debug;
        final int idx = fps != null ? fps.indexOf(',') : -1;
        if (idx > 0)
        {
            return fps.substring(0, idx);
        }
        else
        {
            return "";
        }
    }

    class JmBlockAccess implements IBlockAccess
    {
        private Chunk getChunk(int x, int z)
        {
            ChunkMD chunkMD = DataCache.instance().getChunkMD(new ChunkCoordIntPair(x >> 4, z >> 4));
            if (chunkMD != null && chunkMD.hasChunk())
            {
                return chunkMD.getChunk();
            }
            return null;
        }

        @Override
        public Block getBlock(int x, int y, int z)
        {
            if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000 && y >= 0 && y < 256)
            {
                Chunk chunk = getChunk(x, z);
                if (chunk != null)
                {
                    return chunk.getBlock(x & 15, y, z & 15);
                }
            }
            return Blocks.air;
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z)
        {
            return ForgeHelper.INSTANCE.getWorld().getTileEntity(x, y, z);
        }

        @Override
        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min)
        {
            return ForgeHelper.INSTANCE.getWorld().getLightBrightnessForSkyBlocks(x, y, z, min);
        }

        @Override
        public int getBlockMetadata(int x, int y, int z)
        {
            if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000)
            {
                if (y >= 0 && y < 256)
                {
                    Chunk chunk = getChunk(x, z);
                    if (chunk != null)
                    {
                        x &= 15;
                        z &= 15;
                        return chunk.getBlockMetadata(x, y, z);
                    }
                }
            }
            return 0;
        }

        @Override
        public int isBlockProvidingPowerTo(int x, int y, int z, int directionIn)
        {
            return ForgeHelper.INSTANCE.getWorld().isBlockProvidingPowerTo(x, y, z, directionIn);
        }

        @Override
        public boolean isAirBlock(int x, int y, int z)
        {
            Block block = this.getBlock(x, y, z);
            return block.isAir(this, x, y, z);
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z)
        {
            try
            {
                return mc.theWorld.getBiomeGenForCoords(x,z);
            }
            catch (Throwable throwable)
            {
                Journeymap.getLogger().error("Error in getBiomeGenForCoords(): " + throwable);
                return ForgeHelper.INSTANCE.getWorld().getBiomeGenForCoords(x, z);
            }
        }

        @Override
        public int getHeight()
        {
            return ForgeHelper.INSTANCE.getWorld().getHeight();
        }

        @Override
        public boolean extendedLevelsInChunkCache()
        {
            return ForgeHelper.INSTANCE.getWorld().extendedLevelsInChunkCache();
        }

        @Override
        public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default)
        {
            if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000)
            {
                return _default;
            }

            Chunk chunk = getChunk(x >> 4, z >> 4);
            if (chunk == null || chunk.isEmpty())
            {
                return _default;
            }
            return getBlock(x, y, z).isSideSolid(this, x, y, z, side);
        }
    }
}
