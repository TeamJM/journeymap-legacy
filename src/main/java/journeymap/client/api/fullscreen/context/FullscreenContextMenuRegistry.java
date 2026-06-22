package journeymap.client.api.fullscreen.context;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for fullscreen context menu providers supplied by JourneyMap integrations.
 */
public class FullscreenContextMenuRegistry
{
    private static final FullscreenContextMenuRegistry INSTANCE = new FullscreenContextMenuRegistry();

    private CopyOnWriteArrayList<FullscreenContextMenuProvider> providers = new CopyOnWriteArrayList<FullscreenContextMenuProvider>();

    public static FullscreenContextMenuRegistry getInstance()
    {
        return INSTANCE;
    }

    public void registerProvider(FullscreenContextMenuProvider provider)
    {
        if (provider == null)
        {
            return;
        }
        providers.addIfAbsent(provider);
    }

    public void unregisterProvider(FullscreenContextMenuProvider provider)
    {
        if (provider == null)
        {
            return;
        }
        providers.remove(provider);
    }

    public List<FullscreenContextMenuProvider> getProviders()
    {
        return Collections.unmodifiableList(providers);
    }
}
