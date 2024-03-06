/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.dialog;

import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.cartography.RGB;
import journeymap.client.data.DataCache;
import journeymap.client.forge.event.KeyEventHandler;
import journeymap.client.io.ThemeFileHandler;
import journeymap.client.log.JMLogger;
import journeymap.client.properties.CoreProperties;
import journeymap.client.properties.config.Config;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.map.TileDrawStepCache;
import journeymap.client.service.WebServer;
import journeymap.client.task.main.SoftResetTask;
import journeymap.client.task.multi.MapPlayerTask;
import journeymap.client.task.multi.RenderSpec;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.*;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.option.CategorySlot;
import journeymap.client.ui.option.OptionSlotFactory;
import journeymap.client.ui.option.SlotMetadata;
import journeymap.client.waypoint.WaypointStore;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.input.Keyboard;

import java.util.*;

/**
 * Master options UI
 */
public class OptionsManager extends JmUI
{
    protected static EnumSet<Config.Category> openCategories = EnumSet.noneOf(Config.Category.class);

    protected final int inGameMinimapId;
    protected Config.Category[] initialCategories;
    protected CheckBox minimap1PreviewButton;
    protected CheckBox minimap2PreviewButton;
    protected Button minimap1KeysButton, minimap2KeysButton;
    protected Button fullscreenKeysButton;
    protected Button buttonClose;
    protected Button buttonAbout;
    protected Button renderStatsButton;
    protected Button editGridMinimap1Button;
    protected Button editGridMinimap2Button;
    protected Button editGridFullscreenButton;
    protected SlotMetadata renderStatsSlotMetadata;
    protected CategorySlot cartographyCategorySlot;
    protected ScrollListPane<CategorySlot> optionsListPane;
    protected Map<Config.Category, List<SlotMetadata>> toolbars;
    protected EnumSet<Config.Category> changedCategories = EnumSet.noneOf(Config.Category.class);
    protected boolean forceMinimapUpdate;
    protected ButtonList editGridButtons = new ButtonList();


    public OptionsManager()
    {
        this(null);
    }

    public OptionsManager(JmUI returnDisplay)
    {
        this(returnDisplay, openCategories.toArray(new Config.Category[0]));
    }

    public OptionsManager(JmUI returnDisplay, Config.Category... initialCategories)
    {
        super(String.format("JourneyMap %s %s", Journeymap.JM_VERSION, Constants.getString("jm.common.options")), returnDisplay);
        this.initialCategories = initialCategories;
        this.inGameMinimapId = JourneymapClient.getActiveMinimapId();
    }

    @Override
    public void initGui()
    {
        try
        {
            buttonList.clear();

            if (editGridMinimap1Button == null)
            {
                String name = Constants.getString("jm.common.grid_edit");
                String tooltip = Constants.getString("jm.common.grid_edit.tooltip");
                editGridMinimap1Button = new Button(name);
                editGridMinimap1Button.setTooltip(tooltip);
                editGridMinimap1Button.setDrawBackground(false);
                editGridMinimap2Button = new Button(name);
                editGridMinimap2Button.setTooltip(tooltip);
                editGridMinimap2Button.setDrawBackground(false);
                editGridFullscreenButton = new Button(name);
                editGridFullscreenButton.setTooltip(tooltip);
                editGridFullscreenButton.setDrawBackground(false);
                editGridButtons = new ButtonList(editGridMinimap1Button, editGridMinimap2Button, editGridFullscreenButton);
            }

            if (minimap1PreviewButton == null)
            {
                String name = String.format("%s %s", Constants.getString("jm.minimap.preview"), "1");
                String tooltip = Constants.getString("jm.minimap.preview.tooltip");
                minimap1PreviewButton = new CheckBox(name, false);
                minimap1PreviewButton.setTooltip(tooltip);
            }

            if (minimap2PreviewButton == null)
            {
                String name = String.format("%s %s", Constants.getString("jm.minimap.preview"), "2");
                String tooltip = Constants.getString("jm.minimap.preview.tooltip");
                minimap2PreviewButton = new CheckBox(name, false);
                minimap2PreviewButton.setTooltip(tooltip);
            }

            if (minimap1KeysButton == null)
            {
                String name = Constants.getString("jm.common.hotkeys");
                String tooltip = Constants.getString("jm.common.hotkeys.tooltip");
                minimap1KeysButton = new Button(name);
                minimap1KeysButton.setTooltip(tooltip);
                minimap1KeysButton.setDrawBackground(false);
            }

            if (minimap2KeysButton == null)
            {
                String name = Constants.getString("jm.common.hotkeys");
                String tooltip = Constants.getString("jm.common.hotkeys.tooltip");
                minimap2KeysButton = new Button(name);
                minimap2KeysButton.setTooltip(tooltip);
                minimap2KeysButton.setDrawBackground(false);
            }

            if (fullscreenKeysButton == null)
            {
                String name = Constants.getString("jm.common.hotkeys");
                String tooltip = Constants.getString("jm.common.hotkeys.tooltip");
                fullscreenKeysButton = new Button(name);
                fullscreenKeysButton.setTooltip(tooltip);
                fullscreenKeysButton.setDrawBackground(false);
            }

            if (renderStatsButton == null)
            {
                renderStatsButton = new LabelButton(150, "jm.common.renderstats", 0, 0, 0);
                renderStatsButton.setEnabled(false);
            }

            if (optionsListPane == null)
            {
                List<ScrollListPane.ISlot> categorySlots = new ArrayList<ScrollListPane.ISlot>();
                optionsListPane = new ScrollListPane<CategorySlot>(this, mc, this.width, this.height, this.headerHeight, this.height - 30, 20);
                optionsListPane.setAlignTop(true);
                optionsListPane.setSlots(OptionSlotFactory.getSlots(getToolbars()));
                if (initialCategories != null)
                {
                    for (Config.Category initialCategory : initialCategories)
                    {
                        for (CategorySlot categorySlot : optionsListPane.getRootSlots())
                        {
                            if (categorySlot.getCategory() == initialCategory)
                            {
                                categorySlot.setSelected(true);
                                categorySlots.add(categorySlot);
                            }
                        }
                    }
                }

                // Add Toolbar buttons
                for (ScrollListPane.ISlot rootSlot : optionsListPane.getRootSlots())
                {
                    if (rootSlot instanceof CategorySlot)
                    {
                        CategorySlot categorySlot = (CategorySlot) rootSlot;
                        Config.Category category = categorySlot.getCategory();

                        // Reset button
                        ResetButton resetButton = new ResetButton(category);
                        SlotMetadata resetSlotMetadata = new SlotMetadata(resetButton, 1);

                        switch (category)
                        {
                            case MiniMap1:
                            {
                                categorySlot.getAllChildMetadata().add(new SlotMetadata(minimap1PreviewButton, 4));
                                categorySlot.getAllChildMetadata().add(new SlotMetadata(editGridMinimap1Button, 3));
                                categorySlot.getAllChildMetadata().add(new SlotMetadata(minimap1KeysButton, 2));
                                categorySlot.getAllChildMetadata().add(resetSlotMetadata);
                                break;
                            }
                            case MiniMap2:
                            {
                                categorySlot.getAllChildMetadata().add(new SlotMetadata(minimap2PreviewButton, 4));
                                categorySlot.getAllChildMetadata().add(new SlotMetadata(editGridMinimap2Button, 3));
                                categorySlot.getAllChildMetadata().add(new SlotMetadata(minimap2KeysButton, 2));
                                categorySlot.getAllChildMetadata().add(resetSlotMetadata);
                                break;
                            }
                            case FullMap:
                            {
                                categorySlot.getAllChildMetadata().add(new SlotMetadata(editGridMinimap2Button, 3));
                                categorySlot.getAllChildMetadata().add(new SlotMetadata(fullscreenKeysButton, 2));
                                categorySlot.getAllChildMetadata().add(resetSlotMetadata);
                                break;
                            }
                            case Cartography:
                            {
                                cartographyCategorySlot = categorySlot;
                                renderStatsSlotMetadata = new SlotMetadata(renderStatsButton,
                                        Constants.getString("jm.common.renderstats.title"),
                                        Constants.getString("jm.common.renderstats.tooltip"), 2);
                                categorySlot.getAllChildMetadata().add(renderStatsSlotMetadata);
                                categorySlot.getAllChildMetadata().add(resetSlotMetadata);
                                break;
                            }
                            default:
                            {
                                categorySlot.getAllChildMetadata().add(resetSlotMetadata);
                            }
                        }
                    }
                }

                // Update slots
                optionsListPane.updateSlots();

                if (!categorySlots.isEmpty())
                {
                    // Scroll to first
                    optionsListPane.scrollTo(categorySlots.get(0));
                }
            }
            else
            {
                // 1.7
                // optionsListPane.func_148122_a(width, height, headerHeight, this.height - 30);

                // 1.8
                optionsListPane.setDimensions(width, height, headerHeight, this.height - 30);
                optionsListPane.updateSlots();
            }

            buttonClose = new Button(Constants.getString("jm.common.close"));

            buttonAbout = new Button(Constants.getString("jm.common.splash_about"));

            ButtonList bottomRow = new ButtonList(buttonAbout, buttonClose);
            bottomRow.equalizeWidths(getFontRenderer());
            bottomRow.setWidths(Math.max(150, buttonAbout.getWidth()));
            bottomRow.layoutCenteredHorizontal(width / 2, height - 25, true, 4);

            buttonList.addAll(bottomRow);
        }
        catch (Throwable t)
        {
            JMLogger.logOnce("Error in OptionsManager.initGui(): " + t, t);
        }
    }

    @Override
    protected void layoutButtons()
    {
        if (buttonList.isEmpty())
        {
            initGui();
        }
    }

    @Override
    public void drawScreen(int x, int y, float par3)
    {
        try
        {
            if (forceMinimapUpdate)
            {
                // Minimap buttons:  Force minimap toggles
                if (minimap1PreviewButton.isActive())
                {
                    UIManager.getInstance().switchMiniMapPreset(1);
                }
                else if (minimap2PreviewButton.isActive())
                {
                    UIManager.getInstance().switchMiniMapPreset(2);
                }
            }

            updateRenderStats();

            String[] lastTooltip = optionsListPane.lastTooltip;
            long lastTooltipTime = optionsListPane.lastTooltipTime;
            optionsListPane.lastTooltip = null;
            optionsListPane.drawScreen(x, y, par3);

            super.drawScreen(x, y, par3);

            if (previewMiniMap())
            {
                UIManager.getInstance().getMiniMap().drawMap(true);
                RenderHelper.disableStandardItemLighting();
            }

            if (optionsListPane.lastTooltip != null)
            {
                if (Arrays.equals(optionsListPane.lastTooltip, lastTooltip))
                {
                    optionsListPane.lastTooltipTime = lastTooltipTime;
                    if (System.currentTimeMillis() - optionsListPane.lastTooltipTime > optionsListPane.hoverDelay)
                    {
                        Button button = optionsListPane.lastTooltipMetadata.getButton();
                        drawHoveringText(optionsListPane.lastTooltip, x, button.getBottomY() + 15);
                    }
                }
            }
        }
        catch (Throwable t)
        {
            JMLogger.logOnce("Error in OptionsManager.drawScreen(): " + t, t);
        }
    }

    /**
     * Handles mouse input.
     */
    public void handleMouseInput()
    {
        super.handleMouseInput();
        optionsListPane.handleMouseInput();
    }

    private void updateRenderStats()
    {
        RenderSpec.getSurfaceSpec();
        RenderSpec.getUndergroundSpec();

        // Show validation on render distances
        // TODO: Hashmap properties to buttons somewhere for easier lookup?
        for (ScrollListPane.ISlot rootSlot : optionsListPane.getRootSlots())
        {
            if (rootSlot instanceof CategorySlot)
            {
                CategorySlot categorySlot = (CategorySlot) rootSlot;
                if (categorySlot.getCategory() == Config.Category.Cartography)
                {
                    CoreProperties coreProperties = JourneymapClient.getCoreProperties();
                    for (SlotMetadata slotMetadata : categorySlot.getAllChildMetadata())
                    {
                        if (slotMetadata.getButton() instanceof IPropertyHolder)
                        {
                            Object property = ((IPropertyHolder) slotMetadata.getButton()).getProperty();
                            boolean limitButtonRange = false;
                            if (property == coreProperties.renderDistanceCaveMax)
                            {
                                boolean valid = JourneymapClient.getCoreProperties().hasValidCaveRenderDistances();
                                limitButtonRange = true;
                                if (valid)
                                {
                                    slotMetadata.getButton().resetLabelColors();
                                }
                                else
                                {
                                    slotMetadata.getButton().setLabelColors(RGB.RED_RGB, RGB.RED_RGB, null);
                                }
                            }
                            else if (property == coreProperties.renderDistanceSurfaceMax)
                            {
                                boolean valid = JourneymapClient.getCoreProperties().hasValidSurfaceRenderDistances();
                                limitButtonRange = true;
                                if (valid)
                                {
                                    slotMetadata.getButton().resetLabelColors();
                                }
                                else
                                {
                                    slotMetadata.getButton().setLabelColors(RGB.RED_RGB, RGB.RED_RGB, null);
                                }
                            }
                            else if (property == coreProperties.renderDistanceCaveMin)
                            {
                                limitButtonRange = true;
                            }
                            else if (property == coreProperties.renderDistanceSurfaceMin)
                            {
                                limitButtonRange = true;
                            }

                            if (limitButtonRange)
                            {
                                IntSliderButton button = (IntSliderButton) slotMetadata.getButton();
                                button.maxValue = mc.gameSettings.renderDistanceChunks;
                                if (button.getValue() > mc.gameSettings.renderDistanceChunks)
                                {
                                    button.setValue(mc.gameSettings.renderDistanceChunks);
                                }
                            }
                        }
                    }
                }
            }
        }

        renderStatsButton.displayString = JourneymapClient.getCoreProperties().mappingEnabled.get()
                ? MapPlayerTask.getSimpleStats()
                : Constants.getString("jm.common.enable_mapping_false_text");

        renderStatsButton.setWidth(cartographyCategorySlot.getCurrentColumnWidth());
    }

    @Override
    public void drawBackground(int layer)
    {
        //drawDefaultBackground();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseEvent)
    {
        super.mouseClicked(mouseX, mouseY, mouseEvent);
        boolean pressed = optionsListPane.mousePressed(mouseX, mouseY, mouseEvent);
        if (pressed)
        {
            checkPressedButton();
        }
    }

    @Override
    // 1.7
    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseEvent)
    // 1.8
    // protected void mouseReleased(int mouseX, int mouseY, int mouseEvent)
    {
        super.mouseMovedOrUp(mouseX, mouseY, mouseEvent);

        // 1.7
        optionsListPane.mouseMovedOrUp(mouseX, mouseY, mouseEvent);

        // 1.8
        // optionsListPane.mouseReleased(mouseX, mouseY, mouseEvent);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int lastButtonClicked, long timeSinceMouseClick)
    {
        super.mouseClickMove(mouseX, mouseY, lastButtonClicked, timeSinceMouseClick);
        checkPressedButton();
    }

    /**
     * Check the pressed button in the scroll pane and determine if something needs to be updated or refreshed
     */
    protected void checkPressedButton()
    {
        SlotMetadata slotMetadata = optionsListPane.getLastPressed();
        if (slotMetadata != null)
        {
            // If it's a reset button, reset that action
            if (slotMetadata.getButton() instanceof ResetButton)
            {
                resetOptions(((ResetButton) slotMetadata.getButton()).category);
            }

            // Theme button: Force update
            if (slotMetadata.getName().equals(Constants.getString("jm.common.ui_theme")))
            {
                ThemeFileHandler.getCurrentTheme(true);
                if (previewMiniMap())
                {
                    UIManager.getInstance().getMiniMap().updateDisplayVars(true);
                }
            }

            // Grid edit buttons
            if (editGridButtons.contains(slotMetadata.getButton()))
            {
                UIManager.getInstance().openGridEditor(this);
                return;
            }

            // Minimap buttons:  Force minimap toggles
            if (slotMetadata.getButton() == minimap1PreviewButton)
            {
                minimap2PreviewButton.setToggled(false);
                UIManager.getInstance().switchMiniMapPreset(1);
                UIManager.getInstance().getMiniMap().resetInitTime();
            }

            if (slotMetadata.getButton() == minimap2PreviewButton)
            {
                minimap1PreviewButton.setToggled(false);
                UIManager.getInstance().switchMiniMapPreset(2);
                UIManager.getInstance().getMiniMap().resetInitTime();
            }

            if (slotMetadata.getButton() == minimap1KeysButton || slotMetadata.getButton() == minimap2KeysButton)
            {
                optionsListPane.resetLastPressed();
                UIManager.getInstance().openMiniMapHotkeyHelp(this);
                return;
            }

            if (slotMetadata.getButton() == fullscreenKeysButton)
            {
                optionsListPane.resetLastPressed();
                UIManager.getInstance().openMapHotkeyHelp(this);
                return;
            }
        }

        CategorySlot categorySlot = (CategorySlot) optionsListPane.getLastPressedParentSlot();
        if (categorySlot != null)
        {
            // Track the category of the button so resets can happen when OptionsManager is closed
            Config.Category category = categorySlot.getCategory();
            changedCategories.add(category);

            // If the button is MiniMap-related, force it to update
            if (category == Config.Category.MiniMap1 || category == Config.Category.MiniMap2)
            {
                refreshMinimapOptions();
                DataCache.instance().resetRadarCaches();
                UIManager.getInstance().getMiniMap().updateDisplayVars(true);
            }

            // If the button is Cartography-related, ensure valid
            if (category == Config.Category.Cartography)
            {
                JourneymapClient.getCoreProperties().save();
                RenderSpec.resetRenderSpecs();
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button == buttonClose)
        {
            closeAndReturn();
            return;
        }

        if (button == buttonAbout)
        {
            UIManager.getInstance().openSplash(this);
            return;
        }

        if (button == minimap1PreviewButton)
        {
            minimap2PreviewButton.setToggled(false);
            UIManager.getInstance().switchMiniMapPreset(1);
        }

        if (button == minimap2PreviewButton)
        {
            minimap1PreviewButton.setToggled(false);
            UIManager.getInstance().switchMiniMapPreset(2);
        }
    }

    @Override
    protected void keyTyped(char c, int i)
    {
        switch (i)
        {
            case Keyboard.KEY_ESCAPE:
            {
                if (previewMiniMap())
                {
                    minimap1PreviewButton.setToggled(false);
                    minimap2PreviewButton.setToggled(false);
                }
                else
                {
                    closeAndReturn();
                }
                break;
            }
        }

        boolean optionUpdated = optionsListPane.keyTyped(c, i);
        if (optionUpdated && previewMiniMap())
        {
            UIManager.getInstance().getMiniMap().updateDisplayVars(true);
        }

        // Check for any minimap-related keypresses
        if (previewMiniMap())
        {
            boolean pressed = KeyEventHandler.onKeypress(true);
            if (pressed)
            {
                refreshMinimapOptions();
            }
        }
    }

    protected void resetOptions(Config.Category category)
    {
        for (CategorySlot categorySlot : optionsListPane.getRootSlots())
        {
            if (category.equals(categorySlot.getCategory()))
            {
                for (SlotMetadata slotMetadata : categorySlot.getAllChildMetadata())
                {
                    slotMetadata.resetToDefaultValue();
                    slotMetadata.getButton().refresh(); // TODO move into reset
                }
                break;
            }
        }
        // todo: move this to something specific for cartography
        RenderSpec.resetRenderSpecs();
    }

    protected boolean previewMiniMap()
    {
        return minimap1PreviewButton.getToggled() || minimap2PreviewButton.getToggled();
    }

    protected void refreshMinimapOptions()
    {
        EnumSet cats = EnumSet.of(Config.Category.MiniMap1, Config.Category.MiniMap2);
        for (CategorySlot categorySlot : optionsListPane.getRootSlots())
        {
            if (cats.contains(categorySlot.getCategory()))
            {
                for (SlotMetadata slotMetadata : categorySlot.getAllChildMetadata())
                {
                    slotMetadata.getButton().refresh();
                }
            }
        }
    }

    @Override
    protected void closeAndReturn()
    {
        JourneymapClient.getCoreProperties().optionsManagerViewed.set(Journeymap.JM_VERSION.toString());

        // Just in case a property changed but wasn't saved.
        JourneymapClient.getCoreProperties().ensureValid();
        JourneymapClient.getWebMapProperties().ensureValid();
        JourneymapClient.getFullMapProperties().ensureValid();
        JourneymapClient.getMiniMapProperties1().ensureValid();
        JourneymapClient.getMiniMapProperties2().ensureValid();
        JourneymapClient.getWaypointProperties().ensureValid();

        // Ensure minimap is back to the one used before this opened
        UIManager.getInstance().getMiniMap().setMiniMapProperties(JourneymapClient.getMiniMapProperties(this.inGameMinimapId));

        for (Config.Category category : changedCategories)
        {
            switch (category)
            {
                case MiniMap1:
                {
                    DataCache.instance().resetRadarCaches();
                    UIManager.getInstance().getMiniMap().reset();
                    break;
                }
                case MiniMap2:
                {
                    DataCache.instance().resetRadarCaches();
                    break;
                }
                case FullMap:
                {
                    DataCache.instance().resetRadarCaches();
                    ThemeFileHandler.getCurrentTheme(true);
                    break;
                }
                case WebMap:
                {
                    DataCache.instance().resetRadarCaches();
                    WebServer.setEnabled(JourneymapClient.getWebMapProperties().enabled.get(), true);
                    break;
                }
                case Waypoint:
                {
                    WaypointStore.instance().reset();
                }
                case WaypointBeacon:
                {
                    break;
                }
                case Cartography:
                {
                    RenderSpec.resetRenderSpecs();
                    TileDrawStepCache.instance().invalidateAll();
                    MiniMap.state().requireRefresh();
                    Fullscreen.state().requireRefresh();
                    MapPlayerTask.forceNearbyRemap();
                    break;
                }
                case Advanced:
                {
                    SoftResetTask.queue();
                    WebServer.setEnabled(JourneymapClient.getWebMapProperties().enabled.get(), false);
                    break;
                }
            }
        }

        UIManager.getInstance().getMiniMap().reset();
        if (this.returnDisplay instanceof Fullscreen)
        {
            ((Fullscreen) returnDisplay).reset();
        }

        OptionsManager.openCategories.clear();
        for (CategorySlot categorySlot : optionsListPane.getRootSlots())
        {
            if (categorySlot.isSelected())
            {
                OptionsManager.openCategories.add(categorySlot.getCategory());
            }
        }

        // Ensure MiniMap is reset, regardless
        UIManager.getInstance().getMiniMap().updateDisplayVars(true);

        if (returnDisplay == null)
        {
            UIManager.getInstance().openFullscreenMap();
        }
        else
        {
            UIManager.getInstance().open(returnDisplay);
        }
    }

    Map<Config.Category, List<SlotMetadata>> getToolbars()
    {
        if (toolbars == null)
        {
            this.toolbars = new HashMap<Config.Category, List<SlotMetadata>>();
            for (Config.Category category : Config.Category.values())
            {
//                String name = Constants.getString("jm.config.reset");
//                String tooltip = Constants.getString("jm.config.reset.tooltip");
//                SlotMetadata toolbarSlotMetadata = new SlotMetadata(new ResetButton(), name, tooltip);
//                toolbars.put(category, Arrays.asList(toolbarSlotMetadata));
            }
        }
        return toolbars;
    }

    public static class ResetButton extends Button
    {
        public final Config.Category category;

        public ResetButton(Config.Category category)
        {
            super(Constants.getString("jm.config.reset"));
            this.category = category;
            setTooltip(Constants.getString("jm.config.reset.tooltip"));
            setDrawBackground(false);
            setLabelColors(RGB.RED_RGB, RGB.RED_RGB, null);
        }
    }

    public static class LabelButton extends Button
    {
        DrawUtil.HAlign hAlign = DrawUtil.HAlign.Left;

        public LabelButton(int width, String key, Object... labelArgs)
        {
            super(Constants.getString(key, labelArgs));
            setTooltip(Constants.getString(key + ".tooltip"));
            setDrawBackground(false);
            setDrawFrame(false);
            setEnabled(false);
            setLabelColors(RGB.LIGHT_GRAY_RGB, RGB.LIGHT_GRAY_RGB, RGB.LIGHT_GRAY_RGB);
            this.width = width;
        }

        @Override
        public int getFitWidth(FontRenderer fr)
        {
            return width;
        }

        @Override
        public void fitWidth(FontRenderer fr)
        {
        }

        public void setHAlign(DrawUtil.HAlign hAlign)
        {
            this.hAlign = hAlign;
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY)
        {
            int labelX;
            switch (hAlign)
            {
                case Left:
                {
                    labelX = this.getRightX();
                    break;
                }
                case Right:
                {
                    labelX = this.getX();
                    break;
                }
                default:
                {
                    labelX = this.getCenterX();
                }
            }

            DrawUtil.drawLabel(this.displayString, labelX, this.getMiddleY(), hAlign, DrawUtil.VAlign.Middle, null, 0, labelColor, 255, 1, drawLabelShadow);
        }
    }
}
