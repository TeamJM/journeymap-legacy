/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

import com.google.common.collect.ImmutableSortedMap;
import journeymap.client.JourneymapClient;
import journeymap.client.data.DataCache;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.JMLogger;
import journeymap.client.log.LogFormatter;
import journeymap.client.log.StatTimer;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderFacade;
import net.minecraft.client.renderer.entity.RenderHorse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public class EntityHelper
{
    public static EntityDistanceComparator entityDistanceComparator = new EntityDistanceComparator();
    public static EntityDTODistanceComparator entityDTODistanceComparator = new EntityDTODistanceComparator();
    public static EntityMapComparator entityMapComparator = new EntityMapComparator();
    //private static String[] horseVariantTextures = new String[] {"horse/horse_white.png", "horse/horse_creamy.png", "horse/horse_chestnut.png", "horse/horse_brown.png", "horse/horse_black.png", "horse/horse_gray.png", "/horse/horse_darkbrown.png"};

    public static List<EntityDTO> getEntitiesNearby(String timerName, int maxEntities, boolean hostile, Class... entityClasses)
    {
        StatTimer timer = StatTimer.get("EntityHelper." + timerName);
        timer.start();

        Minecraft mc = ForgeHelper.INSTANCE.getClient();
        List<EntityDTO> list = new ArrayList();

        List<Entity> allEntities = new ArrayList<Entity>(mc.theWorld.loadedEntityList);
        AxisAlignedBB bb = getBB(mc.thePlayer);

        try
        {
            for (Entity entity : allEntities)
            {
                if (entity instanceof EntityLivingBase && !entity.isDead && entity.addedToChunk && bb.intersectsWith(ForgeHelper.INSTANCE.getEntityBoundingBox((EntityLivingBase) entity)))
                {
                    for (Class entityClass : entityClasses)
                    {
                        if (entityClass.isAssignableFrom(entity.getClass()))
                        {
                            EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
                            EntityDTO dto = DataCache.instance().getEntityDTO(entityLivingBase);
                            dto.update(entityLivingBase, hostile);
                            list.add(dto);
                            break;
                        }
                    }
                }
            }

            if (list.size() > maxEntities)
            {
                int before = list.size();
                entityDTODistanceComparator.player = mc.thePlayer;
                Collections.sort(list, entityDTODistanceComparator);
                list = list.subList(0, maxEntities);
            }
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().warn("Failed to " + timerName + ": " + LogFormatter.toString(t));
        }

        timer.stop();
        return list;
    }

    public static List<EntityDTO> getMobsNearby()
    {
        return getEntitiesNearby("getMobsNearby", JourneymapClient.getCoreProperties().maxMobsData.get(), true, IMob.class);
    }

    public static List<EntityDTO> getVillagersNearby()
    {
        return getEntitiesNearby("getVillagersNearby", JourneymapClient.getCoreProperties().maxVillagersData.get(), false, EntityVillager.class);
    }

    public static List<EntityDTO> getAnimalsNearby()
    {
        return getEntitiesNearby("getAnimalsNearby", JourneymapClient.getCoreProperties().maxAnimalsData.get(), false, EntityAnimal.class, EntityGolem.class, EntityWaterMob.class);
    }

    public static boolean isPassive(EntityLiving entityLiving)
    {
        if (entityLiving == null)
        {
            return false;
        }

        if (entityLiving instanceof IMob)
        {
            return false;
        }

        if (entityLiving.getAttackTarget() != null)
        {
            return false;
        }

        return true;
    }

    /**
     * Get nearby non-player entities
     *
     * @return
     */
    public static List<EntityDTO> getPlayersNearby()
    {
        StatTimer timer = StatTimer.get("EntityHelper.getPlayersNearby");
        timer.start();

        Minecraft mc = ForgeHelper.INSTANCE.getClient();
        List<EntityPlayer> allPlayers = new ArrayList<EntityPlayer>(mc.theWorld.playerEntities);
        allPlayers.remove(mc.thePlayer);

        int max = JourneymapClient.getCoreProperties().maxPlayersData.get();
        if (allPlayers.size() > max)
        {
            entityDistanceComparator.player = mc.thePlayer;
            Collections.sort(allPlayers, entityDistanceComparator);
            allPlayers = allPlayers.subList(0, max);
        }

        List<EntityDTO> playerDTOs = new ArrayList<EntityDTO>(allPlayers.size());
        for (EntityPlayer player : allPlayers)
        {
            EntityDTO dto = DataCache.instance().getEntityDTO(player);
            dto.update(player, false);
            playerDTOs.add(dto);
        }

        timer.stop();
        return playerDTOs;
    }


    /**
     * Get a boundingbox to search nearby player.
     *
     * @param player
     * @return
     */
    private static AxisAlignedBB getBB(EntityPlayerSP player)
    {
        int lateralDistance = JourneymapClient.getCoreProperties().radarLateralDistance.get();
        int verticalDistance = JourneymapClient.getCoreProperties().radarVerticalDistance.get();
        return ForgeHelper.INSTANCE.getBoundingBox(player, lateralDistance, verticalDistance);
    }

    /**
     * Put entities into map, preserving the order, using entityId as key
     *
     * @param list
     * @return
     */
    public static Map<String, EntityDTO> buildEntityIdMap(List<? extends EntityDTO> list, boolean sort)
    {
        if (list == null || list.isEmpty())
        {
            return Collections.emptyMap();
        }

        // Sort to keep named entities last.  (Why? display on top of others?)
        if (sort)
        {
            Collections.sort(list, new EntityHelper.EntityMapComparator());
        }

        LinkedHashMap<String, EntityDTO> idMap = new LinkedHashMap<String, EntityDTO>(list.size());
        for (EntityDTO entityMap : list)
        {
            idMap.put("id" + entityMap.entityId, entityMap);
        }
        return ImmutableSortedMap.copyOf(idMap);
    }


    /**
     * Get the simple name of the entity (without Entity prefix)
     *
     * @param entity
     * @return
     */
    public static String getFileName(Entity entity)
    {

        Render entityRender = ForgeHelper.INSTANCE.getRenderManager().getEntityRenderObject(entity);

        // Manually handle horses
        if (entityRender instanceof RenderHorse)
        {
            EntityHorse horse = ((EntityHorse) entity);
            final int type = ((EntityHorse) entity).getHorseType();
            switch (type)
            {
                case 1:
                    return "horse/donkey.png";

                case 2:
                    return "horse/mule.png";

                case 3:
                    return "horse/horse_zombie.png";

                case 4:
                    return "horse/horse_skeleton.png";
                case 0:
                {
                    String variantTexture = horse.getVariantTexturePaths()[0];
                    if (variantTexture.startsWith("textures/entity/"))
                    {
                        return variantTexture.split("textures/entity/")[1];
                    }
                }
                default:
                    return "horse/horse_brown.png";
            }
        }

        // Non-horse mobs
        ResourceLocation loc = RenderFacade.getEntityTexture(entityRender, entity);
        if (loc == null)
        {
            JMLogger.logOnce("Can't get entityTexture for " + entity.getClass() + " via " + entityRender.getClass(), null);
            return null;
        }
        if (loc.getResourceDomain().equals("minecraft"))
        {
            String tex = loc.getResourcePath();
            String search = "/entity/";
            int i = tex.lastIndexOf(search);
            if (i >= 0)
            {
                tex = tex.substring(i + search.length());
            }
            return tex;
        }
        else
        {
            return loc.getResourceDomain() + "/" + loc.getResourcePath();
        }
    }

    private static class EntityMapComparator implements Comparator<EntityDTO>
    {

        @Override
        public int compare(EntityDTO o1, EntityDTO o2)
        {

            Integer o1rank = 0;
            Integer o2rank = 0;

            if (o1.customName != null)
            {
                o1rank++;
            }
            else
            {
                if (o1.username != null)
                {
                    o1rank += 2;
                }
            }

            if (o2.customName != null)
            {
                o2rank++;
            }
            else
            {
                if (o2.username != null)
                {
                    o2rank += 2;
                }
            }

            return o1rank.compareTo(o2rank);
        }

    }

    private static class EntityDistanceComparator implements Comparator<Entity>
    {
        EntityPlayer player;

        @Override
        public int compare(Entity o1, Entity o2)
        {
            return Double.compare(o1.getDistanceSqToEntity(player), o2.getDistanceSqToEntity(player));
        }
    }

    private static class EntityDTODistanceComparator implements Comparator<EntityDTO>
    {
        EntityPlayer player;

        @Override
        public int compare(EntityDTO o1, EntityDTO o2)
        {
            EntityLivingBase e1 = o1.entityLivingRef.get();
            EntityLivingBase e2 = o2.entityLivingRef.get();
            if (e1 == null || e2 == null)
            {
                return 0;
            }
            return Double.compare(e1.getDistanceSqToEntity(player), e2.getDistanceSqToEntity(player));
        }
    }

}
