/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.dialog;

import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.io.ThemeFileHandler;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.GridSpec;
import journeymap.client.model.GridSpecs;
import journeymap.client.model.MapType;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.texture.TextureCache;
import journeymap.client.render.texture.TextureImpl;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.Button;
import journeymap.client.ui.component.*;
import journeymap.client.ui.theme.Theme;
import journeymap.client.ui.theme.ThemeToggle;
import journeymap.common.Journeymap;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL12;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GridEditor extends JmUI
{
    private final TextureImpl colorPickTexture;

    private final int tileSize = 128;
    private final int sampleTextureSize = 128;

    private GridSpecs gridSpecs;
    private ListPropertyButton<GridSpec.Style> buttonStyle;
    private IntSliderButton buttonOpacity;
    private CheckBox checkDay, checkNight, checkUnderground;
    private ThemeToggle buttonDay, buttonNight, buttonUnderground;
    private Integer activeColor;
    private MapType activeMapType;

    private Button buttonReset;
    private Button buttonCancel;
    private Button buttonClose;

    private Rectangle2D.Double colorPickRect;
    private BufferedImage colorPickImg;

    private ButtonList topButtons;
    private ButtonList leftButtons;
    private ButtonList leftChecks;
    private ButtonList bottomButtons;

    public GridEditor(JmUI returnDisplay)
    {
        super(Constants.getString("jm.common.grid_editor"), returnDisplay);
        this.colorPickTexture = TextureCache.instance().getColorPicker();
        this.colorPickRect = new Rectangle2D.Double(0, 0, colorPickTexture.getWidth(), colorPickTexture.getHeight());
        this.colorPickImg = colorPickTexture.getImage();

        this.gridSpecs = JourneymapClient.getCoreProperties().gridSpecs.clone();

        MapType mapType = MapType.day(0);
        activeMapType = mapType;
        this.activeColor = this.gridSpecs.getSpec(activeMapType).getColor();

        Keyboard.enableRepeatEvents(true);
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @Override
    public void initGui()
    {
        try
        {
            if (this.buttonList.isEmpty())
            {
                GridSpec spec = gridSpecs.getSpec(activeMapType);

                // Top
                buttonStyle = new ListPropertyButton<GridSpec.Style>(EnumSet.allOf(GridSpec.Style.class),
                        Constants.getString("jm.common.grid_style"),
                        null, new AtomicReference<GridSpec.Style>(spec.style));

                buttonOpacity = new IntSliderButton(null, new AtomicInteger((int) Math.ceil(spec.alpha * 100)), Constants.getString("jm.common.grid_opacity") + " : ", "", 0, 100, true);
                topButtons = new ButtonList(buttonStyle, buttonOpacity);
                topButtons.equalizeWidths(getFontRenderer());

                // Left Checks
                checkDay = new CheckBox("", activeMapType == MapType.day(0));
                checkNight = new CheckBox("", activeMapType == MapType.night(0));
                checkUnderground = new CheckBox("", activeMapType.isUnderground());
                leftChecks = new ButtonList(checkDay, checkNight, checkUnderground);

                // Left Buttons
                Theme theme = ThemeFileHandler.getCurrentTheme();
                buttonDay = new ThemeToggle(theme, "jm.fullscreen.map_day", "day");
                buttonDay.setToggled(activeMapType == MapType.day(0), false);

                buttonNight = new ThemeToggle(theme, "jm.fullscreen.map_night", "night");
                buttonNight.setToggled(activeMapType == MapType.night(0), false);

                buttonUnderground = new ThemeToggle(theme, "jm.fullscreen.map_caves", "caves");
                buttonUnderground.setToggled(activeMapType.isUnderground(), false);

                leftButtons = new ButtonList(buttonDay, buttonNight, buttonUnderground);

                // Bottom
                buttonReset = new Button(Constants.getString("jm.waypoint.reset"));
                buttonCancel = new Button(Constants.getString("jm.waypoint.cancel"));
                buttonClose = new Button(Constants.getString("jm.waypoint.save"));
                bottomButtons = new ButtonList(buttonReset, buttonCancel, buttonClose);
                bottomButtons.equalizeWidths(getFontRenderer());

                buttonList.addAll(topButtons);
                buttonList.addAll(leftChecks);
                buttonList.addAll(leftButtons);
                buttonList.addAll(bottomButtons);

                updatePreview(activeMapType);
            }
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error(LogFormatter.toString(t));
            UIManager.getInstance().closeAll();
        }
    }

    /**
     * Center buttons in UI.
     */
    @Override
    protected void layoutButtons()
    {
        try
        {
            // Buttons
            initGui();

            // Margins
            final int hgap = 6;
            final int vgap = 6;
            final int startY = Math.max(40, (this.height - 230) / 2);
            final int centerX = this.width / 2;

            // Color picker and top buttons
            int cpSize = topButtons.getHeight(vgap);
            int topRowWidth = hgap + cpSize + topButtons.get(0).getWidth();
            int topRowLeft = centerX - (topRowWidth / 2);
            topButtons.layoutVertical(topRowLeft + hgap + cpSize, startY, true, vgap);
            drawColorPicker(topRowLeft, topButtons.getTopY(), cpSize);

            // Sum Width of Left controls and Map Tile
            //int middleWidth = checkDay.getWidth() + hgap + buttonDay.getWidth() + hgap + tileSize;
            int tileX = centerX - (tileSize / 2);
            int tileY = topButtons.getBottomY() + (vgap * 2);

            // Map Tile
            drawMapTile(tileX, tileY);

            // Left Buttons
            leftButtons.layoutVertical(tileX - leftButtons.get(0).getWidth() - hgap, tileY + vgap, true, vgap);

            // Left Checks
            leftChecks.setHeights(leftButtons.get(0).getHeight());
            leftChecks.setWidths(15);
            leftChecks.layoutVertical(leftButtons.getLeftX() - checkDay.getWidth(), leftButtons.getTopY(), true, vgap);

            // Bottom Buttons
            int bottomY = Math.min(tileY + sampleTextureSize + (vgap * 2), height - 10 - buttonClose.getHeight());
            bottomButtons.equalizeWidths(getFontRenderer(), hgap, topButtons.get(0).getRightX() - topRowLeft);
            bottomButtons.layoutCenteredHorizontal(centerX, bottomY, true, hgap);
        }
        catch (Throwable t)
        {
            logger.error("Error in GridEditor.layoutButtons: " + LogFormatter.toString(t));
        }
    }

    @Override
    public void drawScreen(int x, int y, float par3)
    {
        try
        {
            drawBackground(0);

            layoutButtons();

            for (int k = 0; k < this.buttonList.size(); ++k)
            {
                GuiButton guibutton = (GuiButton) this.buttonList.get(k);
                guibutton.drawButton(this.mc, x, y);
            }

            drawTitle();
            drawLogo();
        }
        catch (Throwable t)
        {
            logger.error("Error in GridEditor.drawScreen: " + LogFormatter.toString(t));
        }
    }

    protected void drawColorPicker(int x, int y, float size)
    {
        int sizeI = (int) size;
        drawRect(x - 1, y - 1, x + sizeI + 1, y + sizeI + 1, -6250336);

        if (colorPickRect.width != size)
        {
            // Updated scaled image only when necessary
            Image image = colorPickTexture.getImage().getScaledInstance(sizeI, sizeI, Image.SCALE_FAST);
            colorPickImg = new BufferedImage(sizeI, sizeI, BufferedImage.TYPE_INT_RGB);

            Graphics g = colorPickImg.createGraphics();
            g.drawImage(image, 0, 0, sizeI, sizeI, null);
            g.dispose();
        }
        colorPickRect.setRect(x, y, size, size);
        float scale = size / colorPickTexture.getWidth();
        DrawUtil.drawImage(colorPickTexture, x, y, false, scale, 0);

        GridSpec activeSpec = gridSpecs.getSpec(activeMapType);
        int colorX = activeSpec.getColorX();
        int colorY = activeSpec.getColorY();
        if (colorX > 0 && colorY > 0)
        {
            colorX += x;
            colorY += y;
            DrawUtil.drawRectangle(colorX - 2, colorY - 2, 5, 5, Color.darkGray.getRGB(), 200);
            DrawUtil.drawRectangle(colorX - 1, colorY, 3, 1, activeColor, 255);
            DrawUtil.drawRectangle(colorX, colorY - 1, 1, 3, activeColor, 255);
        }
    }

    protected void drawMapTile(int x, int y)
    {
        float scale = tileSize / sampleTextureSize;

        drawRect(x - 1, y - 1, x + tileSize + 1, y + tileSize + 1, -6250336);

        TextureImpl tileTex = TextureCache.instance().getTileSample(activeMapType);
        DrawUtil.drawImage(tileTex, x, y, false, 1, 0);
        if (scale == 2)
        {
            DrawUtil.drawImage(tileTex, x + sampleTextureSize, y, true, 1, 0);
            DrawUtil.drawImage(tileTex, x, y + sampleTextureSize, true, 1, 180);
            DrawUtil.drawImage(tileTex, x + sampleTextureSize, y + sampleTextureSize, false, 1, 180);
        }

        GridSpec gridSpec = gridSpecs.getSpec(activeMapType);
        gridSpec.beginTexture(GL12.GL_CLAMP_TO_EDGE, 1f);
        DrawUtil.drawBoundTexture(0, 0, x, y, 0, .25, .25, x + tileSize, y + tileSize);
        gridSpec.finishTexture();
    }

    protected void drawLabel(String label, int x, int y)
    {
        drawString(getFontRenderer(), label, x, y, Color.cyan.getRGB());
    }

    protected void keyTyped(char par1, int par2)
    {
        try
        {
            switch (par2)
            {
                case Keyboard.KEY_ESCAPE:
                    closeAndReturn();
                    return;
                case Keyboard.KEY_RETURN:
                    saveAndClose();
                    return;
                default:
                    break;
            }
        }
        catch (Throwable t)
        {
            logger.error("Error in GridEditor.keyTyped: " + LogFormatter.toString(t));
        }
    }

    @Override
    protected void mouseClickMove(int par1, int par2, int par3, long par4)
    {
        try
        {
            if (buttonOpacity.dragging)
            {
                updateGridSpecs();
            }
            else
            {
                checkColorPicker(par1, par2);
            }
        }
        catch (Throwable t)
        {
            logger.error("Error in GridEditor.mouseClickMove: " + LogFormatter.toString(t));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        try
        {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            checkColorPicker(mouseX, mouseY);
        }
        catch (Throwable t)
        {
            logger.error("Error in GridEditor.mouseClicked: " + LogFormatter.toString(t));
        }
    }

    protected void checkColorPicker(int mouseX, int mouseY)
    {
        if (colorPickRect.contains(mouseX, mouseY))
        {
            int x = mouseX - (int) colorPickRect.x;
            int y = mouseY - (int) colorPickRect.y;
            activeColor = colorPickImg.getRGB(x, y);
            GridSpec activeSpec = gridSpecs.getSpec(activeMapType);
            activeSpec.setColorCoords(x, y);
            updateGridSpecs();
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton)
    {
        try
        {
            if (guibutton == buttonDay)
            {
                updatePreview(MapType.day(0));
            }
            else if (guibutton == buttonNight)
            {
                updatePreview(MapType.night(0));
            }
            else if (guibutton == buttonUnderground)
            {
                updatePreview(MapType.underground(0, 0));
            }

            updateGridSpecs();

            if (guibutton == buttonReset)
            {
                resetGridSpecs();
                return;
            }
            if (guibutton == buttonCancel)
            {
                resetGridSpecs();
                closeAndReturn();
                return;
            }
            if (guibutton == buttonClose)
            {
                saveAndClose();
                return;
            }
        }
        catch (Throwable t)
        {
            logger.error("Error in GridEditor.actionPerformed: " + LogFormatter.toString(t));
        }
    }

    protected void updatePreview(MapType mapType)
    {
        activeMapType = mapType;
        GridSpec activeSpec = gridSpecs.getSpec(activeMapType);
        activeColor = activeSpec.getColor();
        buttonOpacity.setValue((int) (activeSpec.alpha * 100));
        buttonStyle.setValue(activeSpec.style);

        checkDay.setToggled(mapType.isDay());
        checkNight.setToggled(mapType.isNight());
        checkUnderground.setToggled(mapType.isUnderground());
        buttonDay.setToggled(mapType.isDay());
        buttonNight.setToggled(mapType.isNight());
        buttonUnderground.setToggled(mapType.isUnderground());
    }

    protected void updateGridSpecs()
    {
        GridSpec activeSpec = gridSpecs.getSpec(activeMapType);
        int colorX = activeSpec.getColorX();
        int colorY = activeSpec.getColorY();

        GridSpec newSpec = new GridSpec(buttonStyle.getValueHolder().get(), new Color(activeColor), (float) buttonOpacity.getValue() / 100f).setColorCoords(colorX, colorY);

        if (checkDay.getToggled())
        {
            this.gridSpecs.setSpec(MapType.day(0), newSpec);
        }

        if (checkNight.getToggled())
        {
            this.gridSpecs.setSpec(MapType.night(0), newSpec);
        }

        if (checkUnderground.getToggled())
        {
            this.gridSpecs.setSpec(MapType.underground(0, 0), newSpec);
        }
    }

    protected void saveAndClose()
    {
        updateGridSpecs();
        JourneymapClient.getCoreProperties().gridSpecs.updateFrom(this.gridSpecs);
        JourneymapClient.getCoreProperties().save();
        closeAndReturn();
    }

    protected void resetGridSpecs()
    {
        if (checkDay.getToggled())
        {
            this.gridSpecs.setSpec(MapType.day(0), GridSpecs.DEFAULT_DAY.clone());
        }
        if (checkNight.getToggled())
        {
            this.gridSpecs.setSpec(MapType.night(0), GridSpecs.DEFAULT_NIGHT.clone());
        }
        if (checkUnderground.getToggled())
        {
            this.gridSpecs.setSpec(MapType.underground(0, 0), GridSpecs.DEFAULT_UNDERGROUND.clone());
        }
        buttonList.clear();
        initGui();
    }

    @Override
    protected void closeAndReturn()
    {
        if (returnDisplay == null)
        {
            UIManager.getInstance().closeAll();
        }
        else
        {
            UIManager.getInstance().open(returnDisplay);
        }
    }
}
