package journeymap.client.api.fullscreen.context;

/**
 * Describes one entry in the fullscreen map context menu.
 */
public class FullscreenContextMenuEntry
{
    private String actionId;
    private String label;
    private String shortcut;
    private boolean enabled;
    private boolean interactive;
    private Integer backgroundColor;
    private int order;

    public FullscreenContextMenuEntry(String actionId, String label, String shortcut, boolean enabled, boolean interactive, Integer backgroundColor, int order)
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
