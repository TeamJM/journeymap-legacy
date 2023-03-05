/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties;

import com.google.common.base.Objects;
import journeymap.client.properties.config.Config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static journeymap.client.properties.config.Config.Category.Waypoint;
import static journeymap.client.properties.config.Config.Category.WaypointBeacon;

/**
 * Properties for in-game waypoint management and display.
 */
public class WaypointProperties extends PropertiesBase implements Comparable<WaypointProperties>
{
    @Config(category = Waypoint, master = true, key = "jm.waypoint.enable_manager")
    public final AtomicBoolean managerEnabled = new AtomicBoolean(true);

    @Config(category = WaypointBeacon, master = true, key = "jm.waypoint.enable_beacons")
    public final AtomicBoolean beaconEnabled = new AtomicBoolean(true);

    @Config(category = WaypointBeacon, key = "jm.waypoint.show_texture")
    public final AtomicBoolean showTexture = new AtomicBoolean(true);

    @Config(category = WaypointBeacon, key = "jm.waypoint.show_static_beam")
    public final AtomicBoolean showStaticBeam = new AtomicBoolean(true);

    @Config(category = WaypointBeacon, key = "jm.waypoint.show_rotating_beam")
    public final AtomicBoolean showRotatingBeam = new AtomicBoolean(true);

    @Config(category = WaypointBeacon, key = "jm.waypoint.show_name")
    public final AtomicBoolean showName = new AtomicBoolean(true);

    @Config(category = WaypointBeacon, key = "jm.waypoint.show_distance")
    public final AtomicBoolean showDistance = new AtomicBoolean(true);

    @Config(category = WaypointBeacon, key = "jm.waypoint.auto_hide_label")
    public final AtomicBoolean autoHideLabel = new AtomicBoolean(true);

    @Config(category = WaypointBeacon, key = "jm.waypoint.bold_label", defaultBoolean = false)
    public final AtomicBoolean boldLabel = new AtomicBoolean(false);

    @Config(category = WaypointBeacon, key = "jm.waypoint.font_scale", minValue = 1, maxValue = 3, defaultValue = 2)
    public final AtomicInteger fontScale = new AtomicInteger(2);

    @Config(category = WaypointBeacon, key = "jm.waypoint.texture_size")
    public final AtomicBoolean textureSmall = new AtomicBoolean(true);

    @Config(category = Waypoint, key = "jm.waypoint.max_distance", minValue = 0, maxValue = 10000, defaultValue = 0)
    public final AtomicInteger maxDistance = new AtomicInteger(0);

    @Config(category = Waypoint, key = "jm.waypoint.create_deathpoints")
    public final AtomicBoolean createDeathpoints = new AtomicBoolean(true);

    protected transient final String name = "waypoint";

    @Override
    public String getName()
    {
        return name;
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

        WaypointProperties that = (WaypointProperties) o;
        return 0 == this.compareTo(that);
    }

    @Override
    public int hashCode()
    {
        int result = managerEnabled.hashCode();
        result = 31 * result + beaconEnabled.hashCode();
        result = 31 * result + showTexture.hashCode();
        result = 31 * result + showStaticBeam.hashCode();
        result = 31 * result + showRotatingBeam.hashCode();
        result = 31 * result + showName.hashCode();
        result = 31 * result + showDistance.hashCode();
        result = 31 * result + autoHideLabel.hashCode();
        result = 31 * result + boldLabel.hashCode();
        result = 31 * result + fontScale.hashCode();
        result = 31 * result + textureSmall.hashCode();
        result = 31 * result + maxDistance.hashCode();
        result = 31 * result + createDeathpoints.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("autoHideLabel", autoHideLabel)
                .add("beaconEnabled", beaconEnabled)
                .add("boldLabel", boldLabel)
                .add("createDeathpoints", createDeathpoints)
                .add("fontScale", fontScale)
                .add("managerEnabled", managerEnabled)
                .add("maxDistance", maxDistance)
                .add("name", name)
                .add("showDistance", showDistance)
                .add("showName", showName)
                .add("showRotatingBeam", showRotatingBeam)
                .add("showStaticBeam", showStaticBeam)
                .add("showTexture", showTexture)
                .add("textureSmall", textureSmall)
                .toString();
    }

    @Override
    public int compareTo(WaypointProperties other)
    {
        return Integer.valueOf(this.hashCode()).compareTo(other.hashCode());
    }

}
