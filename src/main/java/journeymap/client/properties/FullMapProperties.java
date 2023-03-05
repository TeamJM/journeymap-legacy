/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties;

import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.MapType;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Properties for the full map in-game.
 */
public class FullMapProperties extends InGameMapProperties
{
    public final AtomicReference<MapType.Name> preferredMapType = new AtomicReference<MapType.Name>(MapType.Name.day);

    protected transient final String name = "fullmap";

    public FullMapProperties()
    {
    }

    @Override
    public void newFileInit()
    {
        if (ForgeHelper.INSTANCE.getFontRenderer().getUnicodeFlag())
        {
            super.fontScale.set(2);
        }
    }

    @Override
    public AtomicReference<String> getEntityIconSetName()
    {
        return entityIconSetName;
    }

    @Override
    public AtomicReference<MapType.Name> getPreferredMapType()
    {
        return preferredMapType;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }

        FullMapProperties that = (FullMapProperties) o;
        return 0 == this.compareTo(that);
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + showGrid.hashCode();
        result = 31 * result + showCaves.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return super.toStringHelper(this)
                .add("preferredMapType", preferredMapType)
                .add("showGrid", showGrid)
                .toString();
    }

}
