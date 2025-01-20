/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.render.draw;

import com.google.common.base.Strings;
import journeymap.client.data.DataCache;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.EntityDTO;
import journeymap.client.properties.InGameMapProperties;
import journeymap.client.render.map.GridRenderer;
import journeymap.client.render.texture.TextureCache;
import journeymap.client.render.texture.TextureImpl;
import journeymap.common.Journeymap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders an entity image in the MapOverlay.
 *
 * @author techbrew
 */
public class RadarDrawStepFactory
{

    public List<DrawStep> prepareSteps(List<EntityDTO> entityDTOs, GridRenderer grid, float drawScale, InGameMapProperties mapProperties)
    {
        final boolean showAnimals = mapProperties.showAnimals.get();
        final boolean showPets = mapProperties.showPets.get();
        final boolean showMobHeading = mapProperties.showMobHeading.get();
        final boolean showPlayerHeading = mapProperties.showPlayerHeading.get();
        final List<DrawStep> drawStepList = new ArrayList<DrawStep>();
        final double entityScale = (double)mapProperties.entityScale.get() / 100;
        final boolean entityBlur = mapProperties.entityBlur.get();

        try
        {
            TextureImpl entityIcon, locatorImg;
            boolean isPlayer, isPet;

            String playername = ForgeHelper.INSTANCE.getEntityName(ForgeHelper.INSTANCE.getClient().thePlayer);
            TextureCache tc = TextureCache.instance();
            String iconSetName = mapProperties.getEntityIconSetName().get();

            for (EntityDTO dto : entityDTOs)
            {
                EntityLivingBase entityLiving = dto.entityLivingRef.get();
                if (entityLiving == null)
                {
                    continue;
                }

                try
                {
                    isPet = !Strings.isNullOrEmpty(dto.owner);

                    if (!showPets && isPet)
                    {
                        continue;
                    }

                    if (!showAnimals && dto.passiveAnimal)
                    {
                        if (!(isPet && showPets))
                        {
                            continue;
                        }
                    }

                    if (grid.getPixel(dto.posX, dto.posZ) != null)
                    {
                        isPlayer = entityLiving instanceof EntityPlayer;

                        // Determine and draw locator
                        if (dto.hostile)
                        {
                            locatorImg = tc.getHostileLocator();
                        }
                        else
                        {
                            if (!Strings.isNullOrEmpty(dto.owner) && playername.equals(dto.owner))
                            {
                                locatorImg = tc.getPetLocator();
                            }
                            else
                            {
                                if (isPlayer)
                                {
                                    locatorImg = tc.getOtherLocator();
                                }
                                else
                                {
                                    locatorImg = tc.getNeutralLocator();
                                }
                            }
                        }

                        // Draw entity icon and label
                        if (isPlayer)
                        {
                            entityIcon = tc.getPlayerSkin(ForgeHelper.INSTANCE.getEntityName(entityLiving));
                            DrawEntityStep drawStep = DataCache.instance().getDrawEntityStep(entityLiving);
                            drawStep.update(false, locatorImg, entityIcon, showPlayerHeading, entityScale, true);
                            drawStepList.add(drawStep);
                        }
                        else
                        {
                            entityIcon = tc.getEntityIconTexture(iconSetName, dto.filename);
                            if (entityIcon != null)
                            {
                                DrawEntityStep drawStep = DataCache.instance().getDrawEntityStep(entityLiving);
                                drawStep.update(false, locatorImg, entityIcon, showMobHeading, entityScale, entityBlur);
                                drawStepList.add(drawStep);
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    Journeymap.getLogger().error("Exception during prepareSteps: " + LogFormatter.toString(e));
                }
            }
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Throwable during prepareSteps: " + LogFormatter.toString(t));
        }

        return drawStepList;
    }

}
