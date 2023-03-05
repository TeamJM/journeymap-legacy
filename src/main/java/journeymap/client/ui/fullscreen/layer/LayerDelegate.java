/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.fullscreen.layer;

import journeymap.client.log.LogFormatter;
import journeymap.client.model.BlockCoordIntPair;
import journeymap.client.render.draw.DrawStep;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Delegates mouse actions in MapOverlay to Layer impls.
 */
public class LayerDelegate
{

    private List<DrawStep> drawSteps = new ArrayList<DrawStep>();
    private List<Layer> layers = new ArrayList<Layer>();

    public LayerDelegate()
    {
        layers.add(new BlockInfoLayer());
        layers.add(new WaypointLayer());
    }

    public void onMouseMove(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord)
    {
        drawSteps.clear();
        for (Layer layer : layers)
        {
            try
            {
                drawSteps.addAll(layer.onMouseMove(mc, mouseX, mouseY, gridWidth, gridHeight, blockCoord));
            }
            catch (Exception e)
            {
                Journeymap.getLogger().error(LogFormatter.toString(e));
            }
        }
    }

    public void onMouseClicked(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord, int mouseButton)
    {
        drawSteps.clear();
        for (Layer layer : layers)
        {
            try
            {
                drawSteps.addAll(layer.onMouseClick(mc, mouseX, mouseY, gridWidth, gridHeight, blockCoord));
            }
            catch (Exception e)
            {
                Journeymap.getLogger().error(LogFormatter.toString(e));
            }
        }
    }

    public List<DrawStep> getDrawSteps()
    {
        return drawSteps;
    }

    public interface Layer
    {
        public List<DrawStep> onMouseMove(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord);

        public List<DrawStep> onMouseClick(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord);
    }

}
