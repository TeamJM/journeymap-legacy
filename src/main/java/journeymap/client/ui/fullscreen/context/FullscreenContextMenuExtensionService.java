package journeymap.client.ui.fullscreen.context;

import journeymap.client.api.fullscreen.context.FullscreenContextMenuActionResult;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuContext;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuEntry;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuProvider;
import journeymap.client.api.fullscreen.context.FullscreenContextMenuRegistry;
import journeymap.client.api.fullscreen.context.FullscreenContextTargetType;
import journeymap.client.model.Waypoint;
import journeymap.common.Journeymap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FullscreenContextMenuExtensionService
{
    public FullscreenContextMenuContext createContext(FullscreenContextTarget target)
    {
        if (target instanceof WaypointContextTarget)
        {
            Waypoint waypoint = ((WaypointContextTarget) target).getWaypoint();
            List<Integer> dimensions = new ArrayList<Integer>(waypoint.getDimensions());
            return new FullscreenContextMenuContext(FullscreenContextTargetType.WAYPOINT,
                    waypoint.getX(),
                    waypoint.getY(),
                    waypoint.getY(),
                    waypoint.getZ(),
                    waypoint.getX() >> 4,
                    waypoint.getZ() >> 4,
                    dimensions.isEmpty() ? 0 : dimensions.get(0),
                    Collections.unmodifiableList(dimensions),
                    waypoint);
        }

        MapLocationContextTarget locationTarget = (MapLocationContextTarget) target;
        return new FullscreenContextMenuContext(FullscreenContextTargetType.MAP_LOCATION,
                locationTarget.getX(),
                locationTarget.getResolvedY(),
                locationTarget.getDisplayY(),
                locationTarget.getZ(),
                locationTarget.getChunkX(),
                locationTarget.getChunkZ(),
                locationTarget.getDimension(),
                Collections.singletonList(locationTarget.getDimension()),
                null);
    }

    public List<FullscreenContextMenuItem> buildItems(FullscreenContextMenuContext context)
    {
        List<FullscreenContextMenuItem> items = new ArrayList<FullscreenContextMenuItem>();
        for (FullscreenContextMenuProvider provider : FullscreenContextMenuRegistry.getInstance().getProviders())
        {
            try
            {
                List<FullscreenContextMenuEntry> contributedItems = provider.getMenuItems(context);
                if (contributedItems == null)
                {
                    continue;
                }
                for (FullscreenContextMenuEntry entry : contributedItems)
                {
                    FullscreenContextMenuItem item = createItem(provider, entry);
                    if (item != null)
                    {
                        items.add(item);
                    }
                }
            }
            catch (Throwable t)
            {
                Journeymap.getLogger().error("Failed to build fullscreen context menu items for provider {}: {}", provider.getClass().getName(), String.valueOf(t));
            }
        }
        return items;
    }

    public FullscreenContextMenuActionResult execute(FullscreenContextMenuItem item, FullscreenContextMenuContext context)
    {
        FullscreenContextMenuProvider provider = item.getProvider();
        if (provider == null)
        {
            return FullscreenContextMenuActionResult.CLOSE_MENU;
        }

        try
        {
            FullscreenContextMenuActionResult result = provider.onMenuItemClicked(context, item.getActionId());
            return result == null ? FullscreenContextMenuActionResult.KEEP_MENU_OPEN : result;
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Failed to execute fullscreen context menu action {} for provider {}: {}", item.getActionId(), provider.getClass().getName(), String.valueOf(t));
            return FullscreenContextMenuActionResult.KEEP_MENU_OPEN;
        }
    }

    private FullscreenContextMenuItem createItem(FullscreenContextMenuProvider provider, FullscreenContextMenuEntry entry)
    {
        if (entry == null || entry.getActionId() == null || entry.getActionId().trim().isEmpty() || entry.getLabel() == null)
        {
            return null;
        }
        return new FullscreenContextMenuItem(entry.getActionId(),
                entry.getLabel(),
                entry.getShortcut(),
                entry.isEnabled(),
                entry.isInteractive(),
                entry.getBackgroundColor(),
                entry.getOrder(),
                provider);
    }
}
