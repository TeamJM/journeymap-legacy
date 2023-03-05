/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.waypoint;

import com.google.common.io.Files;
import journeymap.client.model.Waypoint;
import journeymap.common.Journeymap;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Read JM waypoint files and return a collection of the results.
 */
public class JmReader
{
    public Collection<Waypoint> loadWaypoints(File waypointDir)
    {
        ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>();

        File[] files = waypointDir.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".json");
            }
        });

        if (files == null || files.length == 0)
        {
            return waypoints;
        }

        ArrayList<File> obsoleteFiles = new ArrayList<File>();

        for (File waypointFile : files)
        {
            Waypoint wp = load(waypointFile);
            if (wp != null)
            {
                // Check for obsolete filename
                if (!wp.getFileName().endsWith(waypointFile.getName()))
                {
                    wp.setDirty(true);
                    obsoleteFiles.add(waypointFile);
                }
                waypoints.add(wp);
            }
        }

        while (!obsoleteFiles.isEmpty())
        {
            remove(obsoleteFiles.remove(0));
        }

        return waypoints;
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

    private Waypoint load(File waypointFile)
    {
        String waypointString = null;
        Waypoint waypoint = null;
        try
        {
            waypointString = Files.toString(waypointFile, Charset.forName("UTF-8"));
            waypoint = Waypoint.fromString(waypointString);
            return waypoint;
        }
        catch (Throwable e)
        {
            Journeymap.getLogger().error(String.format("Can't load waypoint file %s with contents: %s because %s", waypointFile, waypointString, e.getMessage()));
        }
        return waypoint;
    }
}
