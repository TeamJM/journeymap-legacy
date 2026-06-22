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
     * @param enabled false when the row should be visible but disabled
     * @param interactive false when the row is informational and should ignore clicks
     */
    public FullscreenContextMenuEntry(String actionId, String label, String shortcut, boolean enabled, boolean interactive,
                                      Integer backgroundColor, int order)
    {
        this.actionId = actionId;
        this.label = label;
        this.shortcut = shortcut;
        this.enabled = enabled;
        this.interactive = interactive;
        this.backgroundColor = backgroundColor;
        this.order = order;
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
}
