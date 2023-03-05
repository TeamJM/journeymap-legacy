/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.data;

import com.google.common.cache.CacheLoader;
import journeymap.client.JourneymapClient;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.Waypoint;
import journeymap.client.waypoint.WaypointStore;
import journeymap.common.Journeymap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides waypoint data
 *
 * @author techbrew
 */
public class WaypointsData extends CacheLoader<Class, Collection<Waypoint>>
{
    /**
     * Reset state so classes can be checked again. Useful
     * after post-init of all mods.
     */
    public static void enableRecheck()
    {
        // not currently used
    }

    /**
     * Whether native waypoint management is enabled.
     *
     * @return
     */
    public static boolean isManagerEnabled()
    {
        return JourneymapClient.getWaypointProperties().managerEnabled.get();
    }

    /**
     * Get waypoints from whatever sources are supported.
     *
     * @return
     */
    protected static List<journeymap.client.model.Waypoint> getWaypoints()
    {
        ArrayList<Waypoint> list = new ArrayList<journeymap.client.model.Waypoint>(0);

        if (isManagerEnabled())
        {
            list.addAll(WaypointStore.instance().getAll());
        }

        return list;
    }

    /**
     * Check to see whether one or more class names have been classloaded.
     *
     * @param names
     * @return
     */
    private static boolean waypointClassesFound(String... names) throws Exception
    {
        boolean loaded = true;

        for (String name : names)
        {
            if (!loaded)
            {
                break;
            }
            try
            {
                loaded = false;
                Class.forName(name);
                loaded = true;
                Journeymap.getLogger().debug("Class found: " + name);
            }
            catch (NoClassDefFoundError e)
            {
                throw new Exception("Class detected, but is obsolete: " + e.getMessage());
            }
            catch (ClassNotFoundException e)
            {
                Journeymap.getLogger().debug("Class not found: " + name);
            }
            catch (VerifyError v)
            {
                throw new Exception("Class detected, but is obsolete: " + v.getMessage());
            }
            catch (Throwable t)
            {
                throw new Exception("Class detected, but produced errors.", t);
            }
        }

        return loaded;
    }

    @Override
    public Collection<Waypoint> load(Class aClass) throws Exception
    {
        return getWaypoints();
    }

    public long getTTL()
    {
        return 5000;
    }
}
