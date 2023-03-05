/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.data;

import journeymap.client.model.RegionCoord;
import journeymap.client.model.RegionImageCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Provides data of what's changed in RegionImageCache in a Map.
 * This provider requires parameters for a valid response.
 *
 * @author techbrew
 */
public class ImagesData
{
    public static final String PARAM_SINCE = "images.since";

    // last query time
    final long since;

    // list of region coords changed {[x][z]}
    final List<Object[]> regions;

    // Last time this was queried
    final long queryTime;

    /**
     * Constructor.
     */
    public ImagesData(Long since)
    {
        final long now = new Date().getTime();
        this.queryTime = now;
        this.since = (since == null) ? now : since;

        List<RegionCoord> dirtyRegions = RegionImageCache.instance().getChangedSince(null, this.since);
        if (dirtyRegions.isEmpty())
        {
            this.regions = Collections.EMPTY_LIST;
        }
        else
        {
            this.regions = new ArrayList<Object[]>(dirtyRegions.size());
            for (RegionCoord rc : dirtyRegions)
            {
                this.regions.add(new Integer[]{rc.regionX, rc.regionZ});
            }
        }
    }
}
