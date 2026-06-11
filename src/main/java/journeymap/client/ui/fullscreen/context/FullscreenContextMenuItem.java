package journeymap.client.ui.fullscreen.context;

import journeymap.client.api.fullscreen.context.FullscreenContextMenuProvider;

public class FullscreenContextMenuItem
{
    private final String actionId;
    private final String label;
    private final String shortcut;
    private final boolean enabled;
    private final boolean interactive;
    private final Integer backgroundColor;
    private final int order;
    private final FullscreenContextMenuProvider provider;

    public FullscreenContextMenuItem(String actionId, String label, String shortcut, boolean enabled, boolean interactive, Integer backgroundColor, int order)
    {
        this(actionId, label, shortcut, enabled, interactive, backgroundColor, order, null);
    }

    public FullscreenContextMenuItem(String actionId, String label, String shortcut, boolean enabled, boolean interactive, Integer backgroundColor, int order, FullscreenContextMenuProvider provider)
    {
        this.actionId = actionId;
        this.label = label;
        this.shortcut = shortcut;
        this.enabled = enabled;
        this.interactive = interactive;
        this.backgroundColor = backgroundColor;
        this.order = order;
        this.provider = provider;
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

    public FullscreenContextMenuProvider getProvider()
    {
        return provider;
    }
}
