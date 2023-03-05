/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.data;

import com.google.common.cache.*;
import journeymap.client.JourneymapClient;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.*;
import journeymap.client.render.draw.DrawEntityStep;
import journeymap.client.render.draw.DrawWayPointStep;
import journeymap.client.waypoint.WaypointStore;
import journeymap.common.Journeymap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.ChunkCoordIntPair;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * The mother of all cache managers.
 *
 * @author techbrew
 */
public class DataCache
{
    final LoadingCache<Long, Map> all;
    final LoadingCache<Class, Map<String, EntityDTO>> animals;
    final LoadingCache<Class, Map<String, EntityDTO>> mobs;
    final LoadingCache<Class, Map<String, EntityDTO>> players;
    final LoadingCache<Class, Map<String, EntityDTO>> villagers;
    final LoadingCache<Class, Collection<Waypoint>> waypoints;
    final LoadingCache<Class, EntityDTO> player;
    final LoadingCache<Class, WorldData> world;
    final LoadingCache<RegionImageSet.Key, RegionImageSet> regionImageSets;
    final LoadingCache<Class, Map<String, Object>> messages;
    final LoadingCache<EntityLivingBase, DrawEntityStep> entityDrawSteps;
    final LoadingCache<Waypoint, DrawWayPointStep> waypointDrawSteps;
    final LoadingCache<EntityLivingBase, EntityDTO> entityDTOs;
    final Cache<String, RegionCoord> regionCoords;
    final Cache<String, MapType> mapTypes;
    final LoadingCache<ChunkCoordIntPair, ChunkMD> chunkMetadata;
    final ProxyRemovalListener<ChunkCoordIntPair, ChunkMD> chunkMetadataRemovalListener;
    final HashMap<Cache, String> managedCaches = new HashMap<Cache, String>();
    final WeakHashMap<Cache, String> privateCaches = new WeakHashMap<Cache, String>();
    private final int chunkCacheExpireSeconds = 30;
    private final int defaultConcurrencyLevel = 1;


    // Private constructor
    private DataCache()
    {
        AllData allData = new AllData();
        all = getCacheBuilder().maximumSize(1).expireAfterWrite(allData.getTTL(), TimeUnit.MILLISECONDS).build(allData);
        managedCaches.put(all, "AllData (web)");

        AnimalsData animalsData = new AnimalsData();
        animals = getCacheBuilder().expireAfterWrite(animalsData.getTTL(), TimeUnit.MILLISECONDS).build(animalsData);
        managedCaches.put(animals, "Animals");

        MobsData mobsData = new MobsData();
        mobs = getCacheBuilder().expireAfterWrite(mobsData.getTTL(), TimeUnit.MILLISECONDS).build(mobsData);
        managedCaches.put(mobs, "Mobs");

        PlayerData playerData = new PlayerData();
        player = getCacheBuilder().expireAfterWrite(playerData.getTTL(), TimeUnit.MILLISECONDS).build(playerData);
        managedCaches.put(player, "Player");

        PlayersData playersData = new PlayersData();
        players = getCacheBuilder().expireAfterWrite(playersData.getTTL(), TimeUnit.MILLISECONDS).build(playersData);
        managedCaches.put(players, "Players");

        VillagersData villagersData = new VillagersData();
        villagers = getCacheBuilder().expireAfterWrite(villagersData.getTTL(), TimeUnit.MILLISECONDS).build(villagersData);
        managedCaches.put(villagers, "Villagers");

        WaypointsData waypointsData = new WaypointsData();
        waypoints = getCacheBuilder().expireAfterWrite(waypointsData.getTTL(), TimeUnit.MILLISECONDS).build(waypointsData);
        managedCaches.put(waypoints, "Waypoints");

        WorldData worldData = new WorldData();
        world = getCacheBuilder().expireAfterWrite(worldData.getTTL(), TimeUnit.MILLISECONDS).build(worldData);
        managedCaches.put(world, "World");

        MessagesData messagesData = new MessagesData();
        messages = getCacheBuilder().expireAfterWrite(messagesData.getTTL(), TimeUnit.MILLISECONDS).build(messagesData);
        managedCaches.put(messages, "Messages (web)");

        entityDrawSteps = getCacheBuilder().weakKeys().build(new DrawEntityStep.SimpleCacheLoader());
        managedCaches.put(entityDrawSteps, "DrawEntityStep");

        waypointDrawSteps = getCacheBuilder().weakKeys().build(new DrawWayPointStep.SimpleCacheLoader());
        managedCaches.put(waypointDrawSteps, "DrawWaypointStep");

        entityDTOs = getCacheBuilder().weakKeys().build(new EntityDTO.SimpleCacheLoader());
        managedCaches.put(entityDTOs, "EntityDTO");

        regionImageSets = RegionImageCache.initRegionImageSetsCache(getCacheBuilder());
        managedCaches.put(regionImageSets, "RegionImageSet");

        chunkMetadataRemovalListener = new ProxyRemovalListener<ChunkCoordIntPair, ChunkMD>();
        chunkMetadata = getCacheBuilder().expireAfterAccess(chunkCacheExpireSeconds, TimeUnit.SECONDS).removalListener(chunkMetadataRemovalListener).build(new ChunkMD.SimpleCacheLoader());
        managedCaches.put(chunkMetadata, "ChunkMD");

        regionCoords = getCacheBuilder().expireAfterAccess(chunkCacheExpireSeconds, TimeUnit.SECONDS).build();
        managedCaches.put(regionCoords, "RegionCoord");

        mapTypes = getCacheBuilder().build();
        managedCaches.put(mapTypes, "MapType");
    }

    // Get singleton instance.  Concurrency-safe.
    public static DataCache instance()
    {
        return Holder.INSTANCE;
    }

    public static EntityDTO getPlayer()
    {
        return instance().getPlayer(false);
    }

    /**
     * Get a CacheBuilder with common configuration already set.
     *
     * @return
     */
    private CacheBuilder<Object, Object> getCacheBuilder()
    {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder.concurrencyLevel(defaultConcurrencyLevel);
        if (JourneymapClient.getCoreProperties().recordCacheStats.get())
        {
            builder.recordStats();
        }
        return builder;
    }

    public void addPrivateCache(String name, Cache cache)
    {
        if (privateCaches.containsValue(name))
        {
            Journeymap.getLogger().warn("Overriding private cache: " + name);
        }
        privateCaches.put(cache, name);
    }

    public Cache getPrivateCache(String name)
    {
        for (Map.Entry<Cache, String> entry : privateCaches.entrySet())
        {
            if (entry.getValue().equals(name))
            {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map getAll(long since)
    {
        synchronized (all)
        {
            try
            {
                return all.get(since);
            }
            catch (ExecutionException e)
            {
                Journeymap.getLogger().error("ExecutionException in getAll: " + LogFormatter.toString(e));
                return Collections.EMPTY_MAP;
            }
        }
    }

    public Map<String, EntityDTO> getAnimals(boolean forceRefresh)
    {
        synchronized (animals)
        {
            try
            {
                if (forceRefresh)
                {
                    animals.invalidateAll();
                }
                return animals.get(AnimalsData.class);
            }
            catch (ExecutionException e)
            {
                Journeymap.getLogger().error("ExecutionException in getAnimals: " + LogFormatter.toString(e));
                return Collections.EMPTY_MAP;
            }
        }
    }

    public Map<String, EntityDTO> getMobs(boolean forceRefresh)
    {
        synchronized (mobs)
        {
            try
            {
                if (forceRefresh)
                {
                    mobs.invalidateAll();
                }
                return mobs.get(MobsData.class);
            }
            catch (ExecutionException e)
            {
                Journeymap.getLogger().error("ExecutionException in getMobs: " + LogFormatter.toString(e));
                return Collections.EMPTY_MAP;
            }
        }
    }

    public Map<String, EntityDTO> getPlayers(boolean forceRefresh)
    {
        synchronized (players)
        {
            try
            {
                if (forceRefresh)
                {
                    players.invalidateAll();
                }
                return players.get(PlayersData.class);
            }
            catch (ExecutionException e)
            {
                Journeymap.getLogger().error("ExecutionException in getPlayers: " + LogFormatter.toString(e));
                return Collections.EMPTY_MAP;
            }
        }
    }

    public EntityDTO getPlayer(boolean forceRefresh)
    {
        synchronized (player)
        {
            try
            {
                if (forceRefresh)
                {
                    player.invalidateAll();
                }
                return player.get(PlayerData.class);
            }
            catch (Exception e)
            {
                Journeymap.getLogger().error("ExecutionException in getPlayer: " + LogFormatter.toString(e));
                return null;
            }
        }
    }

    public Map<String, EntityDTO> getVillagers(boolean forceRefresh)
    {
        synchronized (villagers)
        {
            try
            {
                if (forceRefresh)
                {
                    villagers.invalidateAll();
                }
                return villagers.get(VillagersData.class);
            }
            catch (ExecutionException e)
            {
                Journeymap.getLogger().error("ExecutionException in getVillagers: " + LogFormatter.toString(e));
                return Collections.EMPTY_MAP;
            }
        }
    }

    public MapType getMapType(MapType.Name name, Integer vSlice, int dimension)
    {
        // Guarantee surface types don't use a slice
        vSlice = (name != MapType.Name.underground) ? null : vSlice;

        MapType mapType = mapTypes.getIfPresent(MapType.toCacheKey(name, vSlice, dimension));
        if (mapType == null)
        {
            mapType = new MapType(name, vSlice, dimension);
            mapTypes.put(mapType.toCacheKey(), mapType);
        }
        return mapType;
    }

    public Collection<Waypoint> getWaypoints(boolean forceRefresh)
    {
        synchronized (waypoints)
        {
            if (WaypointsData.isManagerEnabled())
            {
                // The store is the cache
                return WaypointStore.instance().getAll();
            }
            else
            {
                return Collections.EMPTY_LIST;
            }
        }
    }

    public Map<String, Object> getMessages(boolean forceRefresh)
    {
        synchronized (messages)
        {
            try
            {
                if (forceRefresh)
                {
                    messages.invalidateAll();
                }
                return messages.get(MessagesData.class);
            }
            catch (ExecutionException e)
            {
                Journeymap.getLogger().error("ExecutionException in getMessages: " + LogFormatter.toString(e));
                return Collections.EMPTY_MAP;
            }
        }
    }

    public WorldData getWorld(boolean forceRefresh)
    {
        synchronized (world)
        {
            try
            {
                if (forceRefresh)
                {
                    world.invalidateAll();
                }
                return world.get(WorldData.class);
            }
            catch (ExecutionException e)
            {
                Journeymap.getLogger().error("ExecutionException in getWorld: " + LogFormatter.toString(e));
                return new WorldData();
            }
        }
    }

    /**
     * Purge radar caches to ensure changed options are immediately seen.
     */
    public void resetRadarCaches()
    {
        animals.invalidateAll();
        mobs.invalidateAll();
        players.invalidateAll();
        villagers.invalidateAll();
        entityDrawSteps.invalidateAll();
        entityDTOs.invalidateAll();
    }

    public DrawEntityStep getDrawEntityStep(EntityLivingBase entity)
    {
        synchronized (entityDrawSteps)
        {
            return entityDrawSteps.getUnchecked(entity);
        }
    }

    public EntityDTO getEntityDTO(EntityLivingBase entity)
    {
        synchronized (entityDTOs)
        {
            return entityDTOs.getUnchecked(entity);
        }
    }

    public DrawWayPointStep getDrawWayPointStep(Waypoint waypoint)
    {
        synchronized (waypointDrawSteps)
        {
            return waypointDrawSteps.getUnchecked(waypoint);
        }
    }
//    public RGB getColor(Color color)
//    {
//        return getColor(color.getRGB());
//    }
//
//    public RGB getColor(int rgbInt)
//    {
//        synchronized (colors)
//        {
//            return colors.getUnchecked(rgbInt);
//        }
//    }

    public ChunkMD getChunkMD(ChunkCoordIntPair coord)
    {
        synchronized (chunkMetadata)
        {
            ChunkMD chunkMD = null;

            try
            {
                chunkMD = chunkMetadata.getUnchecked(coord);
                if (chunkMD != null && !chunkMD.hasChunk())
                {
                    chunkMetadata.invalidate(coord);
                    chunkMD = null;
                }
            }
            catch (CacheLoader.InvalidCacheLoadException e)
            {
                return null;
            }
            catch (Throwable e)
            {
                Journeymap.getLogger().warn("Unexpected error getting ChunkMD from cache: " + e);
            }

            return chunkMD;
        }
    }

    public void addChunkMD(ChunkMD chunkMD)
    {
        synchronized (chunkMetadata)
        {
            chunkMetadata.put(chunkMD.getCoord(), chunkMD);
        }
    }

    public Set<ChunkCoordIntPair> getCachedChunkCoordinates()
    {
        synchronized (chunkMetadata)
        {
            return chunkMetadata.asMap().keySet();
        }
    }

    public void invalidateChunkMD(ChunkCoordIntPair coord)
    {
        synchronized (chunkMetadata)
        {
            chunkMetadata.invalidate(coord);
        }
    }

    public void invalidateChunkMDCache()
    {
        synchronized (chunkMetadata)
        {
            chunkMetadata.invalidateAll();
        }
    }

    public void addChunkMDListener(RemovalListener<ChunkCoordIntPair, ChunkMD> listener)
    {
        synchronized (chunkMetadataRemovalListener)
        {
            chunkMetadataRemovalListener.addDelegateListener(listener);
        }
    }

//    public void resetBlockMetadata()
//    {
//        BlockMD.reset();
//    }

    public LoadingCache<RegionImageSet.Key, RegionImageSet> getRegionImageSets()
    {
        return regionImageSets;
    }

    public Cache<String, RegionCoord> getRegionCoords()
    {
        return regionCoords;
    }

    /**
     * Empties the cache
     */
    public void purge()
    {
        // Flush images, do syncronously to ensure it's done before cache invalidates
        RegionImageCache.instance().flushToDisk(false);

        synchronized (managedCaches)
        {
            for (Cache cache : managedCaches.keySet())
            {
                try
                {
                    cache.invalidateAll();
                }
                catch (Exception e)
                {
                    Journeymap.getLogger().warn("Couldn't purge managed cache: " + cache);
                }
            }
        }

        synchronized (privateCaches)
        {
            for (Cache cache : privateCaches.keySet())
            {
                try
                {
                    cache.invalidateAll();
                }
                catch (Exception e)
                {
                    Journeymap.getLogger().warn("Couldn't purge private cache: " + cache);
                }
            }

            privateCaches.clear();
        }
    }

    public String getDebugHtml()
    {
        StringBuffer sb = new StringBuffer();
        if (JourneymapClient.getCoreProperties().recordCacheStats.get())
        {
            appendDebugHtml(sb, "Managed Caches", managedCaches);
            appendDebugHtml(sb, "Private Caches", privateCaches);
        }
        else
        {
            sb.append("<b>Cache stat recording disabled.  Set config/journeymap.core.config 'recordCacheStats' to 1.</b>");
        }
        return sb.toString();
    }

    private void appendDebugHtml(StringBuffer sb, String name, Map<Cache, String> cacheMap)
    {
        ArrayList<Map.Entry<Cache, String>> list = new ArrayList<Map.Entry<Cache, String>>(cacheMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Cache, String>>()
        {
            @Override
            public int compare(Map.Entry<Cache, String> o1, Map.Entry<Cache, String> o2)
            {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        sb.append("<b>").append(name).append(":</b>");
        sb.append("<pre>");
        for (Map.Entry<Cache, String> entry : list)
        {
            sb.append(toString(entry.getValue(), entry.getKey()));
        }
        sb.append("</pre>");
    }

    private String toString(String label, Cache cache)
    {
        double avgLoadMillis = 0;
        CacheStats cacheStats = cache.stats();
        if (cacheStats.totalLoadTime() > 0 && cacheStats.loadSuccessCount() > 0)
        {
            avgLoadMillis = TimeUnit.NANOSECONDS.toMillis(cacheStats.totalLoadTime()) * 1.0 / cacheStats.loadSuccessCount();
        }

        return String.format("%s<b>%20s:</b> Size: %9s, Hits: %9s, Misses: %9s, Loads: %9s, Errors: %9s, Avg Load Time: %1.2fms", LogFormatter.LINEBREAK, label, cache.size(), cacheStats.hitCount(), cacheStats.missCount(), cacheStats.loadCount(), cacheStats.loadExceptionCount(), avgLoadMillis);
    }

    // On-demand-holder for instance
    private static class Holder
    {
        private static final DataCache INSTANCE = new DataCache();
    }

    class ProxyRemovalListener<K, V> implements RemovalListener<K, V>
    {
        final Map<RemovalListener<K, V>, Void> delegates = Collections.synchronizedMap(new WeakHashMap<RemovalListener<K, V>, Void>());

        void addDelegateListener(RemovalListener<K, V> delegate)
        {
            if (delegates.containsKey(delegate))
            {
                Journeymap.getLogger().warn("RemovalListener already added: " + delegate.getClass());
            }
            else
            {
                delegates.put(delegate, null);
            }
        }

        void removeDelegateListener(RemovalListener<K, V> delegate)
        {
            delegates.remove(delegate);
        }

        @Override
        public void onRemoval(RemovalNotification<K, V> notification)
        {
            for (RemovalListener<K, V> delegate : delegates.keySet())
            {
                delegate.onRemoval(notification);
            }
        }
    }
}
