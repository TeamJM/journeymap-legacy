/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.option;

import journeymap.client.Constants;
import journeymap.client.properties.PropertiesBase;
import journeymap.client.ui.component.ListPropertyButton;
import journeymap.common.Journeymap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Encapsulates the variations of location formats.
 */
public class LocationFormat
{
    private static String[] locationFormatIds = new String[]{"xzyv", "xyvz", "xzy", "xyz", "xz"};
    private HashMap<String, LocationFormatKeys> idToFormat = new HashMap<String, LocationFormatKeys>();

    public LocationFormat()
    {
        for (String id : locationFormatIds)
        {
            idToFormat.put(id, new LocationFormatKeys(id));
        }
    }

    public LocationFormatKeys getFormatKeys(String id)
    {
        LocationFormatKeys locationLocationFormatKeys = idToFormat.get(id);
        if (locationLocationFormatKeys == null)
        {
            Journeymap.getLogger().warn("Invalid location format id: " + id);
            locationLocationFormatKeys = idToFormat.get(locationFormatIds[0]);
        }

        return locationLocationFormatKeys;
    }

    public String getLabel(String id)
    {
        return Constants.getString(getFormatKeys(id).label_key);
    }

    public static class IdProvider implements StringListProvider
    {
        public IdProvider()
        {
        }

        @Override
        public List<String> getStrings()
        {
            return Arrays.asList(locationFormatIds);
        }

        @Override
        public String getDefaultString()
        {
            return locationFormatIds[0];
        }
    }

    public static class LocationFormatKeys
    {
        final String id;
        final String label_key;
        final String verbose_key;
        final String plain_key;

        LocationFormatKeys(String id)
        {
            this.id = id;
            this.label_key = String.format("jm.common.location_%s_label", id);
            this.verbose_key = String.format("jm.common.location_%s_verbose", id);
            this.plain_key = String.format("jm.common.location_%s_plain", id);
        }

        public String format(boolean verbose, int x, int z, int y, int vslice)
        {
            if (verbose)
            {
                return Constants.getString(verbose_key, x, z, y, vslice);
            }
            else
            {
                return Constants.getString(plain_key, x, z, y, vslice);
            }
        }
    }

    public static class Button extends ListPropertyButton<String>
    {
        LocationFormat locationFormat;

        public Button(PropertiesBase properties, AtomicReference<String> valueHolder)
        {
            super(Arrays.asList(locationFormatIds), Constants.getString("jm.common.location_format"), properties, valueHolder);
            if (locationFormat == null)
            {
                locationFormat = new LocationFormat();
            }
        }

        @Override
        public String getFormattedLabel(String id)
        {
            if (locationFormat == null)
            {
                locationFormat = new LocationFormat();
            }
            return String.format(labelPattern, baseLabel, glyph, locationFormat.getLabel(id));
        }

        public String getLabel(String id)
        {
            return locationFormat.getLabel(id);
        }
    }
}
