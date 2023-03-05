/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.data;

import com.google.common.cache.CacheLoader;
import journeymap.client.JourneymapClient;
import journeymap.client.feature.Feature;
import journeymap.client.feature.FeatureManager;
import journeymap.client.model.EntityDTO;
import journeymap.client.model.EntityHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides game-related properties in a Map.
 *
 * @author techbrew
 */
public class PlayersData extends CacheLoader<Class, Map<String, EntityDTO>>
{
    @Override
    public Map<String, EntityDTO> load(Class aClass) throws Exception
    {
        if (!FeatureManager.isAllowed(Feature.RadarPlayers))
        {
            return new HashMap<String, EntityDTO>();
        }

        List<EntityDTO> list = EntityHelper.getPlayersNearby();
        return EntityHelper.buildEntityIdMap(list, true);
    }

    public long getTTL()
    {
        return JourneymapClient.getCoreProperties().cachePlayersData.get();
    }
}
