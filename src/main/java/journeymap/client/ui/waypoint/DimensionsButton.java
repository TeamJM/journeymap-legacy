/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.waypoint;

import journeymap.client.Constants;
import journeymap.client.data.WorldData;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.ui.component.Button;
import journeymap.client.waypoint.WaypointStore;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.world.WorldProvider;

import java.util.List;

/**
 * Created by Mark on 10/12/2014.
 */
class DimensionsButton extends Button
{
    private static boolean allDimSelected;
    private WorldProvider currentWorldProvider;
    private final List<WorldProvider> worldProviders = WorldData.getDimensionProviders(WaypointStore.instance().getLoadedDimensions());

    public DimensionsButton()
    {
        super(0, 0, "");
        if (!allDimSelected)
        {
            this.setCurrentWorldProvider(ForgeHelper.INSTANCE.getClient().thePlayer.worldObj.provider);
        }
        updateLabel();
        // Determine width
        fitWidth(ForgeHelper.INSTANCE.getFontRenderer());
    }

    protected void updateLabel()
    {
        String dimName;

        if (currentWorldProvider != null)
        {
            dimName = WorldData.getSafeDimensionName(currentWorldProvider);
        }
        else
        {
            dimName = Constants.getString("jm.waypoint.dimension_all");
        }
        displayString = Constants.getString("jm.waypoint.dimension", dimName);
    }

    @Override
    public int getFitWidth(FontRenderer fr)
    {
        int maxWidth = 0;
        for (WorldProvider worldProvider : worldProviders)
        {
            String name = Constants.getString("jm.waypoint.dimension", WorldData.getSafeDimensionName(worldProvider));
            maxWidth = Math.max(maxWidth, ForgeHelper.INSTANCE.getFontRenderer().getStringWidth(name));
        }
        return maxWidth + 12;
    }


    private void setCurrentWorldProvider(WorldProvider provider)
    {
        this.currentWorldProvider = provider;
        allDimSelected = provider == null;
    }

    void selectProvider(WorldProvider provider)
    {
        this.setCurrentWorldProvider(provider);
        updateLabel();
    }

    List<WorldProvider> getWorldProviders()
    {
        return worldProviders;
    }

    WorldProvider getCurrentWorldProvider()
    {
        return currentWorldProvider;
    }
}
