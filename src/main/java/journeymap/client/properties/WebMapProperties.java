/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties;

import journeymap.client.model.MapType;
import journeymap.client.properties.config.Config;
import journeymap.client.service.MapApiService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static journeymap.client.properties.config.Config.Category.WebMap;

/**
 * Properties for the web map in browser.
 */
public class WebMapProperties extends MapProperties
{
    @Config(category = WebMap, master = true, key = "jm.webmap.enable", defaultBoolean = false)
    public final AtomicBoolean enabled = new AtomicBoolean(false);

    @Config(category = WebMap, key = "jm.advanced.port", minValue = 80, maxValue = 10000, defaultValue = 8080)
    public final AtomicInteger port = new AtomicInteger(8080);

    @Config(category = WebMap, key = "jm.webmap.google_domain", stringListProvider = MapApiService.TopLevelDomains.class)
    public final AtomicReference<String> googleMapApiDomain = new AtomicReference<String>(".com");

    public final AtomicReference<MapType.Name> preferredMapType = new AtomicReference<MapType.Name>(MapType.Name.day);
    protected transient final String name = "webmap";

    public WebMapProperties()
    {
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

        WebMapProperties that = (WebMapProperties) o;
        return 0 == this.compareTo(that);
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + port.hashCode();
        result = 31 * result + enabled.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "WebMapProperties: " +
                ", enabled=" + enabled +
                ", port=" + port +
                ", showMobs=" + showMobs +
                ", showAnimals=" + showAnimals +
                ", showVillagers=" + showVillagers +
                ", showPets=" + showPets +
                ", showPlayers=" + showPlayers +
                ", showWaypoints=" + showWaypoints +
                ", entityIconSetName=" + getEntityIconSetName();
    }


}
