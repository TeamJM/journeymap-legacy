/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.waypoint;

import journeymap.client.ui.component.OnOffButton;
import net.minecraft.client.Minecraft;

/**
 * Created by Mark on 10/12/2014.
 */
class SortButton extends OnOffButton
{
    final WaypointManagerItem.Sort sort;
    final String labelInactive;

    public SortButton(String label, WaypointManagerItem.Sort sort)
    {
        super(String.format("%s %s", label, WaypointManager.ASCEND), String.format("%s %s", label, WaypointManager.DESCEND), sort.ascending);
        this.labelInactive = label;
        this.sort = sort;
    }

    @Override
    public void toggle()
    {
        sort.ascending = !sort.ascending;
        setActive(true);
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY)
    {
        super.drawButton(minecraft, mouseX, mouseY);
        super.drawUnderline();
    }

    public void setActive(boolean active)
    {
        if (active)
        {
            setToggled(sort.ascending);
        }
        else
        {
            displayString = String.format("%s %s", labelInactive, " ");
        }
    }
}
