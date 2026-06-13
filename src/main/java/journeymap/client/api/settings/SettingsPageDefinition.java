package journeymap.client.api.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Defines one external settings page and the entries JourneyMap should render under it.
 */
public class SettingsPageDefinition
{
    private String pageId;
    private String titleKey;
    private String tooltipKey;
    private int order;
    private List<ExternalSettingEntry> entries;

    public SettingsPageDefinition(String pageId, String titleKey, int order, List<ExternalSettingEntry> entries)
    {
        this(pageId, titleKey, null, order, entries);
    }

    public SettingsPageDefinition(String pageId, String titleKey, String tooltipKey, int order, List<ExternalSettingEntry> entries)
    {
        this.pageId = pageId;
        this.titleKey = titleKey;
        this.tooltipKey = tooltipKey;
        this.order = order;
        this.entries = new ArrayList<ExternalSettingEntry>();
        if (entries != null)
        {
            this.entries.addAll(entries);
        }
        // Sort once during construction so the options screen can render pages without mutating providers.
        Collections.sort(this.entries, new Comparator<ExternalSettingEntry>()
        {
            @Override
            public int compare(ExternalSettingEntry first, ExternalSettingEntry second)
            {
                int orderCompare = Integer.compare(first.getOrder(), second.getOrder());
                return orderCompare != 0 ? orderCompare : first.getId().compareTo(second.getId());
            }
        });
    }

    public String getPageId()
    {
        return pageId;
    }

    public String getTitleKey()
    {
        return titleKey;
    }

    public String getTooltipKey()
    {
        return tooltipKey;
    }

    public int getOrder()
    {
        return order;
    }

    public List<ExternalSettingEntry> getEntries()
    {
        return Collections.unmodifiableList(entries);
    }
}
