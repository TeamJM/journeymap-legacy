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
 * Encapsulates the variations of time formats.
 */
public class TimeFormat
{
    private static String[] timeFormatIds = new String[]{
        "day_12hr",
        "day_24hr",
        "day_only",
        "time_only_12hr",
        "time_only_24hr"
    };
    private HashMap<String, TimeFormatKeys> idToFormat = new HashMap<String, TimeFormatKeys>();

    public TimeFormat()
    {
        for (String id : timeFormatIds)
        {
            idToFormat.put(id, new TimeFormatKeys(id));
        }
    }

    public TimeFormatKeys getFormatKeys(String id)
    {
        TimeFormatKeys timeTimeFormatKeys = idToFormat.get(id);
        if (timeTimeFormatKeys == null)
        {
            Journeymap.getLogger().warn("Invalid time format id: " + id);
            timeTimeFormatKeys = idToFormat.get(timeFormatIds[0]);
        }

        return timeTimeFormatKeys;
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
            return Arrays.asList(timeFormatIds);
        }

        @Override
        public String getDefaultString()
        {
            return timeFormatIds[0];
        }
    }

    public static class TimeFormatKeys
    {
        final String id;
        final String label_key;
        final String plain_key;

        TimeFormatKeys(String id)
        {
            this.id = id;
            this.label_key = String.format("jm.common.time_%s_label", id);
            this.plain_key = String.format("jm.common.time_%s_plain", id);
        }

        public String format(String days, String hours, String minutes, String am_pm)
        {
            return Constants.getString(plain_key, days, hours, minutes, am_pm);
        }
    }

    public static class Button extends ListPropertyButton<String>
    {
        TimeFormat timeFormat;

        public Button(PropertiesBase properties, AtomicReference<String> valueHolder)
        {
            super(Arrays.asList(timeFormatIds), Constants.getString("jm.common.time_format"), properties, valueHolder);
            if (timeFormat == null)
            {
                timeFormat = new TimeFormat();
            }
        }

        @Override
        public String getFormattedLabel(String id)
        {
            if (timeFormat == null)
            {
                timeFormat = new TimeFormat();
            }
            return String.format(labelPattern, baseLabel, glyph, timeFormat.getLabel(id));
        }

        public String getLabel(String id)
        {
            return timeFormat.getLabel(id);
        }
    }
}
