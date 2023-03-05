/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.waypoint;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import journeymap.client.JourneymapClient;
import journeymap.client.io.FileHandler;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.Waypoint;
import journeymap.common.Journeymap;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Disk-backed cache for Waypoints.
 */
public class WaypointStore
{
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Cache<String, Waypoint> cache = CacheBuilder.newBuilder().build();
    private final Set<Integer> dimensions = new HashSet<Integer>();
    private boolean loaded = false;

    private WaypointStore()
    {
    }

    public static WaypointStore instance()
    {
        return Holder.INSTANCE;
    }

    private boolean writeToFile(Waypoint waypoint)
    {
        File waypointFile = null;
        try
        {
            // Write to file
            waypointFile = new File(FileHandler.getWaypointDir(), waypoint.getFileName());
            Files.write(gson.toJson(waypoint), waypointFile, Charset.forName("UTF-8"));
            return true;
        }
        catch (Exception e)
        {
            Journeymap.getLogger().error(String.format("Can't save waypoint file %s: %s", waypointFile, LogFormatter.toString(e)));
            return false;
        }
    }

    public Collection<Waypoint> getAll()
    {
        return cache.asMap().values();
    }

    public void add(Waypoint waypoint)
    {
        if (cache.getIfPresent(waypoint.getId()) == null)
        {
            cache.put(waypoint.getId(), waypoint);
        }
    }

    public void save(Waypoint waypoint)
    {
        cache.put(waypoint.getId(), waypoint);
        boolean saved = writeToFile(waypoint);
        if (saved)
        {
            waypoint.setDirty(false);
        }
    }

    public void bulkSave()
    {
        for (Waypoint waypoint : cache.asMap().values())
        {
            if (waypoint.isDirty())
            {
                boolean saved = writeToFile(waypoint);
                if (saved)
                {
                    waypoint.setDirty(false);
                }
            }
        }
    }

    public void remove(Waypoint waypoint)
    {
        cache.invalidate(waypoint.getId());
        File waypointFile = null;
        waypointFile = new File(FileHandler.getWaypointDir(), waypoint.getFileName());
        remove(waypointFile);
    }

    private void remove(File waypointFile)
    {
        try
        {
            waypointFile.delete();
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn(String.format("Can't delete waypoint file %s: %s", waypointFile, e.getMessage()));
            waypointFile.deleteOnExit();
        }
    }

    public void reset()
    {
        cache.invalidateAll();
        dimensions.clear();
        loaded = false;
        if (JourneymapClient.getWaypointProperties().managerEnabled.get())
        {
            load();
        }
    }

    private void load()
    {
        synchronized (cache)
        {
            ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>();
            File waypointDir = null;
            try
            {
                cache.invalidateAll();

                waypointDir = FileHandler.getWaypointDir();
                waypoints.addAll(new JmReader().loadWaypoints(waypointDir));

                load(waypoints, false);

                Journeymap.getLogger().info(String.format("Loaded %s waypoints from %s", cache.size(), waypointDir));
            }
            catch (Exception e)
            {
                Journeymap.getLogger().error(String.format("Error loading waypoints from %s: %s", waypointDir, LogFormatter.toString(e)));
            }
        }
    }

    public void load(Collection<Waypoint> waypoints, boolean forceSave)
    {
        for (Waypoint waypoint : waypoints)
        {
            if (forceSave || (!waypoint.isReadOnly() && waypoint.isDirty()))
            {
                save(waypoint);
            }
            else
            {
                cache.put(waypoint.getId(), waypoint);
            }

            dimensions.addAll(waypoint.getDimensions());
        }
        loaded = true;
    }

    public boolean hasLoaded()
    {
        return loaded;
    }

    public List<Integer> getLoadedDimensions()
    {
        return new ArrayList<Integer>(dimensions);
    }

    private static class Holder
    {
        private static final WaypointStore INSTANCE = new WaypointStore();
    }


}
