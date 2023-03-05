/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;


import com.google.common.base.Objects;
import journeymap.client.JourneymapClient;
import journeymap.client.data.DataCache;
import journeymap.client.feature.Feature;
import journeymap.client.feature.FeatureManager;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.io.FileHandler;
import journeymap.client.log.StatTimer;
import journeymap.client.properties.InGameMapProperties;
import journeymap.client.properties.MapProperties;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.draw.DrawWayPointStep;
import journeymap.client.render.draw.RadarDrawStepFactory;
import journeymap.client.render.draw.WaypointDrawStepFactory;
import journeymap.client.render.map.GridRenderer;
import journeymap.client.task.multi.MapPlayerTask;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapState
{
    public final int minZoom = 0;
    public final int maxZoom = 5;

    // These can be safely changed at will
    public AtomicBoolean follow = new AtomicBoolean(true);

    public String playerLastPos = "0,0"; //$NON-NLS-1$
    StatTimer refreshTimer = StatTimer.get("MapState.refresh");
    StatTimer generateDrawStepsTimer = StatTimer.get("MapState.generateDrawSteps");

    // These must be internally managed
    private MapType preferredMapType;
    private MapType lastMapType;
    private File worldDir = null;
    private long lastRefresh = 0;
    private long lastMapTypeChange = 0;

    private boolean underground = false;

    private boolean caveMappingAllowed = false;
    private List<DrawStep> drawStepList = new ArrayList<DrawStep>();
    private List<DrawWayPointStep> drawWaypointStepList = new ArrayList<DrawWayPointStep>();
    private String playerBiome = "";
    private InGameMapProperties lastMapProperties = null;
    private List<EntityDTO> entityList = new ArrayList<EntityDTO>(32);
    private int lastPlayerChunkX = 0;
    private int lastPlayerChunkZ = 0;
    private boolean highQuality;

    /**
     * Default constructor
     */
    public MapState()
    {
    }

    public void refresh(Minecraft mc, EntityPlayer player, InGameMapProperties mapProperties)
    {
        refreshTimer.start();
        lastMapProperties = mapProperties;
        boolean showCaves = mapProperties.showCaves.get();
        if (lastMapType == null)
        {
            lastMapType = getMapType(showCaves);
        }
        this.caveMappingAllowed = FeatureManager.isAllowed(Feature.MapCaves);
        this.worldDir = FileHandler.getJMWorldDir(mc);

        this.underground = DataCache.getPlayer().underground;
        Integer vSlice = this.underground && showCaves ? player.chunkCoordY : null;
        this.preferredMapType = MapType.from(mapProperties.getPreferredMapType().get(), vSlice, player.dimension);

        lastPlayerChunkX = player.chunkCoordX;
        lastPlayerChunkZ = player.chunkCoordZ;
        highQuality = JourneymapClient.getCoreProperties().tileHighDisplayQuality.get();

        if (player.dimension != this.getCurrentMapType().dimension)
        {
            follow.set(true);
        }
        else
        {
            if (!worldDir.equals(this.worldDir))
            {
                follow.set(true);
            }
            else
            {
                if (lastMapType == null || getMapType(showCaves).isUnderground() != lastMapType.isUnderground())
                {
                    follow.set(true);
                }
            }
        }

        playerBiome = DataCache.getPlayer().biome;

        updateLastRefresh();

        refreshTimer.stop();
    }

    public void setMapType(MapType.Name mapTypeName)
    {
        setMapType(MapType.from(mapTypeName, DataCache.getPlayer()));
    }

    public void toggleMapType()
    {
        EntityDTO player = DataCache.getPlayer();
        MapType currentMapType = getCurrentMapType();
        final EntityLivingBase playerEntity = player.entityLivingRef.get();
        if (playerEntity == null)
        {
            return;
        }
        if (currentMapType.isUnderground())
        {
            if (!ForgeHelper.INSTANCE.hasNoSky(playerEntity))
            {
                lastMapProperties.showCaves.set(false);
                setMapType(MapType.day(player));
            }
            else
            {
                setMapType(MapType.underground(player));
            }
        }
        else if (currentMapType.isDay())
        {
            setMapType(MapType.night(player));
        }
        else if (currentMapType.isNight())
        {
            if (underground && caveMappingAllowed)
            {
                lastMapProperties.showCaves.set(true);
                setMapType(MapType.underground(player));
            }
            else
            {
                setMapType(MapType.day(player));
            }
        }
    }

    public void setMapType(MapType mapType)
    {
        if (!mapType.equals(getCurrentMapType()))
        {
            if (!mapType.isUnderground())
            {
                lastMapProperties.getPreferredMapType().set(mapType.name);
                preferredMapType = mapType;
            }
        }
        lastMapProperties.save();
        setLastMapTypeChange(mapType);
        requireRefresh();
    }

    public MapType getCurrentMapType()
    {
        boolean showCaves = lastMapProperties.showCaves.get();
        return getMapType(showCaves);
    }

    public MapType getMapType(boolean showCaves)
    {
        MapType mapType = null;

        EntityDTO player = DataCache.getPlayer();
        if (ForgeHelper.INSTANCE.hasNoSky(player.entityLivingRef.get()))
        {
            mapType = MapType.underground(player);
        }
        else if (underground && caveMappingAllowed && showCaves)
        {
            mapType = MapType.underground(DataCache.getPlayer());
        }
        else
        {
            if (preferredMapType == null)
            {
                this.preferredMapType = MapType.from(lastMapProperties.getPreferredMapType().get(), DataCache.getPlayer());
            }
            mapType = preferredMapType;
        }

        if (!Objects.equal(mapType, lastMapType))
        {
            setLastMapTypeChange(mapType);
        }

        return mapType;
    }

    public long getLastMapTypeChange()
    {
        return lastMapTypeChange;
    }

    private void setLastMapTypeChange(MapType mapType)
    {
        if (lastMapType != null && !(mapType.isUnderground() && lastMapType.isUnderground()))
        {
            lastMapTypeChange = System.currentTimeMillis();
        }
        this.lastMapType = mapType;
    }

    public boolean isUnderground()
    {
        return underground;
    }

    public File getWorldDir()
    {
        return worldDir;
    }

    public String getPlayerBiome()
    {
        return playerBiome;
    }

    public List<DrawStep> getDrawSteps()
    {
        return drawStepList;
    }

    public List<DrawWayPointStep> getDrawWaypointSteps()
    {
        return drawWaypointStepList;
    }

    public void generateDrawSteps(Minecraft mc, GridRenderer gridRenderer, WaypointDrawStepFactory waypointRenderer, RadarDrawStepFactory radarRenderer, InGameMapProperties mapProperties, float drawScale, boolean checkWaypointDistance)
    {
        generateDrawStepsTimer.start();
        lastMapProperties = mapProperties;

        drawStepList.clear();
        drawWaypointStepList.clear();
        entityList.clear();

        if (mapProperties.zoomLevel.get() == 0)
        {
            drawScale = drawScale * .5f;
        }

        if (FeatureManager.isAllowed(Feature.RadarAnimals))
        {
            if (mapProperties.showAnimals.get() || mapProperties.showPets.get())
            {
                entityList.addAll(DataCache.instance().getAnimals(false).values());
            }
        }
        if (FeatureManager.isAllowed(Feature.RadarVillagers))
        {
            if (mapProperties.showVillagers.get())
            {
                entityList.addAll(DataCache.instance().getVillagers(false).values());
            }
        }
        if (FeatureManager.isAllowed(Feature.RadarMobs))
        {
            if (mapProperties.showMobs.get())
            {
                entityList.addAll(DataCache.instance().getMobs(false).values());
            }
        }

        if (FeatureManager.isAllowed(Feature.RadarPlayers))
        {
            if (mapProperties.showPlayers.get())
            {
                entityList.addAll(DataCache.instance().getPlayers(false).values());
            }
        }

        // Sort to keep named entities last
        if (!entityList.isEmpty())
        {
            Collections.sort(entityList, EntityHelper.entityMapComparator);
            drawStepList.addAll(radarRenderer.prepareSteps(entityList, gridRenderer, drawScale, mapProperties));
        }

        // Draw waypoints
        if (mapProperties.showWaypoints.get())
        {
            boolean showLabel = mapProperties.showWaypointLabels.get();
            drawWaypointStepList.addAll(waypointRenderer.prepareSteps(DataCache.instance().getWaypoints(false), gridRenderer, checkWaypointDistance, showLabel));
        }

        generateDrawStepsTimer.stop();
    }

    public boolean zoomIn()
    {
        if (lastMapProperties.zoomLevel.get() < maxZoom)
        {
            return setZoom(lastMapProperties.zoomLevel.get() + 1);
        }
        return false;
    }

    public boolean zoomOut()
    {
        if (lastMapProperties.zoomLevel.get() > minZoom)
        {
            return setZoom(lastMapProperties.zoomLevel.get() - 1);
        }
        return false;
    }

    public boolean setZoom(int zoom)
    {
        if (zoom > maxZoom || zoom < minZoom || zoom == lastMapProperties.zoomLevel.get())
        {
            return false;
        }
        lastMapProperties.zoomLevel.set(zoom);
        requireRefresh();
        return true;
    }

    public int getZoom()
    {
        return lastMapProperties.zoomLevel.get();
    }

    public void requireRefresh()
    {
        this.lastRefresh = 0;
    }

    public void updateLastRefresh()
    {
        this.lastRefresh = System.currentTimeMillis();
    }

    public boolean shouldRefresh(Minecraft mc, MapProperties mapProperties)
    {
        EntityDTO player = DataCache.getPlayer();

        if (MapPlayerTask.getlastTaskCompleted() - lastRefresh > 500)
        {
            return true;
        }

        if (this.getCurrentMapType().dimension != player.dimension)
        {
            return true;
        }

        if (this.underground != player.underground)
        {
            return true;
        }

        if (this.getCurrentMapType().vSlice != null && (!player.underground || this.getCurrentMapType().vSlice != player.chunkCoordY))
        {
            return true;
        }

        int diffX = Math.abs(lastPlayerChunkX - player.chunkCoordX);
        if (diffX > 2)
        {
            return true; // should happen on a teleport
        }

        int diffZ = Math.abs(lastPlayerChunkZ - player.chunkCoordZ);
        if (diffZ > 2)
        {
            return true; // should happen on a teleport
        }

        if (lastMapProperties == null || !lastMapProperties.equals(mapProperties))
        {
            return true;
        }

        lastPlayerChunkX = player.chunkCoordX;
        lastPlayerChunkZ = player.chunkCoordZ;

        return false;
    }

    public boolean isHighQuality()
    {
        return highQuality;
    }

    public boolean isCaveMappingAllowed()
    {
        return caveMappingAllowed;
    }

    public int getDimension()
    {
        return getCurrentMapType().dimension;
    }
}
