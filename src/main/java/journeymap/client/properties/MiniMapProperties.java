/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties;

import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.MapType;
import journeymap.client.properties.config.Config;
import journeymap.client.ui.minimap.Orientation;
import journeymap.client.ui.minimap.Position;
import journeymap.client.ui.minimap.ReticleOrientation;
import journeymap.client.ui.minimap.Shape;
import journeymap.client.ui.option.TimeFormat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static journeymap.client.properties.config.Config.Category.Inherit;

/**
 * Properties for the minimap in-game.
 */
public class MiniMapProperties extends InGameMapProperties
{
    @Config(category = Inherit, master = true, key = "jm.minimap.enable_minimap")
    public final AtomicBoolean enabled = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.minimap.shape", defaultEnum = "Circle")
    public final AtomicReference<Shape> shape = new AtomicReference<Shape>(Shape.Circle);

    @Config(category = Inherit, key = "jm.minimap.position", defaultEnum = "TopRight")
    public final AtomicReference<Position> position = new AtomicReference<Position>(Position.TopRight);

    @Config(category = Inherit, key = "jm.common.time_format", stringListProvider = TimeFormat.IdProvider.class)
    public final AtomicReference<String> timeFormat = new AtomicReference<String>(new TimeFormat.IdProvider().getDefaultString());

    @Config(category = Inherit, key = "jm.minimap.show_fps", defaultBoolean = false)
    public final AtomicBoolean showFps = new AtomicBoolean(false);

    @Config(category = Inherit, key = "jm.minimap.show_biome")
    public final AtomicBoolean showBiome = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.minimap.show_time")
    public final AtomicBoolean showTime = new AtomicBoolean(false);

    @Config(category = Inherit, key = "jm.minimap.show_location")
    public final AtomicBoolean showLocation = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.minimap.size", minValue = 1, maxValue = 100, defaultValue = 30)
    public final AtomicInteger sizePercent = new AtomicInteger(30);

    @Config(category = Inherit, key = "jm.minimap.frame_alpha", minValue = 0, maxValue = 100, defaultValue = 100)
    public final AtomicInteger frameAlpha = new AtomicInteger(100);

    @Config(category = Inherit, key = "jm.minimap.terrain_alpha", minValue = 0, maxValue = 100, defaultValue = 100)
    public final AtomicInteger terrainAlpha = new AtomicInteger(100);

    @Config(category = Inherit, key = "jm.minimap.orientation.button", defaultEnum = "PlayerHeading")
    public final AtomicReference<Orientation> orientation = new AtomicReference<Orientation>(Orientation.North);

    @Config(category = Inherit, key = "jm.minimap.compass_font_scale", minValue = 1, maxValue = 4, defaultValue = 1)
    public final AtomicInteger compassFontScale = new AtomicInteger(1);

    @Config(category = Inherit, key = "jm.minimap.show_compass")
    public final AtomicBoolean showCompass = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.minimap.show_reticle")
    public final AtomicBoolean showReticle = new AtomicBoolean(true);

    @Config(category = Inherit, key = "jm.minimap.reticle_orientation", defaultEnum = "Compass")
    public final AtomicReference<ReticleOrientation> reticleOrientation = new AtomicReference<ReticleOrientation>(ReticleOrientation.Compass);

    public final AtomicReference<MapType.Name> preferredMapType = new AtomicReference<MapType.Name>(MapType.Name.day);
    protected transient final String name;
    protected boolean active = false;

    public MiniMapProperties()
    {
        this("minimap");
    }

    protected MiniMapProperties(String name)
    {
        this.name = name;
    }

    @Override
    public AtomicReference<String> getEntityIconSetName()
    {
        return entityIconSetName;
    }

    @Override
    public AtomicReference<MapType.Name> getPreferredMapType()
    {
        return preferredMapType;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
        save();
    }

    public int getId()
    {
        return 1;
    }

    /**
     * Gets the size relative to current screen height.
     *
     * @return
     */
    public int getSize()
    {
        return (int) Math.max(128, Math.floor((sizePercent.get() / 100.0) * ForgeHelper.INSTANCE.getClient().displayHeight));
    }

    @Override
    public void newFileInit()
    {
        this.setActive(true);
        if (ForgeHelper.INSTANCE.getFontRenderer().getUnicodeFlag())
        {
            super.fontScale.set(2);
            compassFontScale.set(2);
        }
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
        if (!super.equals(o))
        {
            return false;
        }

        MiniMapProperties that = (MiniMapProperties) o;
        return 0 == this.compareTo(that);
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (enabled != null ? enabled.hashCode() : 0);
        result = 31 * result + (shape != null ? shape.hashCode() : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        result = 31 * result + (showFps != null ? showFps.hashCode() : 0);
        result = 31 * result + (showBiome != null ? showBiome.hashCode() : 0);
        result = 31 * result + (showTime != null ? showTime.hashCode() : 0);
        result = 31 * result + timeFormat.hashCode();
        result = 31 * result + (showLocation != null ? showLocation.hashCode() : 0);
        result = 31 * result + showWaypointLabels.hashCode();
        result = 31 * result + sizePercent.hashCode();
        result = 31 * result + frameAlpha.hashCode();
        result = 31 * result + terrainAlpha.hashCode();
        result = 31 * result + orientation.hashCode();
        result = 31 * result + compassFontScale.hashCode();
        result = 31 * result + showCompass.hashCode();
        result = 31 * result + showReticle.hashCode();
        result = 31 * result + reticleOrientation.hashCode();
        result = 31 * result + entityIconSetName.hashCode();
        result = 31 * result + preferredMapType.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return super.toStringHelper(this)
                .add("active", active)
                .add("compassFontScale", compassFontScale)
                .add("enabled", enabled)
                .add("frameAlpha", frameAlpha)
                .add("name", name)
                .add("orientation", orientation)
                .add("position", position)
                .add("preferredMapType", preferredMapType)
                .add("reticleOrientation", reticleOrientation)
                .add("shape", shape)
                .add("showBiome", showBiome)
                .add("showTime", showTime)
                .add("timeFormat", timeFormat)
                .add("showCompass", showCompass)
                .add("showFps", showFps)
                .add("showLocation", showLocation)
                .add("showReticle", showReticle)
                .add("sizePercent", sizePercent)
                .add("terrainAlpha", terrainAlpha)
                .toString();
    }


}
