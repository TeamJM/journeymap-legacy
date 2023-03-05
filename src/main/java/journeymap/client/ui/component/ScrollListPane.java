/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.component;

import journeymap.client.ui.option.SlotMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Adapted from GuiListExtended
 */
public class ScrollListPane<T extends ScrollListPane.ISlot> extends GuiSlot
{
    final JmUI parent;
    public SlotMetadata lastTooltipMetadata;
    public String[] lastTooltip;
    public long lastTooltipTime;
    public long hoverDelay = 800;
    int hpad = 12;
    List<T> rootSlots;
    List<ISlot> currentSlots = new ArrayList<ISlot>(0);
    SlotMetadata lastPressed;
    int lastClickedIndex;
    int scrollbarX;
    int listWidth;
    boolean alignTop;

    public ScrollListPane(JmUI parent, Minecraft mc, int width, int height, int top, int bottom, int slotHeight)
    {
        super(mc, width, height, top, bottom, slotHeight);
        this.parent = parent;
        setDimensions(width, height, top, bottom);
    }

    public void setDimensions(int width, int height, int top, int bottom)
    {
        // 1.7
        // super.func_148122_a(width, height, top, bottom);

        // 1.8
        super.setDimensions(width, height, top, bottom);
        scrollbarX = this.width - (hpad);
        listWidth = this.width - (hpad * 4);
    }

    @Override
    protected int getSize()
    {
        return this.currentSlots == null ? 0 : currentSlots.size();
    }

    public void setSlots(List<T> slots)
    {
        this.rootSlots = slots;
        updateSlots();
    }

    public List<T> getRootSlots()
    {
        return rootSlots;
    }

    public void updateSlots()
    {
        int sizeBefore = currentSlots.size();
        this.currentSlots.clear();

        int columnWidth = 0;
        for (ISlot slot : rootSlots)
        {
            columnWidth = Math.max(columnWidth, slot.getColumnWidth());
        }

        for (ISlot slot : rootSlots)
        {
            currentSlots.add(slot);
            List<? extends ISlot> children = slot.getChildSlots(listWidth, columnWidth);
            if (children != null && !children.isEmpty())
            {
                currentSlots.addAll(children);
            }
        }
        int sizeAfter = currentSlots.size();

        if (sizeBefore < sizeAfter)
        {
            scrollBy(-(sizeAfter * slotHeight));
            scrollBy(lastClickedIndex * slotHeight);
        }
    }

    public void scrollTo(ISlot slot)
    {
        scrollBy(-(currentSlots.size() * slotHeight));
        scrollBy(currentSlots.indexOf(slot) * slotHeight);
    }

    /**
     * Handles mouse input.
     */
    public void handleMouseInput()
    {
        // TODO ?
        // super.handleMouseInput();
    }

    /**
     * The element in the slot that was clicked, boolean for whether it was double clicked or not
     */
    protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY)
    {

    }

    @Override
    public boolean isSelected(int p_148131_1_)
    {
        return false;
    }

    protected void drawBackground()
    {

    }

    protected void drawSlot(int slotIndex, int x, int y, int slotHeight, Tessellator tessellator, int mouseX, int mouseY)
    {
        // 1.7
        //boolean selected = this.func_148124_c(mouseX, mouseY) == slotIndex;

        // 1.8
        boolean selected = this.getSlotIndexFromScreenCoords(mouseX, mouseY) == slotIndex;
        SlotMetadata tooltipMetadata = this.getSlot(slotIndex).drawSlot(slotIndex, x, y, this.getListWidth(), slotHeight, mouseX, mouseY, selected);
        if (tooltipMetadata != null && !Arrays.equals(tooltipMetadata.getTooltip(), lastTooltip))
        {
            lastTooltipMetadata = tooltipMetadata;
            lastTooltip = tooltipMetadata.getTooltip();
            lastTooltipTime = System.currentTimeMillis();
        }
    }

    /**
     * Gets the width of the list
     */
    public int getListWidth()
    {
        return listWidth;
    }

    public boolean mousePressed(int mouseX, int mouseY, int mouseEvent)
    {
        // 1.7
        // boolean overSlot = this.func_148141_e(mouseY);

        // 1.8
        boolean overSlot = this.isMouseYWithinSlotBounds(mouseY);

        if (overSlot)
        {
            // 1.7
            // int slotIndex = this.func_148124_c(mouseX, mouseY);

            // 1.8
            int slotIndex = this.getSlotIndexFromScreenCoords(mouseX, mouseY);

            if (slotIndex >= 0)
            {
                int i1 = this.left + hpad + this.width / 2 - this.getListWidth() / 2 + 2;
                int j1 = this.top + 4 - this.getAmountScrolled() + slotIndex * this.slotHeight + this.headerPadding;
                int relativeX = mouseX - i1;
                int relativeY = mouseY - j1;
                lastClickedIndex = -1;
                if (this.getSlot(slotIndex).mousePressed(slotIndex, mouseX, mouseY, mouseEvent, relativeX, relativeY))
                {
                    // 1.7
                    //this.func_148143_b(false);

                    // 1.8
                    this.setEnabled(false);
                    lastClickedIndex = slotIndex;
                    lastPressed = this.getSlot(slotIndex).getLastPressed();
                    updateSlots();
                    return true;
                }
            }
        }

        return false;
    }


    public boolean mouseReleased(int x, int y, int mouseEvent)
    {
        for (int slotIndex = 0; slotIndex < this.getSize(); ++slotIndex)
        {
            int i1 = this.left + hpad + this.width / 2 - this.getListWidth() / 2 + 2;
            int j1 = this.top + 4 - this.getAmountScrolled() + slotIndex * this.slotHeight + this.headerPadding;
            int relativeX = x - i1;
            int relativeY = y - j1;
            this.getSlot(slotIndex).mouseReleased(slotIndex, x, y, mouseEvent, relativeX, relativeY);
        }

        // 1.7
        //this.func_148143_b(true);

        // 1.8
        this.setEnabled(true);

        lastPressed = null;
        return false;
    }

    /**
     * Gets the ISlot object for the given index
     */
    public ISlot getSlot(int index)
    {
        return currentSlots.get(index);
    }

    public SlotMetadata getLastPressed()
    {
        return lastPressed;
    }

    public void resetLastPressed()
    {
        lastPressed = null;
    }

    public ISlot getLastPressedParentSlot()
    {
        if (lastPressed != null)
        {
            for (ISlot slot : rootSlots)
            {
                if (slot.contains(lastPressed))
                {
                    return slot;
                }
            }
        }
        return null;
    }

    public boolean keyTyped(char c, int i)
    {
        for (int slotIndex = 0; slotIndex < this.getSize(); ++slotIndex)
        {
            if (this.getSlot(slotIndex).keyTyped(c, i))
            {
                lastClickedIndex = slotIndex;
                lastPressed = this.getSlot(slotIndex).getLastPressed();
                updateSlots();
                return true;
            }
        }
        return false;
    }

    @Override
    protected int getScrollBarX()
    {
        return scrollbarX;
    }

    @Override
    protected void drawContainerBackground(Tessellator tessellator)
    {
        parent.drawGradientRect(0, top, this.width, top + this.height, -1072689136, -804253680);
    }

    @Override
    protected int getContentHeight()
    {
        int contentHeight = super.getContentHeight();
        if (alignTop)
        {
            contentHeight = Math.max((this.bottom - this.top) - 4, contentHeight);
        }
        return contentHeight;
    }

    public void setAlignTop(boolean alignTop)
    {
        this.alignTop = alignTop;
    }


    public interface ISlot
    {
        Collection<SlotMetadata> getMetadata();

        /**
         * Returns SlotMetadata of item hovered, if any.
         */
        // 1.7 SlotMetadata drawSlot(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean isSelected);
        SlotMetadata drawSlot(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected);

        /**
         * Returns true if the mouse has been pressed on a control in this slot.
         */
        boolean mousePressed(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY);

        /**
         * Returns array of strings to display in a hover if the mouse is over the slot
         */
        String[] mouseHover(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY);

        /**
         * Fired when the mouse button is released. Arguments: index, x, y, mouseEvent, relativeX, relativeY
         */
        void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY);

        /**
         * Called when a key is pressed. Return true to prevent event propagation further.
         */
        boolean keyTyped(char c, int i);

        List<? extends ISlot> getChildSlots(int listWidth, int columnWidth);

        SlotMetadata getLastPressed();

        void setEnabled(boolean enabled);

        int getColumnWidth();

        boolean contains(SlotMetadata slotMetadata);
    }
}
