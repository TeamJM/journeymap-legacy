/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.feature;

import java.util.EnumSet;

public enum Feature
{
    RadarPlayers,
    RadarAnimals,
    RadarMobs,
    RadarVillagers,
    MapCaves;

    public static EnumSet<Feature> radar()
    {
        return EnumSet.of(RadarPlayers, RadarAnimals, RadarMobs, RadarVillagers);
    }

    public static EnumSet<Feature> all()
    {
        return EnumSet.allOf(Feature.class);
    }
}
