/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.fullscreen.context;

import journeymap.client.api.fullscreen.context.FullscreenContextMenuContext;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuEntry;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuProvider;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuRegistry;
import journeymap.client.cartography.RGB;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.LogFormatter;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.ui.component.JmUI;
import journeymap.common.Journeymap;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Renders registered fullscreen context menu entries and dispatches selected actions.
 */
public class FullscreenContextMenu
{
    private static final int OUTER_PADDING = 2;
    private static final int HORIZONTAL_PADDING = 6;
    private static final int SHORTCUT_GAP = 12;
    private static final int ROW_PADDING = 2;
    private static final int MENU_BACKGROUND = RGB.BLACK_RGB;
    private static final int MENU_BORDER = RGB.DARK_GRAY_RGB;
    private static final int ROW_HOVER = 0x2F5D7C;
    private static final int ROW_DISABLED = RGB.DARK_GRAY_RGB;
    private static final int TEXT = RGB.WHITE_RGB;
    private static final int TEXT_DISABLED = RGB.GRAY_RGB;
    private static final int TEXT_SHORTCUT = 0x8CE86D;

    private final Logger logger = Journeymap.getLogger();
    private final JourneyMapFullscreenContextMenuProvider defaultProvider;
    private final FullscreenContextMenuContext context;
    private final List<MenuItem> items = new ArrayList<MenuItem>();
    private int x;
    private int y;
    private int width;
    private int height;
    private int rowHeight;

    public FullscreenContextMenu(FullscreenContextMenuContext context, JmUI returnDisplay, int mouseX, int mouseY, int screenWidth, int screenHeight)
    {
        this.context = context;
        this.defaultProvider = new JourneyMapFullscreenContextMenuProvider(returnDisplay);
        collectItems();
        layout(mouseX, mouseY, screenWidth, screenHeight);
    }

    public boolean isEmpty()
    {
        return items.isEmpty();
    }

    public void draw(int mouseX, int mouseY, int screenWidth, int screenHeight)
    {
        if (items.isEmpty())
        {
            return;
        }

        clampToScreen(screenWidth, screenHeight);
        FontRenderer fontRenderer = ForgeHelper.INSTANCE.getFontRenderer();
        DrawUtil.drawRectangle(x, y, width, height, MENU_BACKGROUND, 220);
        DrawUtil.drawRectangle(x, y, width, 1, MENU_BORDER, 255);
        DrawUtil.drawRectangle(x, y + height - 1, width, 1, MENU_BORDER, 255);
        DrawUtil.drawRectangle(x, y, 1, height, MENU_BORDER, 255);
        DrawUtil.drawRectangle(x + width - 1, y, 1, height, MENU_BORDER, 255);

        for (int i = 0; i < items.size(); i++)
        {
            MenuItem item = items.get(i);
            FullscreenContextMenuEntry entry = item.entry;
            int rowY = y + OUTER_PADDING + (i * rowHeight);
            boolean hovered = isMouseOverRow(mouseX, mouseY, i);
            Integer backgroundColor = entry.getBackgroundColor();

            if (backgroundColor != null)
            {
                DrawUtil.drawRectangle(x + OUTER_PADDING, rowY, width - (OUTER_PADDING * 2), rowHeight, backgroundColor, entry.isEnabled() ? 190 : 100);
            }
            else if (hovered && entry.isEnabled() && entry.isInteractive())
            {
                DrawUtil.drawRectangle(x + OUTER_PADDING, rowY, width - (OUTER_PADDING * 2), rowHeight, ROW_HOVER, 210);
            }
            else if (!entry.isEnabled())
            {
                DrawUtil.drawRectangle(x + OUTER_PADDING, rowY, width - (OUTER_PADDING * 2), rowHeight, ROW_DISABLED, 90);
            }

            drawEntryText(fontRenderer, entry, rowY);
        }

        RenderHelper.disableStandardItemLighting();
    }

    public boolean contains(int mouseX, int mouseY)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (mouseButton != 0 || !contains(mouseX, mouseY))
        {
            return false;
        }

        int row = (mouseY - y - OUTER_PADDING) / rowHeight;
        if (row < 0 || row >= items.size())
        {
            return true;
        }

        MenuItem item = items.get(row);
        FullscreenContextMenuEntry entry = item.entry;
        if (!entry.isEnabled() || !entry.isInteractive())
        {
            return true;
        }

        try
        {
            item.provider.onMenuItemClicked(context, entry.getActionId());
        }
        catch (Exception e)
        {
            logger.error("Error handling fullscreen context menu item {}: {}", entry.getActionId(), LogFormatter.toString(e));
        }
        return true;
    }

    public boolean keyTyped(char c, int keyCode)
    {
        return defaultProvider.keyTyped(context, keyCode);
    }

    /**
     * Providers are isolated so a bad integration cannot break the fullscreen map UI.
     */
    private void collectItems()
    {
        addProviderItems(defaultProvider);
        List<FullscreenContextMenuProvider> providers = FullscreenContextMenuRegistry.getInstance().getProviders();
        for (FullscreenContextMenuProvider provider : providers)
        {
            addProviderItems(provider);
        }

        Collections.sort(items, new Comparator<MenuItem>()
        {
            @Override
            public int compare(MenuItem first, MenuItem second)
            {
                return first.entry.getOrder() - second.entry.getOrder();
            }
        });
    }

    private void addProviderItems(FullscreenContextMenuProvider provider)
    {
        try
        {
            List<FullscreenContextMenuEntry> entries = provider.getMenuItems(context);
            if (entries == null)
            {
                return;
            }

            for (FullscreenContextMenuEntry entry : entries)
            {
                if (entry != null && entry.getLabel() != null && entry.getLabel().length() > 0)
                {
                    items.add(new MenuItem(provider, entry));
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Error collecting fullscreen context menu entries: {}", LogFormatter.toString(e));
        }
    }

    private void layout(int mouseX, int mouseY, int screenWidth, int screenHeight)
    {
        FontRenderer fontRenderer = ForgeHelper.INSTANCE.getFontRenderer();
        rowHeight = fontRenderer.FONT_HEIGHT + (ROW_PADDING * 2);
        int labelWidth = 0;
        int shortcutWidth = 0;

        for (MenuItem item : items)
        {
            FullscreenContextMenuEntry entry = item.entry;
            labelWidth = Math.max(labelWidth, fontRenderer.getStringWidth(entry.getLabel()));
            if (entry.getShortcut() != null && entry.getShortcut().length() > 0)
            {
                shortcutWidth = Math.max(shortcutWidth, fontRenderer.getStringWidth(entry.getShortcut()) + SHORTCUT_GAP);
            }
        }

        width = labelWidth + shortcutWidth + (HORIZONTAL_PADDING * 2) + (OUTER_PADDING * 2);
        height = (items.size() * rowHeight) + (OUTER_PADDING * 2);
        x = mouseX;
        y = mouseY;
        clampToScreen(screenWidth, screenHeight);
    }

    private void drawEntryText(FontRenderer fontRenderer, FullscreenContextMenuEntry entry, int rowY)
    {
        int textColor = entry.isEnabled() ? TEXT : TEXT_DISABLED;
        int textX = x + OUTER_PADDING + HORIZONTAL_PADDING;
        int textY = rowY + ROW_PADDING;
        String shortcut = entry.getShortcut();

        if (shortcut != null && shortcut.length() > 0)
        {
            fontRenderer.drawString(shortcut, textX, textY, TEXT_SHORTCUT);
            textX += fontRenderer.getStringWidth(shortcut) + 4;
        }

        fontRenderer.drawString(entry.getLabel(), textX, textY, textColor);
    }

    /**
     * Keeps the menu fully visible when opened near the game window edges.
     */
    private void clampToScreen(int screenWidth, int screenHeight)
    {
        x = Math.max(0, Math.min(x, screenWidth - width));
        y = Math.max(0, Math.min(y, screenHeight - height));
    }

    private boolean isMouseOverRow(int mouseX, int mouseY, int row)
    {
        int rowY = y + OUTER_PADDING + (row * rowHeight);
        return mouseX >= x + OUTER_PADDING && mouseX < x + width - OUTER_PADDING && mouseY >= rowY && mouseY < rowY + rowHeight;
    }

    public static class MenuItem
    {
        private final FullscreenContextMenuProvider provider;
        private final FullscreenContextMenuEntry entry;

        private MenuItem(FullscreenContextMenuProvider provider, FullscreenContextMenuEntry entry)
        {
            this.provider = provider;
            this.entry = entry;
        }
    }
}
