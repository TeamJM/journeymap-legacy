/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

import com.google.common.base.Strings;
import com.google.common.cache.CacheLoader;
import journeymap.client.forge.helper.ForgeHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StringUtils;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * JSON-safe attributes derived from an EntityLivingBase.
 */
public class EntityDTO implements Serializable
{
    public final String entityId;
    public transient WeakReference<EntityLivingBase> entityLivingRef;
    public String filename;
    public Boolean hostile;
    public double posX;
    public double posY;
    public double posZ;
    public int chunkCoordX;
    public int chunkCoordY;
    public int chunkCoordZ;
    public double heading;
    public String customName;
    public String owner;
    public Integer profession;
    public String username;
    public String biome;
    public int dimension;
    public Boolean underground;
    public boolean invisible;
    public boolean sneaking;
    public boolean passiveAnimal;

    private EntityDTO(EntityLivingBase entity)
    {
        this.entityLivingRef = new WeakReference<EntityLivingBase>(entity);
        this.entityId = entity.getUniqueID().toString();
    }

    public void update(EntityLivingBase entity, boolean hostile)
    {
        EntityPlayer currentPlayer = ForgeHelper.INSTANCE.getClient().thePlayer;
        this.dimension = entity.dimension;
        this.posX = entity.posX;
        this.posY = entity.posY;
        this.posZ = entity.posZ;
        this.chunkCoordX = entity.chunkCoordX;
        this.chunkCoordY = entity.chunkCoordY;
        this.chunkCoordZ = entity.chunkCoordZ;
        this.heading = Math.round(entity.rotationYawHead % 360);
        if (currentPlayer != null)
        {
            this.invisible = entity.isInvisibleToPlayer(currentPlayer);
        }
        else
        {
            this.invisible = false;
        }
        this.sneaking = entity.isSneaking();

        // Player check
        if (entity instanceof EntityPlayer)
        {
            String name = StringUtils.stripControlCodes(ForgeHelper.INSTANCE.getEntityName(entity));
            this.filename = "/skin/" + name;
            this.username = name;
        }
        else
        {
            this.filename = EntityHelper.getFileName(entity);
            this.username = null;
        }

        // Owner
        String owner = null;
        if (entity instanceof EntityTameable)
        {
            Entity ownerEntity = ((EntityTameable) entity).getOwner();
            if (ownerEntity != null)
            {
                owner = ForgeHelper.INSTANCE.getEntityName(ownerEntity);
            }
        }
        else if (entity instanceof IEntityOwnable)
        {
            Entity ownerEntity = ((IEntityOwnable) entity).getOwner();
            if (ownerEntity != null)
            {
                owner = ForgeHelper.INSTANCE.getEntityName(ownerEntity);
            }
        }
        else if (entity instanceof EntityHorse)
        {
            // TODO: Test this with and without owners
            // 1.8
            String ownerUuidString = ((EntityHorse) entity).func_152119_ch();
            // 1.8.8
            // String ownerUuidString = ((EntityHorse) entity).getOwnerId();
            if (!Strings.isNullOrEmpty(ownerUuidString))
            {
                try
                {
                    if (currentPlayer.getUniqueID().equals(UUID.fromString(ownerUuidString)))
                    {
                        owner = ForgeHelper.INSTANCE.getEntityName(currentPlayer);
                    }
                }
                catch (Throwable t)
                {
                    t.printStackTrace();
                }
            }
        }

        this.owner = owner;

        String customName = null;

        // TODO: Recompare to branch to ensure it matches bugfixes
        boolean passive = false;
        if (entity instanceof EntityLiving)
        {
            EntityLiving entityLiving = (EntityLiving) entity;

            // CustomName
            if (ForgeHelper.INSTANCE.hasCustomName(entity) && entityLiving.getAlwaysRenderNameTag())
            {
                customName = StringUtils.stripControlCodes(((EntityLiving) entity).getCustomNameTag());
            }

            // Hostile check
            if (!hostile && currentPlayer != null)
            {
                EntityLivingBase attackTarget = ((EntityLiving) entity).getAttackTarget();
                if (attackTarget != null && attackTarget.getUniqueID().equals(currentPlayer.getUniqueID()))
                {
                    hostile = true;
                }
            }

            // Passive check

            if (EntityHelper.isPassive((EntityLiving) entity))
            {
                passive = true;
            }
        }
        this.customName = customName;
        this.hostile = hostile;
        this.passiveAnimal = passive;

        // Profession
        if (entity instanceof EntityVillager)
        {
            this.profession = ((EntityVillager) entity).getProfession();
        }
        else
        {
            this.profession = null;
        }
    }

    public static class SimpleCacheLoader extends CacheLoader<EntityLivingBase, EntityDTO>
    {
        @Override
        public EntityDTO load(EntityLivingBase entity) throws Exception
        {
            return new EntityDTO(entity);
        }
    }
}
