/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.fullscreen.layer;

import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.cartography.RGB;
import journeymap.client.data.DataCache;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.BlockCoordIntPair;
import journeymap.client.model.ChunkMD;
import journeymap.client.properties.FullMapProperties;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.map.GridRenderer;
import journeymap.client.ui.option.LocationFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author techbrew 2/26/14.
 */
public class BlockInfoLayer implements LayerDelegate.Layer
{
    private final List<DrawStep> drawStepList = new ArrayList<DrawStep>(1);
    LocationFormat locationFormat = new LocationFormat();
    LocationFormat.LocationFormatKeys locationFormatKeys;
    BlockCoordIntPair lastCoord = null;
    long lastClicked = 0;
    int lastMouseX;
    int lastMouseY;
    BlockInfoStep blockInfoStep;
    FontRenderer fontRenderer = ForgeHelper.INSTANCE.getFontRenderer();

    public BlockInfoLayer()
    {
        blockInfoStep = new BlockInfoStep();
        drawStepList.add(blockInfoStep);
    }

    @Override
    public List<DrawStep> onMouseMove(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord)
    {
        if (!blockCoord.equals(lastCoord))
        {
            FullMapProperties fullMapProperties = JourneymapClient.getFullMapProperties();

            locationFormatKeys = locationFormat.getFormatKeys(fullMapProperties.locationFormat.get());

            lastCoord = blockCoord;

            // Get block under mouse
            Chunk chunk = mc.theWorld.getChunkFromChunkCoords(blockCoord.x >> 4, blockCoord.z >> 4);
            String info;
            if (!chunk.isEmpty())
            {
                ChunkMD chunkMD = DataCache.instance().getChunkMD(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition));
                int blockY = chunkMD.getPrecipitationHeight(blockCoord.x & 15, blockCoord.z & 15);
                String biome = ForgeHelper.INSTANCE.getBiome(blockCoord.x, blockY, blockCoord.z).biomeName;

                info = locationFormatKeys.format(fullMapProperties.locationFormatVerbose.get(),
                        blockCoord.x,
                        blockCoord.z,
                        blockY,
                        (blockY >> 4)) + " " + biome;
            }
            else
            {
                info = Constants.getString("jm.common.location_xz_verbose", blockCoord.x, blockCoord.z);
            }

            double infoHeight = DrawUtil.getLabelHeight(fontRenderer, true) * getMapFontScale();
            blockInfoStep.update(info, gridWidth / 2, gridHeight - infoHeight);
        }
        else
        {
            blockInfoStep.update(blockInfoStep.text, gridWidth / 2, blockInfoStep.y);
        }

        return drawStepList;
    }

    private double getMapFontScale()
    {
        return JourneymapClient.getFullMapProperties().fontScale.get();
    }

    @Override
    public List<DrawStep> onMouseClick(Minecraft mc, double mouseX, double mouseY, int gridWidth, int gridHeight, BlockCoordIntPair blockCoord)
    {
        return Collections.EMPTY_LIST;
    }

    class BlockInfoStep implements DrawStep
    {

        Integer bgColor = RGB.DARK_GRAY_RGB;
        Integer fgColor = RGB.WHITE_RGB;
        double fontScale = 1;
        boolean fontShadow = false;
        int alpha = 255;
        int ticks = 20 * 5;
        private double x;
        private double y;
        private String text;

        void update(String text, double x, double y)
        {
            this.text = text;
            this.x = x;
            this.y = y;
            this.alpha = 255;
            this.ticks = 20 * 5;
        }

        @Override
        public void draw(double xOffset, double yOffset, GridRenderer gridRenderer, float drawScale, double fontScale, double rotation)
        {
            if (ticks-- < 0 && alpha > 0)
            {
                alpha -= 1; // Fade
            }
            if (alpha > 10 && text != null)
            {
                DrawUtil.drawLabel(text, x, y, DrawUtil.HAlign.Center, DrawUtil.VAlign.Above, bgColor, Math.max(0, alpha), fgColor, Math.max(0, alpha), getMapFontScale(), fontShadow);
            }

        }
    }
}
