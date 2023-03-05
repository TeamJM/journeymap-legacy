/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.option;

import java.util.List;

/**
 * Created by Mark on 9/25/2014.
 */
public interface StringListProvider
{
    public List<String> getStrings();

    public String getDefaultString();
}
