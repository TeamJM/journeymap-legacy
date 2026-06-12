package journeymap.client.ui.option;

import journeymap.client.Constants;
import journeymap.client.api.settings.ExternalSettingEntry;
import journeymap.client.api.settings.SettingsPageDefinition;
import journeymap.client.ui.component.ScrollListPane;
import net.minecraft.client.resources.I18n;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExternalSettingsCategorySlot extends CategorySlot
{
    protected SettingsPageDefinition pageDefinition;
    protected Map<String, SlotMetadata> slotMetadataById = new LinkedHashMap<String, SlotMetadata>();

    public ExternalSettingsCategorySlot(SettingsPageDefinition pageDefinition)
    {
        super(resolveRequired(pageDefinition.getTitleKey()), resolveTooltip(pageDefinition), pageDefinition.getOrder(), pageDefinition.getPageId(),
                false);
        this.pageDefinition = pageDefinition;
    }

    @Override
    public List<ScrollListPane.ISlot> getChildSlots(int listWidth, int columnWidth)
    {
        ensureSlotMetadata();
        childMetadataList.clear();
        for (ExternalSettingEntry entry : pageDefinition.getEntries())
        {
            SlotMetadata slotMetadata = slotMetadataById.get(entry.getId());
            if (slotMetadata != null)
            {
                slotMetadata.getButton().refresh();
                slotMetadata.getButton().setEnabled(entry.isEnabled());
                if (entry.isVisible())
                {
                    childMetadataList.add(slotMetadata);
                }
            }
        }
        return super.getChildSlots(listWidth, columnWidth);
    }

    protected void ensureSlotMetadata()
    {
        if (!slotMetadataById.isEmpty())
        {
            return;
        }

        for (ExternalSettingEntry entry : pageDefinition.getEntries())
        {
            SlotMetadata slotMetadata = ExternalSettingsSlotFactory.createSlot(entry);
            if (slotMetadata != null)
            {
                slotMetadataById.put(entry.getId(), slotMetadata);
            }
        }
    }

    protected static String resolveRequired(String key)
    {
        return Constants.getString(key);
    }

    protected static String resolveTooltip(SettingsPageDefinition pageDefinition)
    {
        if (pageDefinition.getTooltipKey() != null)
        {
            String tooltip = I18n.format(pageDefinition.getTooltipKey());
            if (!tooltip.equals(pageDefinition.getTooltipKey()))
            {
                return tooltip;
            }
        }
        return Constants.getString("jm.settings.external_page.tooltip");
    }
}
