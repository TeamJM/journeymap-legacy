/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties;

import com.google.common.base.Objects;
import journeymap.client.io.IconSetFileHandler;
import journeymap.client.model.MapType;
import journeymap.client.properties.config.Config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static journeymap.client.properties.config.Config.Category.Inherit;

/**
 * Shared Properties for the various map types.
 */
public abstract class MapProperties extends PropertiesBase implements Comparable<MapProperties>
{
    @Config(category = Inherit, key = "jm.common.show_mobs")
    public final AtomicBoolean showMobs = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.common.show_animals")
    public final AtomicBoolean showAnimals = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.common.show_villagers")
    public final AtomicBoolean showVillagers = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.common.show_pets")
    public final AtomicBoolean showPets = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.common.show_players")
    public final AtomicBoolean showPlayers = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.common.show_waypoints")
    public final AtomicBoolean showWaypoints = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.common.show_self")
    public final AtomicBoolean showSelf = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.common.show_grid")
    public final AtomicBoolean showGrid = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.common.mob_icon_set", stringListProvider = IconSetFileHandler.IconSetStringListProvider.class)
    public final AtomicReference<String> entityIconSetName = new AtomicReference<String>("2D");

    @Config(category = Inherit, key = "jm.common.entity_scale", minValue = 50, maxValue = 400, defaultValue = 100)
    public final AtomicInteger entityScale = new AtomicInteger(100);

    @Config(category = Inherit, key = "jm.common.entity_blur")
    public final AtomicBoolean entityBlur = new AtomicBoolean(true);

    public final AtomicInteger zoomLevel = new AtomicInteger(0);

    protected MapProperties()
    {
    }

    public abstract AtomicReference<String> getEntityIconSetName();

    public abstract AtomicReference<MapType.Name> getPreferredMapType();

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

        MapProperties that = (MapProperties) o;
        return this.compareTo(that) == 0;
    }

    @Override
    public int hashCode()
    {
        int result = 31 * showMobs.hashCode();
        result = 31 * result + showAnimals.hashCode();
        result = 31 * result + showVillagers.hashCode();
        result = 31 * result + showPets.hashCode();
        result = 31 * result + showPlayers.hashCode();
        result = 31 * result + showWaypoints.hashCode();
        result = 31 * result + showSelf.hashCode();
        result = 31 * result + getEntityIconSetName().hashCode();
        result = 31 * result + entityScale.hashCode();
        result = 31 * result + entityBlur.hashCode();
        return result;
    }

    @Override
    public int compareTo(MapProperties other)
    {
        return Integer.valueOf(this.hashCode()).compareTo(other.hashCode());
    }

    protected Objects.ToStringHelper toStringHelper(MapProperties me)
    {
        return Objects.toStringHelper(me)
                .add("entityIconSetName", entityIconSetName)
                .add("showAnimals", showAnimals)
                .add("showMobs", showMobs)
                .add("showPets", showPets)
                .add("showPlayers", showPlayers)
                .add("showSelf", showSelf)
                .add("showVillagers", showVillagers)
                .add("showWaypoints", showWaypoints)
                .add("zoomLevel", zoomLevel)
                .add("entityScale", entityScale)
                .add("entityBlur", entityBlur);
    }


}
