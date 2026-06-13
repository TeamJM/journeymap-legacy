package journeymap.client.api.fullscreen.context;

import java.util.List;

/**
 * Provides additional entries for the fullscreen map right-click menu.
 */
public interface FullscreenContextMenuProvider
{
    /**
     * Returns the entries that should be appended for the current map target.
     */
    List<FullscreenContextMenuEntry> getMenuItems(FullscreenContextMenuContext context);

    /**
     * Handles a click for an entry previously returned by this provider.
     */
    boolean onMenuItemClicked(FullscreenContextMenuContext context, String actionId);
}
