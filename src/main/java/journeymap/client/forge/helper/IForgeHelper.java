/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.helper;

import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import java.net.SocketAddress;
import java.util.Iterator;

/**
 * Interface used to encapsulate compile-time differences between Minecraft/Forge versions.
 */
public interface IForgeHelper
{
    public IRenderHelper getRenderHelper();

    public IBlockAccess getIBlockAccess();

    public IColorHelper getColorHelper();

    public Minecraft getClient();

    public ScaledResolution getScaledResolution();

    public EnumSkyBlock getSkyBlock();

    public FontRenderer getFontRenderer();

    public int getPlayerDimension();

    public boolean hasNoSky(World world);

    public World getWorld();

    public World getWorld(Chunk chunk);

    public RenderManager getRenderManager();

    public String getEntityName(Entity entity);

    public boolean hasCustomName(Entity entity);

    public int getLightOpacity(BlockMD blockMD, int blockX, int blockY, int blockZ);

    public AxisAlignedBB getBoundingBox(EntityPlayer player, double lateralDistance, double verticalDistance);

    public AxisAlignedBB getEntityBoundingBox(EntityLivingBase entity);

    public AxisAlignedBB getBoundingBox(int x1, int y1, int z1, int x2, int y2, int z2);

    public Vec3 newVec3(double x, double y, double z);

    public Vec3 getEntityPositionVector(Entity entity);

    public String getRealmsServerName();

    public Tessellator getTessellator();

    public int getDimension(World world);

    public int getDimension(WorldProvider worldProvider);

    public int getSavedLightValue(Chunk chunk, int x, int y, int z);

    public boolean canBlockSeeTheSky(Chunk chunk, int x, int y, int z);

    public int getHeight(Chunk chunk, int x, int z);

    public int getPrecipitationHeight(Chunk chunk, int x, int z);

    public int getLightOpacity(Chunk chunk, Block block, int localX, int y, int localZ);

    public TileEntity getTileEntity(int blockX, int y, int blockZ);

    public String getBlockName(Block block, int meta);

    public BiomeGenBase getBiome(ChunkMD chunkMD, int x, int y, int z);

    public BiomeGenBase getBiome(int x, int y, int z);

    public int getBlockMeta(Chunk chunk, final int x, int y, final int z);

    public boolean hasNoSky(Entity entity);

    public boolean hasChunkData(Chunk chunk);

    public Iterator<Block> getRegisteredBlocks();

    public SocketAddress getSocketAddress(NetworkManager netManager);

    public String getFPS();
}
