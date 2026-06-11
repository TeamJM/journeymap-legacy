package journeymap.client.api.fullscreen.context;

import java.util.List;

public interface FullscreenContextMenuProvider
{
    List<FullscreenContextMenuEntry> getMenuItems(FullscreenContextMenuContext context);

    FullscreenContextMenuActionResult onMenuItemClicked(FullscreenContextMenuContext context, String actionId);
}
