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
    private List<DrawStep> mapDrawSteps = new ArrayList<DrawStep>();
    private List<DrawStep> screenDrawSteps = new ArrayList<DrawStep>();
    private List<Layer> layers = new ArrayList<Layer>();
    private WaypointLayer waypointLayer;

    public LayerDelegate()
    {
        layers.add(new BlockInfoLayer());
        waypointLayer = new WaypointLayer();
        layers.add(waypointLayer);
    }

    public void onMouseMove(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord)
    {
        clearDrawSteps();
        for (Layer layer : layers)
        {
            try
            {
                addDrawSteps(layer.onMouseMove(mc, mouseX, mouseY, gridWidth, gridHeight, blockCoord));
            }
            catch (Exception e)
            {
                Journeymap.getLogger().error(LogFormatter.toString(e));
            }
        }
    }

    public void onMouseClicked(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord, int mouseButton)
    {
        clearDrawSteps();
        for (Layer layer : layers)
        {
            try
            {
                addDrawSteps(layer.onMouseClick(mc, mouseX, mouseY, gridWidth, gridHeight, blockCoord));
            }
            catch (Exception e)
            {
                Journeymap.getLogger().error(LogFormatter.toString(e));
            }
        }
    }

    private void clearDrawSteps()
    {
        drawSteps.clear();
        mapDrawSteps.clear();
        screenDrawSteps.clear();
    }

    private void addDrawSteps(List<DrawStep> steps)
    {
        for (DrawStep drawStep : steps)
        {
            drawSteps.add(drawStep);
            // Map steps inherit pan/zoom transforms; screen steps stay aligned to the GUI.
            if (drawStep instanceof ScreenLayerDrawStep)
            {
                screenDrawSteps.add(drawStep);
            }
            else
            {
                mapDrawSteps.add(drawStep);
            }
        }
    }

    public List<DrawStep> getDrawSteps()
    {
        return drawSteps;
    }

    public List<DrawStep> getMapDrawSteps()
    {
        return mapDrawSteps;
    }

    public List<DrawStep> getScreenDrawSteps()
    {
        return screenDrawSteps;
    }


    public interface Layer
    {
        public List<DrawStep> onMouseMove(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord);

        public List<DrawStep> onMouseClick(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord);
    }

}
