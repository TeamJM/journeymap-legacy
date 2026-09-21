/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.waypoint;

import journeymap.client.Constants;
import journeymap.client.cartography.RGB;
import journeymap.client.data.WorldData;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.JMLogger;
import journeymap.client.model.Waypoint;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.ui.component.Button;
import journeymap.client.ui.component.ScrollPane;
import journeymap.client.ui.component.TextField;
import journeymap.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.world.WorldProvider;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class DimensionPickerPopup
{
    private static final int VISIBLE_ROWS = 10;
    private static final int PAD = 3;
    private static final int GUTTER = 12;
    private static final int GAP = 10;
    private static final int SEARCH_HEIGHT = 14;
    private static final int SCROLLBAR = 6;
    private static final int ROW_MARGIN = 4;
    private static final int LIST_TOP_PAD = 4;
    private static final int LIST_BOTTOM_PAD = 4;
    private static final int COLOR_FRAME = 0xA0A0A0;

    private final FontRenderer fr;
    private final Listener listener;
    private final TextField searchField;
    private final List<EntryButton> entries = new ArrayList<>();
    private final List<EntryButton> filtered = new ArrayList<>();
    private final ScrollPane scrollPane;
    private final int rowHeight;

    private boolean open;
    private Integer selectedDimension;
    private boolean scrollToSelectedPending;

    // Anchor
    private int anchorX, anchorY, anchorWidth, topLimit, screenWidth;

    // Layout
    private int x, y, width, height, listX, listWidth, listHeight;

    DimensionPickerPopup(FontRenderer fr, Listener listener)
    {
        this.fr = fr;
        this.listener = listener;
        this.rowHeight = fr.FONT_HEIGHT + 4;
        this.searchField = new TextField("", fr, 100, SEARCH_HEIGHT);

        this.scrollPane = new PickerScrollPane(ForgeHelper.INSTANCE.getClient(), filtered, rowHeight);
        this.scrollPane.setShowSelectionBox(false);
        this.scrollPane.setShowFrame(false);
    }

    boolean isOpen()
    {
        return open;
    }

    void open(List<WorldProvider> providers, WorldProvider selected)
    {
        rebuild(providers);
        selectedDimension = selected == null ? null : ForgeHelper.INSTANCE.getDimension(selected);
        searchField.setText("");
        searchField.setFocused(true);
        open = true;
        applyFilter();
        scrollToSelectedPending = true;
    }

    void close()
    {
        open = false;
        searchField.setFocused(false);
    }

    void setAnchor(int anchorX, int anchorY, int anchorWidth, int topLimit, int screenWidth)
    {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorWidth = anchorWidth;
        this.topLimit = topLimit;
        this.screenWidth = screenWidth;
    }

    private void rebuild(List<WorldProvider> providers)
    {
        entries.clear();

        Map<Integer, Integer> counts = new HashMap<>();
        int total = 0;
        for (Waypoint waypoint : WaypointStore.instance().getAll())
        {
            total++;
            for (Integer dim : waypoint.getDimensions())
            {
                counts.merge(dim, 1, Integer::sum);
            }
        }

        int playerDim = ForgeHelper.INSTANCE.getDimension(ForgeHelper.INSTANCE.getClient().thePlayer.worldObj.provider);

        entries.add(new EntryButton(null, null, Constants.getString("jm.waypoint.dimension_all"), total, false));

        for (WorldProvider provider : providers)
        {
            int dim = ForgeHelper.INSTANCE.getDimension(provider);
            String name;
            try
            {
                name = WorldData.getSafeDimensionName(provider);
            }
            catch (Exception e)
            {
                JMLogger.logOnce("Can't get dimension name from provider: ", e);
                name = Integer.toString(dim);
            }
            Integer count = counts.get(dim);
            entries.add(new EntryButton(provider, dim, name, count == null ? 0 : count, dim == playerDim));
        }
    }

    private void applyFilter()
    {
        String q = searchField.getText().trim().toLowerCase();

        filtered.clear();
        filtered.add(entries.get(0));
        for (int i = 1; i < entries.size(); i++)
        {
            EntryButton entry = entries.get(i);
            if (q.isEmpty() || entry.searchText.contains(q))
            {
                filtered.add(entry);
            }
        }

        scrollPane.scrollBy(-scrollPane.getAmountScrolled());
    }

    private boolean isSelected(EntryButton entry)
    {
        return Objects.equals(entry.dimension, selectedDimension);
    }

    private void updateLayout()
    {
        int contentWidth = 0;
        for (EntryButton entry : filtered)
        {
            contentWidth = Math.max(contentWidth, entry.getFitWidth(fr));
        }

        width = Math.max(anchorWidth, PAD + contentWidth + ROW_MARGIN + SCROLLBAR + PAD);
        width = Math.min(width, Math.max(60, screenWidth - 4));

        int slotHeight = scrollPane.getSlotHeight();
        int available = anchorY - topLimit - 4;
        int roomForRows = (available - SEARCH_HEIGHT - (PAD * 3) - LIST_TOP_PAD - LIST_BOTTOM_PAD) / slotHeight;
        int maxRows = Math.max(1, Math.min(VISIBLE_ROWS, roomForRows));

        listWidth = width - (PAD * 2);
        listHeight = (maxRows * slotHeight) + LIST_TOP_PAD + LIST_BOTTOM_PAD;
        height = PAD + SEARCH_HEIGHT + PAD + listHeight + PAD;

        x = Math.max(2, Math.min(anchorX, screenWidth - 2 - width));
        y = Math.max(topLimit, anchorY - 2 - height);

        listX = x + PAD;
        int listY = y + PAD + SEARCH_HEIGHT + PAD;

        searchField.setX(x + PAD + 1);
        searchField.setY(y + PAD);
        searchField.setWidth(width - (PAD * 2) - 2);

        scrollPane.setDimensions(listWidth, listHeight, 0, 0, listX, listY);
    }

    private void centerSelected()
    {
        scrollToSelectedPending = false;

        int index = -1;
        for (int i = 0; i < filtered.size(); i++)
        {
            if (isSelected(filtered.get(i)))
            {
                index = i;
                break;
            }
        }

        if (index < 0)
        {
            return;
        }

        int slotHeight = scrollPane.getSlotHeight();
        int visibleRows = (listHeight - LIST_TOP_PAD - LIST_BOTTOM_PAD) / slotHeight;
        int firstRow = index - ((visibleRows - 1) / 2);

        scrollPane.scrollBy((firstRow * slotHeight) - scrollPane.getAmountScrolled());
    }

    void scrollBy(int pixels)
    {
        scrollPane.scrollBy(pixels);
    }

    void draw(int mouseX, int mouseY, float partialTicks)
    {
        if (!open)
        {
            return;
        }

        updateLayout();

        if (scrollToSelectedPending)
        {
            centerSelected();
        }

        DrawUtil.drawRectangle(x - 1, y - 1, width + 2, height + 2, COLOR_FRAME, 255);
        DrawUtil.drawRectangle(x, y, width, height, RGB.BLACK_RGB, 235);

        searchField.drawTextBox();

        scrollPane.drawScreen(mouseX, mouseY, partialTicks);
    }

    void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (!open)
        {
            return;
        }

        updateLayout();

        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height)
        {
            close();
            return;
        }

        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.setFocused(true);

        if (mouseButton != 0 || mouseX >= listX + listWidth - SCROLLBAR)
        {
            return;
        }

        Button clicked = scrollPane.mouseClicked(mouseX, mouseY, mouseButton);
        if (clicked instanceof EntryButton)
        {
            WorldProvider provider = ((EntryButton) clicked).provider;
            close();
            listener.onDimensionPicked(provider);
        }
    }

    void keyTyped(char c, int i)
    {
        if (!open)
        {
            return;
        }

        if (i == Keyboard.KEY_ESCAPE)
        {
            close();
            return;
        }

        String before = searchField.getText();
        searchField.textboxKeyTyped(c, i);
        if (!searchField.getText().equals(before))
        {
            applyFilter();
        }
    }

    interface Listener
    {
        void onDimensionPicked(WorldProvider provider);
    }

    private static class PickerScrollPane extends ScrollPane
    {
        PickerScrollPane(Minecraft mc, List<? extends Scrollable> items, int itemHeight)
        {
            super(mc, 100, 100, items, itemHeight, 0);
        }

        @Override
        protected int getContentHeight()
        {
            return Math.max(super.getContentHeight() + LIST_BOTTOM_PAD, (bottom - top) - 4);
        }

        @Override
        protected int getScrollBarX()
        {
            return paneWidth - SCROLLBAR;
        }

        @Override
        public int getWidth()
        {
            return paneWidth;
        }
    }

    private class EntryButton extends Button
    {
        final WorldProvider provider;
        final Integer dimension;
        final String name;
        final String idLabel;
        final int count;
        final boolean playerDimension;
        final String searchText;

        EntryButton(WorldProvider provider, Integer dimension, String name, int count, boolean playerDimension)
        {
            super(0, 0, "");
            this.provider = provider;
            this.dimension = dimension;
            this.name = name;
            this.idLabel = dimension == null ? "" : "(" + dimension + ")";
            this.count = count;
            this.playerDimension = playerDimension;
            this.searchText = (dimension == null ? name : name + " " + dimension).toLowerCase();

            this.id = 1;
            resetLabelColors();
            setHeight(rowHeight);
        }

        @Override
        public void setScrollableWidth(int width)
        {
            setWidth(width - SCROLLBAR);
        }

        @Override
        public int getFitWidth(FontRenderer fr)
        {
            int fit = GUTTER + fr.getStringWidth(name);
            if (!idLabel.isEmpty())
            {
                fit += 4 + fr.getStringWidth(idLabel);
            }
            return fit + GAP + fr.getStringWidth(Integer.toString(count));
        }

        @Override
        public void drawScrollable(Minecraft mc, int mouseX, int mouseY)
        {
            drawRow(mousePressed(mc, mouseX, mouseY));
        }

        @Override
        public void drawPartialScrollable(Minecraft mc, int px, int py, int pWidth, int pHeight)
        {
            if (pHeight <= 0)
            {
                return;
            }

            int scale = ForgeHelper.INSTANCE.getScaledResolution().getScaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(px * scale, mc.displayHeight - ((py + pHeight) * scale), pWidth * scale, pHeight * scale);
            drawRow(false);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        private void drawRow(boolean active)
        {
            int textY = getY() + 1 + ((getHeight() - fr.FONT_HEIGHT) / 2);

            if (active)
            {
                DrawUtil.drawRectangle(getX(), getY(), getWidth(), getHeight(), smallBgHoverColor, 180);
            }

            if (playerDimension)
            {
                fr.drawString(">", getX() + 1, textY, RGB.GREEN_RGB);
            }

            int textX = getX() + GUTTER;
            int nameColor = isSelected(this) ? RGB.CYAN_RGB : (active ? hoverLabelColor : labelColor);
            fr.drawStringWithShadow(name, textX, textY, nameColor);

            if (!idLabel.isEmpty())
            {
                fr.drawString(idLabel, textX + fr.getStringWidth(name) + 4, textY, RGB.GRAY_RGB);
            }

            String countLabel = Integer.toString(count);
            fr.drawString(countLabel, getX() + getWidth() - 1 - fr.getStringWidth(countLabel), textY,
                    count > 0 ? RGB.LIGHT_GRAY_RGB : RGB.GRAY_RGB);
        }
    }
}
