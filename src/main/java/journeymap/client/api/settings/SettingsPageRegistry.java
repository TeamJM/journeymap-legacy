package journeymap.client.api.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for external settings pages shown in the JourneyMap options GUI.
 */
public class SettingsPageRegistry
{
    private static final SettingsPageRegistry INSTANCE = new SettingsPageRegistry();

    private Map<String, SettingsPageDefinition> pageDefinitions = new LinkedHashMap<String, SettingsPageDefinition>();

    public static SettingsPageRegistry getInstance()
    {
        return INSTANCE;
    }

    public synchronized void registerPage(SettingsPageProvider provider)
    {
        if (provider == null)
        {
            return;
        }

        registerPage(provider.getPageDefinition());
    }

    public synchronized void registerPage(SettingsPageDefinition definition)
    {
        if (definition == null || definition.getPageId() == null)
        {
            return;
        }

        pageDefinitions.put(definition.getPageId(), definition);
    }

    public synchronized void unregisterPage(String pageId)
    {
        if (pageId == null)
        {
            return;
        }
        pageDefinitions.remove(pageId);
    }

    public synchronized List<SettingsPageDefinition> getPages()
    {
        List<SettingsPageDefinition> pages = new ArrayList<SettingsPageDefinition>();
        pages.addAll(pageDefinitions.values());

        // Keep page order stable so reopening the options GUI does not reshuffle external pages.
        Collections.sort(pages, new Comparator<SettingsPageDefinition>()
        {
            @Override
            public int compare(SettingsPageDefinition first, SettingsPageDefinition second)
            {
                int orderCompare = Integer.compare(first.getOrder(), second.getOrder());
                return orderCompare != 0 ? orderCompare : first.getPageId().compareTo(second.getPageId());
            }
        });
        return pages;
    }
}
