package journeymap.client.api.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsPageRegistry
{
    private static final SettingsPageRegistry INSTANCE = new SettingsPageRegistry();

    private Map<String, SettingsPageProvider> providers = new LinkedHashMap<String, SettingsPageProvider>();

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

        SettingsPageDefinition definition = provider.getPageDefinition();
        if (definition == null || definition.getPageId() == null)
        {
            return;
        }

        providers.put(definition.getPageId(), provider);
    }

    public synchronized void unregisterPage(String pageId)
    {
        if (pageId == null)
        {
            return;
        }
        providers.remove(pageId);
    }

    public synchronized List<SettingsPageDefinition> getPages()
    {
        List<SettingsPageDefinition> pages = new ArrayList<SettingsPageDefinition>();
        for (SettingsPageProvider provider : providers.values())
        {
            SettingsPageDefinition definition = provider.getPageDefinition();
            if (definition != null)
            {
                pages.add(definition);
            }
        }

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
