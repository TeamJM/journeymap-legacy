/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.data;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableMap;
import journeymap.client.JourneymapClient;
import journeymap.client.model.Waypoint;

import java.util.*;

/**
 * Provides game-related properties in a Map.
 *
 * @author techbrew
 */
public class AllData extends CacheLoader<Long, Map>
{
    /**
     * Constructor.
     */
    public AllData()
    {
    }

    @Override
    public Map load(Long since) throws Exception
    {
        DataCache cache = DataCache.instance();
        LinkedHashMap<Key, Object> props = new LinkedHashMap<Key, Object>();

        props.put(Key.world, cache.getWorld(false));
        props.put(Key.player, cache.getPlayer(false));
        props.put(Key.images, new ImagesData(since));

        if (JourneymapClient.getWebMapProperties().showWaypoints.get())
        {
            int currentDimension = cache.getPlayer(false).dimension;
            Collection<Waypoint> waypoints = cache.getWaypoints(false);
            Map<String, Waypoint> wpMap = new HashMap<String, Waypoint>();
            for (Waypoint waypoint : waypoints)
            {
                if (waypoint.getDimensions().contains(currentDimension))
                {
                    wpMap.put(waypoint.getId(), waypoint);
                }
            }
            props.put(Key.waypoints, wpMap);
        }
        else
        {
            props.put(Key.waypoints, Collections.emptyMap());
        }

        if (!WorldData.isHardcoreAndMultiplayer())
        {
            if (JourneymapClient.getWebMapProperties().showAnimals.get() || JourneymapClient.getWebMapProperties().showPets.get())
            {
                props.put(Key.animals, cache.getAnimals(false));
            }
            else
            {
                props.put(Key.animals, Collections.emptyMap());
            }

            if (JourneymapClient.getWebMapProperties().showMobs.get())
            {
                props.put(Key.mobs, cache.getMobs(false));
            }
            else
            {
                props.put(Key.mobs, Collections.emptyMap());
            }

            if (JourneymapClient.getWebMapProperties().showPlayers.get())
            {
                props.put(Key.players, cache.getPlayers(false));
            }
            else
            {
                props.put(Key.players, Collections.emptyMap());
            }

            if (JourneymapClient.getWebMapProperties().showVillagers.get())
            {
                props.put(Key.villagers, cache.getVillagers(false));
            }
            else
            {
                props.put(Key.villagers, Collections.emptyMap());
            }
        }

        return ImmutableMap.copyOf(props);
    }

    /**
     * Return length of time in millis data should be kept.
     */
    public long getTTL()
    {
        return JourneymapClient.getCoreProperties().renderDelay.get() * 2000;
    }

    public static enum Key
    {
        animals,
        images,
        mobs,
        player,
        players,
        villagers,
        waypoints,
        world
    }
}
