/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties;

import com.google.common.base.Objects;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.io.ThemeFileHandler;
import journeymap.client.log.JMLogger;
import journeymap.client.model.GridSpecs;
import journeymap.client.properties.config.Config;
import journeymap.client.task.multi.RenderSpec;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static journeymap.client.properties.config.Config.Category.*;

/**
 * Properties for basic mod configuration.
 */
public class CoreProperties extends PropertiesBase implements Comparable<CoreProperties>
{
    @Config(category = Advanced, key = "jm.advanced.loglevel", stringListProvider = JMLogger.LogLevelStringProvider.class)
    public final AtomicReference<String> logLevel = new AtomicReference<String>("INFO");

    @Config(category = Advanced, key = "jm.advanced.automappoll", minValue = 500, maxValue = 10000, defaultValue = 2000)
    public final AtomicInteger autoMapPoll = new AtomicInteger(2000);

    @Config(category = Advanced, key = "jm.advanced.cache_animals", minValue = 1000, maxValue = 10000, defaultValue = 3100)
    public final AtomicInteger cacheAnimalsData = new AtomicInteger(3100);

    @Config(category = Advanced, key = "jm.advanced.cache_mobs", minValue = 1000, maxValue = 10000, defaultValue = 3000)
    public final AtomicInteger cacheMobsData = new AtomicInteger(3000);

    @Config(category = Advanced, key = "jm.advanced.cache_player", minValue = 500, maxValue = 2000, defaultValue = 1000)
    public final AtomicInteger cachePlayerData = new AtomicInteger(1000);

    @Config(category = Advanced, key = "jm.advanced.cache_players", minValue = 1000, maxValue = 10000, defaultValue = 2000)
    public final AtomicInteger cachePlayersData = new AtomicInteger(2000);

    @Config(category = Advanced, key = "jm.advanced.cache_villagers", minValue = 1000, maxValue = 10000, defaultValue = 2200)
    public final AtomicInteger cacheVillagersData = new AtomicInteger(2200);

    @Config(category = Advanced, key = "jm.advanced.announcemod", defaultBoolean = true)
    public final AtomicBoolean announceMod = new AtomicBoolean(true);

    @Config(category = Advanced, key = "jm.advanced.checkupdates", defaultBoolean = true)
    public final AtomicBoolean checkUpdates = new AtomicBoolean(true);

    @Config(category = Advanced, key = "jm.advanced.recordcachestats", defaultBoolean = false)
    public final AtomicBoolean recordCacheStats = new AtomicBoolean(false);

    @Config(category = Advanced, key = "jm.advanced.browserpoll", minValue = 1000, maxValue = 10000, defaultValue = 2000)
    public final AtomicInteger browserPoll = new AtomicInteger(2000);

    @Config(category = FullMap, key = "jm.common.ui_theme", stringListProvider = ThemeFileHandler.ThemeStringListProvider.class)
    public final AtomicReference<String> themeName = new AtomicReference<String>(new ThemeFileHandler.ThemeStringListProvider().getDefaultString());

    @Config(category = Cartography, key = "jm.common.map_style_caveignoreglass", defaultBoolean = true)
    public final AtomicBoolean caveIgnoreGlass = new AtomicBoolean(true);

    @Config(category = Cartography, key = "jm.common.map_style_bathymetry", defaultBoolean = false)
    public final AtomicBoolean mapBathymetry = new AtomicBoolean(false);

    @Config(category = Cartography, key = "jm.common.map_style_transparency", defaultBoolean = true)
    public final AtomicBoolean mapTransparency = new AtomicBoolean(true);

    @Config(category = Cartography, key = "jm.common.map_style_cavelighting", defaultBoolean = true)
    public final AtomicBoolean mapCaveLighting = new AtomicBoolean(true);

    @Config(category = Cartography, key = "jm.common.map_style_antialiasing", defaultBoolean = true)
    public final AtomicBoolean mapAntialiasing = new AtomicBoolean(true);

    @Config(category = Cartography, key = "jm.common.map_style_plantshadows", defaultBoolean = false)
    public final AtomicBoolean mapPlantShadows = new AtomicBoolean(false);

    @Config(category = Cartography, key = "jm.common.map_style_plants", defaultBoolean = false)
    public final AtomicBoolean mapPlants = new AtomicBoolean(false);

    @Config(category = Cartography, key = "jm.common.map_style_crops", defaultBoolean = true)
    public final AtomicBoolean mapCrops = new AtomicBoolean(true);

    @Config(category = Cartography, key = "jm.common.map_style_caveshowsurface", defaultBoolean = true)
    public final AtomicBoolean mapSurfaceAboveCaves = new AtomicBoolean(true);

    @Config(category = Cartography, key = "jm.common.renderdistance_cave_min", minValue = 1, maxValue = 32, defaultValue = 3, sortOrder = 101)
    public final AtomicInteger renderDistanceCaveMin = new AtomicInteger(3);

    @Config(category = Cartography, key = "jm.common.renderdistance_cave_max", minValue = 1, maxValue = 32, defaultValue = 3, sortOrder = 102)
    public final AtomicInteger renderDistanceCaveMax = new AtomicInteger(3);

    @Config(category = Cartography, key = "jm.common.renderdistance_surface_min", minValue = 1, maxValue = 32, defaultValue = 4, sortOrder = 103)
    public final AtomicInteger renderDistanceSurfaceMin = new AtomicInteger(4);

    @Config(category = Cartography, key = "jm.common.renderdistance_surface_max", minValue = 1, maxValue = 32, defaultValue = 7, sortOrder = 104)
    public final AtomicInteger renderDistanceSurfaceMax = new AtomicInteger(7);

    @Config(category = Cartography, key = "jm.common.renderdelay", minValue = 0, maxValue = 10, defaultValue = 2)
    public final AtomicInteger renderDelay = new AtomicInteger(2);

    @Config(category = Cartography, key = "jm.common.revealshape", defaultEnum = "Circle")
    public final AtomicReference<RenderSpec.RevealShape> revealShape = new AtomicReference<RenderSpec.RevealShape>(RenderSpec.RevealShape.Circle);

    @Config(category = Cartography, key = "jm.common.alwaysmapcaves", defaultBoolean = false)
    public final AtomicBoolean alwaysMapCaves = new AtomicBoolean();

    @Config(category = Cartography, key = "jm.common.alwaysmapsurface", defaultBoolean = false)
    public final AtomicBoolean alwaysMapSurface = new AtomicBoolean();

    @Config(category = Cartography, key = "jm.common.tile_display_quality", defaultBoolean = true)
    public final AtomicBoolean tileHighDisplayQuality = new AtomicBoolean(true);

    @Config(category = Advanced, key = "jm.common.radar_max_animals", minValue = 1, maxValue = 128, defaultValue = 32)
    public final AtomicInteger maxAnimalsData = new AtomicInteger(32);

    @Config(category = Advanced, key = "jm.common.radar_max_mobs", minValue = 1, maxValue = 128, defaultValue = 32)
    public final AtomicInteger maxMobsData = new AtomicInteger(32);

    @Config(category = Advanced, key = "jm.common.radar_max_players", minValue = 1, maxValue = 128, defaultValue = 32)
    public final AtomicInteger maxPlayersData = new AtomicInteger(32);

    @Config(category = Advanced, key = "jm.common.radar_max_villagers", minValue = 1, maxValue = 128, defaultValue = 32)
    public final AtomicInteger maxVillagersData = new AtomicInteger(32);

    @Config(category = Advanced, key = "jm.common.radar_hide_sneaking", defaultBoolean = true)
    public final AtomicBoolean hideSneakingEntities = new AtomicBoolean(true);

    @Config(category = Advanced, key = "jm.common.radar_lateral_distance", minValue = 16, maxValue = 512, defaultValue = 64)
    public final AtomicInteger radarLateralDistance = new AtomicInteger(64);

    @Config(category = Advanced, key = "jm.common.radar_vertical_distance", minValue = 8, maxValue = 256, defaultValue = 16)
    public final AtomicInteger radarVerticalDistance = new AtomicInteger(16);

    @Config(category = Advanced, key = "jm.advanced.tile_render_type", minValue = 1, maxValue = 4, defaultValue = 1)
    public final AtomicInteger tileRenderType = new AtomicInteger(1);

    public final GridSpecs gridSpecs = new GridSpecs();
    public final AtomicBoolean mappingEnabled = new AtomicBoolean(true);
    public final AtomicReference<String> renderOverlayEventTypeName = new AtomicReference<String>(RenderGameOverlayEvent.ElementType.ALL.name());
    public final AtomicBoolean renderOverlayPreEvent = new AtomicBoolean(true);
    public final AtomicReference<String> optionsManagerViewed = new AtomicReference<String>("");
    public final AtomicReference<String> splashViewed = new AtomicReference<String>("");

    protected transient final String name = "core";

    public CoreProperties()
    {
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int compareTo(CoreProperties other)
    {
        return Integer.valueOf(this.hashCode()).compareTo(other.hashCode());
    }

    public RenderGameOverlayEvent.ElementType getRenderOverlayEventType()
    {
        return Enum.valueOf(RenderGameOverlayEvent.ElementType.class, renderOverlayEventTypeName.get());
    }

    /**
     * Should return true if save needed after validation.
     *
     * @return
     */
    @Override
    protected boolean validate()
    {
        boolean saveNeeded = super.validate();

        if (renderDistanceCaveMax.get() < renderDistanceCaveMin.get())
        {
            renderDistanceCaveMax.set(renderDistanceCaveMin.get());
            saveNeeded = true;
        }

        if (renderDistanceSurfaceMax.get() < renderDistanceSurfaceMin.get())
        {
            renderDistanceSurfaceMax.set(renderDistanceSurfaceMin.get());
            saveNeeded = true;
        }

        int gameRenderDistance = ForgeHelper.INSTANCE.getClient().gameSettings.renderDistanceChunks;
        for (AtomicInteger prop : Arrays.asList(renderDistanceCaveMin, renderDistanceCaveMax, renderDistanceSurfaceMin, renderDistanceSurfaceMax))
        {
            if (prop.get() > gameRenderDistance)
            {
                prop.set(gameRenderDistance);
                saveNeeded = true;
            }
        }

        return saveNeeded;
    }

    public boolean hasValidCaveRenderDistances()
    {
        return renderDistanceCaveMax.get() >= renderDistanceCaveMin.get();
    }

    public boolean hasValidSurfaceRenderDistances()
    {
        return renderDistanceSurfaceMax.get() >= renderDistanceSurfaceMin.get();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        CoreProperties that = (CoreProperties) o;
        return 0 == that.compareTo(this);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(announceMod, autoMapPoll, browserPoll, cacheAnimalsData, cacheMobsData, cachePlayerData,
                cachePlayersData, cacheVillagersData, caveIgnoreGlass, checkUpdates, renderDelay, hideSneakingEntities,
                logLevel, mapAntialiasing, mapBathymetry, mapCaveLighting, mapCrops, mapPlants, mapPlantShadows,
                mapSurfaceAboveCaves, mapTransparency, maxAnimalsData, maxMobsData, maxPlayersData, maxVillagersData,
                name, radarLateralDistance, radarVerticalDistance, recordCacheStats, renderOverlayEventTypeName,
                renderOverlayPreEvent, renderDistanceCaveMin, renderDistanceCaveMax, renderDistanceSurfaceMin,
                renderDistanceSurfaceMax, revealShape, themeName, gridSpecs);
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("announceMod", announceMod)
                .add("autoMapPoll", autoMapPoll)
                .add("browserPoll", browserPoll)
                .add("cacheAnimalsData", cacheAnimalsData)
                .add("cacheMobsData", cacheMobsData)
                .add("cachePlayerData", cachePlayerData)
                .add("cachePlayersData", cachePlayersData)
                .add("cacheVillagersData", cacheVillagersData)
                .add("caveIgnoreGlass", caveIgnoreGlass)
                .add("isUpdateCheckEnabled", checkUpdates)
                .add("renderDelay", renderDelay)
                .add("hideSneakingEntities", hideSneakingEntities)
                .add("logLevel", logLevel)
                .add("mapAntialiasing", mapAntialiasing)
                .add("mapBathymetry", mapBathymetry)
                .add("mapCaveLighting", mapCaveLighting)
                .add("mapCrops", mapCrops)
                .add("mapPlants", mapPlants)
                .add("mapPlantShadows", mapPlantShadows)
                .add("mapSurfaceAboveCaves", mapSurfaceAboveCaves)
                .add("tileHighDisplayQuality", tileHighDisplayQuality)
                .add("mapTransparency", mapTransparency)
                .add("maxAnimalsData", maxAnimalsData)
                .add("maxMobsData", maxMobsData)
                .add("maxPlayersData", maxPlayersData)
                .add("maxVillagersData", maxVillagersData)
                .add("optionsManagerViewed", optionsManagerViewed)
                .add("radarLateralDistance", radarLateralDistance)
                .add("radarVerticalDistance", radarVerticalDistance)
                .add("recordCacheStats", recordCacheStats)
                .add("renderOverlayEventTypeName", renderOverlayEventTypeName)
                .add("renderOverlayPreEvent", renderOverlayPreEvent)
                .add("renderDistanceCaveMin", renderDistanceCaveMin)
                .add("renderDistanceCaveMax", renderDistanceCaveMax)
                .add("renderDistanceSurfaceMin", renderDistanceSurfaceMin)
                .add("renderDistanceSurfaceMax", renderDistanceSurfaceMax)
                .add("revealShape", revealShape)
                .add("themeName", themeName)
                .add("tileRenderType", tileRenderType)
                .toString();
    }
}
