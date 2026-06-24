package journeymap.client.api.fullscreen.context;

/**
 * Describes one entry in the fullscreen map context menu.
 */
public class FullscreenContextMenuEntry
{
    private final String actionId;
    private final String label;
    private final String shortcut;
    private final boolean enabled;
    private final boolean interactive;
    private final Integer backgroundColor;
    private final int order;

    /**
     * Use the named factories instead of exposing adjacent boolean parameters in the public API.
     */
    private FullscreenContextMenuEntry(String actionId, String label, String shortcut, boolean enabled,
                                       boolean interactive, Integer backgroundColor, int order)
    {
        this.actionId = actionId;
        this.label = label;
        this.shortcut = shortcut;
        this.enabled = enabled;
        this.interactive = interactive;
        this.backgroundColor = backgroundColor;
        this.order = order;
    }

    /**
     * Creates a clickable menu entry. The action id is passed back to the provider when the row is clicked.
     */
    public static FullscreenContextMenuEntry action(String actionId, String label)
    {
        if (actionId == null || actionId.length() == 0)
        {
            throw new IllegalArgumentException("actionId is required for interactive menu entries");
        }
        return create(actionId, label, true);
    }

    /**
     * Creates a visible row that can be used for coordinates or other read-only information.
     */
    public static FullscreenContextMenuEntry info(String label)
    {
        return create(null, label, false);
    }

    /**
     * Returns a copy with a shortcut label shown before the entry text.
     */
    public FullscreenContextMenuEntry withShortcut(String shortcut)
    {
        return new FullscreenContextMenuEntry(actionId, label, shortcut, enabled, interactive, backgroundColor, order);
    }

    /**
     * Returns a copy with the enabled state changed. Disabled entries are visible but should not run actions.
     */
    public FullscreenContextMenuEntry withEnabled(boolean enabled)
    {
        return new FullscreenContextMenuEntry(actionId, label, shortcut, enabled, interactive, backgroundColor, order);
    }

    /**
     * Returns a copy with an optional row background color.
     */
    public FullscreenContextMenuEntry withBackgroundColor(Integer backgroundColor)
    {
        return new FullscreenContextMenuEntry(actionId, label, shortcut, enabled, interactive, backgroundColor, order);
    }

    /**
     * Returns a copy with the sort order changed. Lower values should be rendered first.
     */
    public FullscreenContextMenuEntry withOrder(int order)
    {
        return new FullscreenContextMenuEntry(actionId, label, shortcut, enabled, interactive, backgroundColor, order);
    }

    public String getActionId()
    {
        return actionId;
    }

    public String getLabel()
    {
        return label;
    }

    public String getShortcut()
    {
        return shortcut;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Returns false for informational rows that should render like menu items but ignore clicks.
     */
    public boolean isInteractive()
    {
        return interactive;
    }

    public Integer getBackgroundColor()
    {
        return backgroundColor;
    }

    public int getOrder()
    {
        return order;
    }

    /**
     * Shared validation for both row types.
     */
    private static FullscreenContextMenuEntry create(String actionId, String label, boolean interactive)
    {
        if (label == null || label.length() == 0)
        {
            throw new IllegalArgumentException("label is required for fullscreen context menu entries");
        }
        return new FullscreenContextMenuEntry(actionId, label, null, true, interactive, null, 0);
    }
}
