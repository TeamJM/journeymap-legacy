/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.waypoint;

import journeymap.client.Constants;
import journeymap.client.command.CmdTeleportWaypoint;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.JMLogger;
import journeymap.client.model.Waypoint;
import journeymap.client.properties.config.Config;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.*;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.ui.option.CategorySlot;
import journeymap.client.ui.option.SlotMetadata;
import journeymap.client.waypoint.WaypointStore;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;

/**
 * Waypoint Manager 2nd Edition
 */
public class WaypointManager extends JmUI
{
    final static String ASCEND = "\u25B2";
    final static String DESCEND = "\u25BC";
    final static int COLWAYPOINT = 0;
    final static int COLLOCATION = 20;
    final static int COLNAME = 60;
    final static int DEFAULT_ITEMWIDTH = 460;
    private static WaypointManagerItem.Sort currentSort;
    private final String on = Constants.getString("jm.common.on");
    private final String off = Constants.getString("jm.common.off");
    protected int colWaypoint = COLWAYPOINT;
    protected int colLocation = COLLOCATION;
    protected int colName = COLNAME;
    protected int itemWidth = DEFAULT_ITEMWIDTH;
    protected ScrollListPane itemScrollPane;
    protected int rowHeight = 16;
    Boolean canUserTeleport;
    private SortButton buttonSortName, buttonSortDistance;
    private DimensionsButton buttonDimensions;
    private Button buttonClose, buttonAdd, buttonOptions;
    private OnOffButton buttonToggleAll;
    private ButtonList bottomButtons;
    private Waypoint focusWaypoint;
    private ArrayList<WaypointManagerItem> items = new ArrayList<WaypointManagerItem>();

    public WaypointManager()
    {
        this(null, null);
    }

    public WaypointManager(JmUI returnDisplay)
    {
        this(null, returnDisplay);
    }

    public WaypointManager(Waypoint focusWaypoint, JmUI returnDisplay)
    {
        super(Constants.getString("jm.waypoint.manage_title"), returnDisplay);
        this.focusWaypoint = focusWaypoint;
    }

    @Override
    public void initGui()
    {
        try
        {
            buttonList.clear();

            canUserTeleport = CmdTeleportWaypoint.isPermitted(mc);
            FontRenderer fr = getFontRenderer();

            if (buttonSortDistance == null)
            {
                WaypointManagerItem.Sort distanceSort = new WaypointManagerItem.DistanceComparator(ForgeHelper.INSTANCE.getClient().thePlayer, true);
                String distanceLabel = Constants.getString("jm.waypoint.distance");
                buttonSortDistance = new SortButton(distanceLabel, distanceSort);
                buttonSortDistance.setTextOnly(fr);
//                String tooltip = Constants.getString("jm.waypoint.distance.tooltip");
//                buttonSortDistance.setTooltip(tooltip);
            }
            buttonList.add(buttonSortDistance);

            if (buttonSortName == null)
            {
                WaypointManagerItem.Sort nameSort = new WaypointManagerItem.NameComparator(true);
                buttonSortName = new SortButton(Constants.getString("jm.waypoint.name"), nameSort);
                buttonSortName.setTextOnly(fr);
//                String tooltip = Constants.getString("jm.waypoint.name.tooltip");
//                buttonSortName.setTooltip(tooltip);
            }
            buttonList.add(buttonSortName);

            if (buttonToggleAll == null)
            {
                String enableOn = Constants.getString("jm.waypoint.enable_all", "", on);
                String enableOff = Constants.getString("jm.waypoint.enable_all", "", off);
                buttonToggleAll = new OnOffButton(enableOff, enableOn, true);
                buttonToggleAll.setTextOnly(getFontRenderer());
//                String tooltip = Constants.getString("jm.waypoint.enable_all.tooltip");
//                buttonToggleAll.setTooltip(tooltip);
            }
            buttonList.add(buttonToggleAll);

            // Bottom buttons
            if (buttonDimensions == null)
            {
                buttonDimensions = new DimensionsButton();
            }

            if (buttonAdd == null)
            {
                buttonAdd = new Button(Constants.getString("jm.waypoint.new"));
                buttonAdd.fitWidth(getFontRenderer());
                buttonAdd.setWidth(buttonAdd.getWidth() * 2);
//                String tooltip = Constants.getString("jm.waypoint.new.tooltip");
//                buttonAdd.setTooltip(tooltip);
            }

            if (buttonOptions == null)
            {
                buttonOptions = new Button(Constants.getString("jm.common.options_button"));
                buttonOptions.fitWidth(getFontRenderer());
//                String tooltip = Constants.getString("jm.waypoint.help.tooltip");
//                buttonOptions.setTooltip(tooltip);
            }

            buttonClose = new Button(Constants.getString("jm.common.close"));

            bottomButtons = new ButtonList(buttonOptions, buttonAdd, buttonDimensions, buttonClose);
            buttonList.addAll(bottomButtons);


            if (this.items.isEmpty())
            {
                updateItems();
                if (currentSort == null)
                {
                    updateSort(buttonSortDistance);
                }
                else
                {
                    if (buttonSortDistance.sort.equals(currentSort))
                    {
                        buttonSortDistance.sort.ascending = currentSort.ascending;
                        buttonSortDistance.setActive(true);
                        buttonSortName.setActive(false);
                    }
                    if (buttonSortName.sort.equals(currentSort))
                    {
                        buttonSortName.sort.ascending = currentSort.ascending;
                        buttonSortName.setActive(true);
                        buttonSortDistance.setActive(false);
                    }
                }
            }
            else
            {

            }

            if (itemScrollPane == null)
            {
                this.itemScrollPane = new ScrollListPane(this, mc, this.width, this.height, headerHeight, this.height - 30, 20);
            }
            else
            {
                itemScrollPane.setDimensions(width, height, headerHeight, this.height - 30);
                itemScrollPane.updateSlots();
            }

            // Update slots
            itemScrollPane.setSlots(items);
            if (!items.isEmpty())
            {
                itemScrollPane.scrollTo(items.get(0));
            }

        }
        catch (Throwable t)
        {
            JMLogger.logOnce("Error in OptionsManager.initGui(): " + t, t);
        }
    }

    @Override
    protected void layoutButtons()
    {
        if (buttonList.isEmpty() || itemScrollPane == null)
        {
            initGui();
        }

        buttonToggleAll.setDrawButton(!items.isEmpty());
        buttonSortDistance.setDrawButton(!items.isEmpty());
        buttonSortName.setDrawButton(!items.isEmpty());

        bottomButtons.equalizeWidths(getFontRenderer());
        int bottomButtonWidth = Math.min(bottomButtons.getWidth(4) + 25, itemScrollPane.getListWidth());
        bottomButtons.equalizeWidths(getFontRenderer(), 4, bottomButtonWidth);
        bottomButtons.layoutCenteredHorizontal(width / 2, height - 25, true, 4);
    }

    @Override
    public void drawScreen(int x, int y, float par3)
    {
        if (mc == null)
        {
            return;
        }

        if (buttonList.isEmpty() || itemScrollPane == null)
        {
            initGui();
        }

        try
        {
            // 1.7
            // itemScrollPane.func_148122_a(width, height, headerHeight, this.height - 30);

            // 1.8
            itemScrollPane.setDimensions(width, height, headerHeight, this.height - 30);
            String[] lastTooltip = itemScrollPane.lastTooltip;
            long lastTooltipTime = itemScrollPane.lastTooltipTime;
            itemScrollPane.lastTooltip = null;
            itemScrollPane.drawScreen(x, y, par3);

            super.drawScreen(x, y, par3);

            // Header buttons

            if (!items.isEmpty())
            {
                int headerY = headerHeight - getFontRenderer().FONT_HEIGHT;
                WaypointManagerItem firstRow = items.get(0);
                if (firstRow.y > headerY + 16)
                {
                    headerY = firstRow.y - 16;
                }

                buttonToggleAll.centerHorizontalOn(firstRow.getButtonEnableCenterX()).setY(headerY);
                buttonSortDistance.centerHorizontalOn(firstRow.getLocationLeftX()).setY(headerY);
                colName = buttonSortDistance.getRightX() + 10;

                buttonSortName.setPosition(colName - 5, headerY);
            }
            buttonToggleAll.drawUnderline();

            for (List<SlotMetadata> toolbar : getToolbars().values())
            {
                for (SlotMetadata slotMetadata : toolbar)
                {
                    slotMetadata.getButton().secondaryDrawButton();
                }
            }

            if (itemScrollPane.lastTooltip != null)
            {
                if (Arrays.equals(itemScrollPane.lastTooltip, lastTooltip))
                {
                    itemScrollPane.lastTooltipTime = lastTooltipTime;
                    if (System.currentTimeMillis() - itemScrollPane.lastTooltipTime > itemScrollPane.hoverDelay)
                    {
                        Button button = itemScrollPane.lastTooltipMetadata.getButton();
                        drawHoveringText(itemScrollPane.lastTooltip, x, button.getBottomY() + 15);
                    }
                }
            }
        }
        catch (Throwable t)
        {
            JMLogger.logOnce("Error in OptionsManager.drawScreen(): " + t, t);
        }
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
        boolean pressed = itemScrollPane.mousePressed(mouseX, mouseY, mouseEvent);
        if (pressed)
        {
            checkPressedButton();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);

        // 1.7
        // itemScrollPane.mouseMovedOrUp(mouseX, mouseY, state);

        // 1.8
        itemScrollPane.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int lastButtonClicked, long timeSinceMouseClick)
    {
        super.mouseClickMove(mouseX, mouseY, lastButtonClicked, timeSinceMouseClick);
        checkPressedButton();
    }

    @Override
    /**
     * Handles mouse input.
     */
    public void handleMouseInput()
    {
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();

        if (i != 0)
        {
            if (i > 1)
            {
                i = -1;
            }

            if (i < -1)
            {
                i = 1;
            }

            this.itemScrollPane.scrollBy(this.rowHeight * i);
        }
    }

    /**
     * Check the pressed button in the scroll pane and determine if something needs to be updated or refreshed
     */
    protected void checkPressedButton()
    {
        SlotMetadata slotMetadata = itemScrollPane.getLastPressed();
        if (slotMetadata != null) // TODO
        {


        }

        ScrollListPane.ISlot parentSlot = (CategorySlot) itemScrollPane.getLastPressedParentSlot();
        if (parentSlot != null)
        {
            // TODO
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton)
    {
        if (guibutton == buttonClose)
        {
            refreshAndClose();
            return;
        }
        if (guibutton == buttonSortName)
        {
            updateSort(buttonSortName);
            return;
        }
        if (guibutton == buttonSortDistance)
        {
            updateSort(buttonSortDistance);
            return;
        }
        if (guibutton == buttonDimensions)
        {
            buttonDimensions.nextValue();
            updateItems();
            buttonList.clear();
            return;
        }
        if (guibutton == buttonAdd)
        {
            Waypoint waypoint = Waypoint.of(mc.thePlayer);
            UIManager.getInstance().openWaypointEditor(waypoint, true, this);
            return;
        }
        if (guibutton == buttonToggleAll)
        {
            boolean state = buttonToggleAll.getToggled();
            state = toggleItems(state);
            buttonToggleAll.setToggled(state);
            buttonList.clear(); // todo no hack
            return;
        }
        if (guibutton == buttonOptions)
        {
            UIManager.getInstance().openOptionsManager(this, Config.Category.Waypoint, Config.Category.WaypointBeacon);
            return;
        }
    }

    @Override
    protected void keyTyped(char c, int i)
    {
        switch (i)
        {
            case Keyboard.KEY_ESCAPE:
            {
                closeAndReturn();
                break;
            }
        }

        boolean keyUsed = itemScrollPane.keyTyped(c, i);
        if (keyUsed)
        {
            return;
        }

        if (i == Keyboard.KEY_HOME)
        {
            this.itemScrollPane.scrollBy(-this.itemScrollPane.getAmountScrolled());
        }

        if (i == Keyboard.KEY_END)
        {
            this.itemScrollPane.scrollBy(items.size() * rowHeight);
        }
    }

    protected boolean toggleItems(boolean enable)
    {
        for (WaypointManagerItem item : items)
        {
            if (enable == item.waypoint.isEnable())
            {
                enable = !enable;
                break;
            }
        }
        for (WaypointManagerItem item : items)
        {
            if (item.waypoint.isEnable() != enable)
            {
                item.enableWaypoint(enable);
            }
        }
        return !enable;
    }

    protected void updateItems()
    {
        items.clear();
        Integer currentDim = DimensionsButton.currentWorldProvider == null ? null : ForgeHelper.INSTANCE.getDimension(DimensionsButton.currentWorldProvider);
        FontRenderer fr = getFontRenderer();
        itemWidth = 0;

        Collection<Waypoint> waypoints = WaypointStore.instance().getAll();
        boolean allOn = true;
        for (Waypoint waypoint : waypoints)
        {
            WaypointManagerItem item = new WaypointManagerItem(waypoint, fr, this);
            item.getDistanceTo(mc.thePlayer);
            if (currentDim == null || item.waypoint.getDimensions().contains(currentDim))
            {
                items.add(item);
                if (allOn)
                {
                    allOn = waypoint.isEnable();
                }
            }
        }

        if (items.isEmpty())
        {
            itemWidth = DEFAULT_ITEMWIDTH;
        }
        else
        {
            itemWidth = items.get(0).internalWidth;
        }

        buttonToggleAll.setToggled(!allOn);
        updateCount();

        if (currentSort != null)
        {
            Collections.sort(items, currentSort);
        }
    }

    protected void updateSort(SortButton sortButton)
    {
        // 1.8.8
        // for (GuiButton button : buttonList)
        for (Button button : (List<Button>) buttonList)
        {
            if (button instanceof SortButton)
            {
                if (button == sortButton)
                {
                    if (sortButton.sort.equals(currentSort))
                    {
                        sortButton.toggle();
                    }
                    else
                    {
                        sortButton.setActive(true);
                    }
                    currentSort = sortButton.sort;
                }
                else
                {
                    ((SortButton) button).setActive(false);
                }
            }
        }

        if (currentSort != null)
        {
            Collections.sort(items, currentSort);
        }

        if (itemScrollPane != null)
        {
            itemScrollPane.setSlots(items);
        }
        //layoutButtons();
    }

    protected void updateCount()
    {
        String itemCount = items.isEmpty() ? "" : Integer.toString(items.size());
        String enableOn = Constants.getString("jm.waypoint.enable_all", itemCount, on);
        String enableOff = Constants.getString("jm.waypoint.enable_all", itemCount, off);
        buttonToggleAll.setLabels(enableOff, enableOn);
    }

    protected boolean isSelected(WaypointManagerItem item)
    {
        return itemScrollPane.isSelected(item.getSlotIndex()); // TODO
    }

    protected int getMargin()
    {
        return width > itemWidth + 2 ? (width - itemWidth) / 2 : 0;
    }

    public void removeWaypoint(WaypointManagerItem item)
    {
        WaypointStore.instance().remove(item.waypoint);
        this.items.remove(item);
    }

    protected void refreshAndClose()
    {
        closeAndReturn();
    }

    @Override
    protected void closeAndReturn()
    {
        bottomButtons.setEnabled(false);
        WaypointStore.instance().bulkSave();
        Fullscreen.state().requireRefresh();
        bottomButtons.setEnabled(true);

        if (returnDisplay == null)
        {
            UIManager.getInstance().closeAll();
        }
        else
        {
            UIManager.getInstance().open(returnDisplay);
        }
    }

    Map<Config.Category, List<SlotMetadata>> getToolbars()
    {
        return Collections.EMPTY_MAP;
    }
}
