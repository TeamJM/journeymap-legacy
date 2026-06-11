package journeymap.client.ui.fullscreen.context;

import journeymap.client.api.fullscreen.context.FullscreenContextMenuActionResult;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuContext;
import journeymap.client.Constants;
import journeymap.client.command.CmdTeleportWaypoint;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.Waypoint;
import journeymap.client.model.WaypointLifecycle;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.component.Button;
import journeymap.client.ui.fullscreen.Fullscreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class FullscreenContextMenu
{
    private static final int BUTTON_TEXT_PADDING = 4;
    private final FontRenderer fontRenderer = ForgeHelper.INSTANCE.getFontRenderer();
    private final Fullscreen owner;
    private final FullscreenContextMenuModel model = new FullscreenContextMenuModel();
    private final FullscreenContextActionService actionService = new FullscreenContextActionService();
    private final FullscreenContextMenuExtensionService extensionService = new FullscreenContextMenuExtensionService();
    private final List<Button> buttons = new ArrayList<>();
    private final Map<Button, FullscreenContextMenuItem> itemsByButton = new IdentityHashMap<>();
    private FullscreenContextTarget target;
    private FullscreenContextMenuContext context;
    private boolean open;
    private int x;
    private int y;
    private int width;
    private int height;

    public FullscreenContextMenu(Fullscreen owner)
    {
        this.owner = owner;
    }

    public boolean isOpen()
    {
        return open;
    }

    public void close()
    {
        open = false;
        buttons.clear();
        itemsByButton.clear();
        target = null;
        context = null;
    }

    public void openFor(FullscreenContextTarget target, int mouseX, int mouseY, int screenWidth, int screenHeight)
    {
        this.target = target;
        context = extensionService.createContext(target);
        rebuildButtons(buildMenuItems(target));
        int resolvedX = mouseX + width > screenWidth ? mouseX - width : mouseX;
        int resolvedY = mouseY + height > screenHeight ? mouseY - height : mouseY;
        this.x = Math.max(0, Math.min(resolvedX, Math.max(0, screenWidth - width)));
        this.y = Math.max(0, Math.min(resolvedY, Math.max(0, screenHeight - height)));
        layoutButtons();
        open = true;
    }

    public void draw(Minecraft mc, int mouseX, int mouseY)
    {
        if (!open)
        {
            return;
        }

        DrawUtil.drawRectangle(x - 1, y - 1, width + 2, height + 2, Color.black.getRGB(), 220);
        DrawUtil.drawRectangle(x, y, width, height, new Color(35, 35, 35).getRGB(), 230);
        for (Button button : buttons)
        {
            button.drawButton(mc, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (!open)
        {
            return false;
        }

        for (Button button : buttons)
        {
            if (button.mouseOver(mouseX, mouseY))
            {
                FullscreenContextMenuItem item = itemsByButton.get(button);
                if (item != null && item.isInteractive() && item.isEnabled() && mouseButton == 0)
                {
                    execute(item);
                }
                return true;
            }
        }

        close();
        return false;
    }

    public boolean keyTyped(char c, int keyCode)
    {
        if (!open)
        {
            return false;
        }

        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            close();
            return true;
        }

        boolean controlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (!controlDown && keyCode == Constants.KB_WAYPOINT.getKeyCode())
        {
            execute("create_waypoint");
            return true;
        }
        if (controlDown && keyCode == Constants.KB_WAYPOINT.getKeyCode())
        {
            execute("open_waypoint_manager");
            return true;
        }
        if (keyCode == Keyboard.KEY_O)
        {
            execute("open_settings");
            return true;
        }
        if (target instanceof MapLocationContextTarget && keyCode == Constants.KB_FULLSCREEN_TELEPORT.getKeyCode())
        {
            execute("teleport_here");
            return true;
        }
        if (target instanceof WaypointContextTarget && keyCode == Constants.KB_FULLSCREEN_EDIT_WAYPOINT.getKeyCode())
        {
            execute("edit_waypoint");
            return true;
        }
        if (target instanceof WaypointContextTarget && keyCode == Constants.KB_FULLSCREEN_DELETE_WAYPOINT.getKeyCode())
        {
            execute("delete_waypoint");
            return true;
        }
        if (target instanceof WaypointContextTarget && keyCode == Constants.KB_FULLSCREEN_TOGGLE_WAYPOINT.getKeyCode())
        {
            Waypoint waypoint = ((WaypointContextTarget) target).getWaypoint();
            if (!waypoint.isTemporary())
            {
                execute("toggle_waypoint_visibility");
                return true;
            }
        }
        return false;
    }

    private List<FullscreenContextMenuItem> buildMenuItems(FullscreenContextTarget target)
    {
        List<FullscreenContextMenuItem> items = new ArrayList<FullscreenContextMenuItem>(model.build(target));
        items.addAll(extensionService.buildItems(context));
        Collections.sort(items, new Comparator<FullscreenContextMenuItem>()
        {
            @Override
            public int compare(FullscreenContextMenuItem first, FullscreenContextMenuItem second)
            {
                return Integer.valueOf(first.getOrder()).compareTo(second.getOrder());
            }
        });
        return items;
    }

    private void rebuildButtons(List<FullscreenContextMenuItem> items)
    {
        buttons.clear();
        itemsByButton.clear();
        width = 0;
        height = 0;
        int buttonHeight = fontRenderer.FONT_HEIGHT + BUTTON_TEXT_PADDING;
        for (FullscreenContextMenuItem item : items)
        {
            Button button = new Button(applyShortcut(item));
            button.setDefaultStyle(false);
            button.setHeight(buttonHeight);
            button.fitWidth(fontRenderer);
            button.setEnabled(item.isEnabled());
            if (!item.isInteractive())
            {
                button.setDrawLabelShadow(false);
            }
            if (item.getBackgroundColor() != null)
            {
                button.setBackgroundColors(item.getBackgroundColor(), item.getBackgroundColor(), item.getBackgroundColor());
                button.setLabelColors(Color.white.getRGB(), Color.white.getRGB(), Color.lightGray.getRGB());
            }
            buttons.add(button);
            itemsByButton.put(button, item);
            width = Math.max(width, button.getWidth());
            height += buttonHeight;
        }

        width += 12;
    }

    private void layoutButtons()
    {
        int offsetY = y;
        for (Button button : buttons)
        {
            button.setWidth(width);
            button.setPosition(x, offsetY);
            offsetY += button.getHeight();
        }
    }

    private String applyShortcut(FullscreenContextMenuItem item)
    {
        String shortcut = item.getShortcut();
        if (shortcut == null || shortcut.isEmpty())
        {
            shortcut = resolveShortcut(item.getActionId());
        }
        if (shortcut == null || shortcut.isEmpty())
        {
            return item.getLabel();
        }
        return EnumChatFormatting.GREEN + shortcut + EnumChatFormatting.RESET + " " + item.getLabel();
    }

    private String resolveShortcut(String actionId)
    {
        if ("create_waypoint".equals(actionId))
        {
            return Constants.getKeyName(Constants.KB_WAYPOINT);
        }
        if ("open_waypoint_manager".equals(actionId))
        {
            return "Ctrl+" + Constants.getKeyName(Constants.KB_WAYPOINT);
        }
        if ("open_settings".equals(actionId))
        {
            return Keyboard.getKeyName(Keyboard.KEY_O);
        }
        if ("teleport_here".equals(actionId))
        {
            return Constants.getKeyName(Constants.KB_FULLSCREEN_TELEPORT);
        }
        if ("edit_waypoint".equals(actionId))
        {
            return Constants.getKeyName(Constants.KB_FULLSCREEN_EDIT_WAYPOINT);
        }
        if ("delete_waypoint".equals(actionId))
        {
            return Constants.getKeyName(Constants.KB_FULLSCREEN_DELETE_WAYPOINT);
        }
        if ("toggle_waypoint_visibility".equals(actionId))
        {
            return Constants.getKeyName(Constants.KB_FULLSCREEN_TOGGLE_WAYPOINT);
        }
        return null;
    }

    private void execute(FullscreenContextMenuItem item)
    {
        String actionId = item.getActionId();
        if ("copy_coordinates".equals(actionId))
        {
            actionService.copyCoordinates(target);
        }
        else if ("create_waypoint".equals(actionId))
        {
            actionService.openWaypointEditor(toLocationTarget(target), WaypointLifecycle.PERSISTENT, owner);
        }
        else if ("create_temporary_waypoint".equals(actionId))
        {
            actionService.quickCreateWaypoint(toLocationTarget(target), WaypointLifecycle.TEMPORARY);
            owner.clearContextSelection();
        }
        else if ("teleport_here".equals(actionId))
        {
            if (CmdTeleportWaypoint.isPermitted(ForgeHelper.INSTANCE.getClient()))
            {
                actionService.teleportTo(target);
                UIManager.getInstance().closeAll();
            }
        }
        else if ("open_waypoint_manager".equals(actionId))
        {
            UIManager.getInstance().openWaypointManager(null, owner);
        }
        else if ("open_settings".equals(actionId))
        {
            UIManager.getInstance().openOptionsManager();
        }
        else if ("edit_waypoint".equals(actionId) || "edit_waypoint_primary".equals(actionId))
        {
            actionService.openWaypointEditor(((WaypointContextTarget) target).getWaypoint(), owner);
        }
        else if ("toggle_waypoint_visibility".equals(actionId))
        {
            actionService.toggleWaypointVisibility(((WaypointContextTarget) target).getWaypoint());
            owner.clearContextSelection();
        }
        else if ("restore_waypoint".equals(actionId))
        {
            actionService.restoreWaypoint(((WaypointContextTarget) target).getWaypoint());
            owner.clearContextSelection();
        }
        else if ("delete_waypoint".equals(actionId))
        {
            actionService.deleteWaypoint(((WaypointContextTarget) target).getWaypoint());
            owner.clearContextSelection();
        }
        else
        {
            FullscreenContextMenuActionResult result = extensionService.execute(item, context);
            if (result == FullscreenContextMenuActionResult.CLOSE_MENU)
            {
                close();
            }
            return;
        }
        close();
    }

    private void execute(String actionId)
    {
        FullscreenContextMenuItem item = findItem(actionId);
        if (item != null)
        {
            execute(item);
        }
    }

    private FullscreenContextMenuItem findItem(String actionId)
    {
        for (FullscreenContextMenuItem item : itemsByButton.values())
        {
            if (actionId.equals(item.getActionId()))
            {
                return item;
            }
        }
        return null;
    }

    private MapLocationContextTarget toLocationTarget(FullscreenContextTarget target)
    {
        if (target instanceof MapLocationContextTarget)
        {
            return (MapLocationContextTarget) target;
        }

        Waypoint waypoint = ((WaypointContextTarget) target).getWaypoint();
        return new MapLocationContextTarget(waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.getY(), ForgeHelper.INSTANCE.getPlayerDimension());
    }
}
