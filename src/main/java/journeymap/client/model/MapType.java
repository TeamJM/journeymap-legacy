/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

import journeymap.client.data.DataCache;

import java.util.HashMap;

/**
 * Encapsulate irreducible complexity of map type-related properties.
 */
public class MapType
{
    private static HashMap<Integer, MapType> cache = new HashMap<Integer, MapType>(256); // Max 4 names * 8 slices * 8 dimensions
    public final Integer vSlice;
    public final Name name;
    public final int dimension;
    private final int theHashCode;
    private final String theCacheKey;

    public MapType(Name name, Integer vSlice, int dimension)
    {
        // Guarantee surface types don't use a slice
        vSlice = (name != Name.underground) ? null : vSlice;
        this.name = name;
        this.vSlice = vSlice;
        this.dimension = dimension;
        this.theCacheKey = toCacheKey(name, vSlice, dimension);
        this.theHashCode = theCacheKey.hashCode();
    }

    public static MapType from(Name name, Integer vSlice, int dimension)
    {
        return DataCache.instance().getMapType(name, vSlice, dimension);
    }

    public static MapType from(Integer vSlice, int dimension)
    {
        return from(vSlice == null ? Name.surface : Name.underground, vSlice, dimension);
    }

    public static MapType from(Name name, EntityDTO player)
    {
        return from(name, player.chunkCoordY, player.dimension);
    }

    public static MapType day(int dimension)
    {
        return from(Name.day, null, dimension);
    }

    public static MapType day(EntityDTO player)
    {
        return from(Name.day, null, player.dimension);
    }

    public static MapType night(int dimension)
    {
        return from(Name.night, null, dimension);
    }

    public static MapType night(EntityDTO player)
    {
        return from(Name.night, null, player.dimension);
    }

    public static MapType underground(EntityDTO player)
    {
        return from(Name.underground, player.chunkCoordY, player.dimension);
    }

    public static MapType underground(Integer vSlice, int dimension)
    {
        return from(Name.underground, vSlice, dimension);
    }

    public static String toCacheKey(Name name, Integer vSlice, int dimension)
    {
        return "" + dimension + name + vSlice;
    }

    public String toCacheKey()
    {
        return theCacheKey;
    }

    public String toString()
    {
        return name.name();
    }

    public String name()
    {
        return name.name();
    }

    public boolean isUnderground()
    {
        return name == Name.underground;
    }

    public boolean isSurface()
    {
        return name == Name.surface;
    }

    public boolean isDay()
    {
        return name == Name.day;
    }

    public boolean isNight()
    {
        return name == Name.night;
    }

    @Override
    public int hashCode()
    {
        return theHashCode;
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

        MapType mapType = (MapType) o;

        if (dimension != mapType.dimension)
        {
            return false;
        }
        if (name != mapType.name)
        {
            return false;
        }
        if (vSlice != null ? !vSlice.equals(mapType.vSlice) : mapType.vSlice != null)
        {
            return false;
        }

        return true;
    }

    public enum Name
    {
        day, night, underground, surface
    }

}
