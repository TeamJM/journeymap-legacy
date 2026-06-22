package journeymap.client.ui.option;

import journeymap.client.Constants;
import journeymap.client.api.settings.ExternalSettingEntry;
import journeymap.client.api.settings.SettingsPageDefinition;
import journeymap.client.api.settings.ExternalSettingEntry.ListBinding;
import journeymap.client.cartography.RGB;
import journeymap.client.ui.component.BindingBooleanButton;
import journeymap.client.ui.component.BindingDoubleSliderButton;
import journeymap.client.ui.component.BindingIntSliderButton;
import journeymap.client.ui.component.BindingListButton;
import journeymap.client.ui.component.BindingStringInputButton;
import journeymap.client.ui.component.Button;
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
            SlotMetadata slotMetadata = createSlot(entry);
            if (slotMetadata != null)
            {
                slotMetadataById.put(entry.getId(), slotMetadata);
            }
        }
    }

    protected static SlotMetadata<?> createSlot(ExternalSettingEntry entry)
    {
        switch (entry.getKind())
        {
            case LABEL:
                return createLabelSlot(entry);
            case BOOLEAN:
                return createBooleanSlot(entry);
            case INT_SLIDER:
                return createIntSliderSlot(entry);
            case DOUBLE_SLIDER:
                return createDoubleSliderSlot(entry);
            case LIST:
                return createListSlot(entry);
            case STRING_INPUT:
                return createStringInputSlot(entry);
            default:
                return null;
        }
    }

    protected static SlotMetadata<?> createLabelSlot(ExternalSettingEntry entry)
    {
        Button button = new Button(resolveRequired(entry.getTitleKey()));
        button.setEnabled(false);
        button.setDefaultStyle(false);
        button.setDrawBackground(false);
        button.setDrawFrame(false);
        button.setLabelColors(RGB.LIGHT_GRAY_RGB, RGB.LIGHT_GRAY_RGB, RGB.LIGHT_GRAY_RGB);
        return new SlotMetadata<Object>(button, button.displayString, resolveOptional(entry.getTooltipKey()), "", "", entry.isAdvanced());
    }

    protected static SlotMetadata<Boolean> createBooleanSlot(ExternalSettingEntry entry)
    {
        String name = resolveRequired(entry.getTitleKey());
        BindingBooleanButton button = new BindingBooleanButton(name, entry.getBooleanBinding());
        return new SlotMetadata<Boolean>(button, name, resolveOptional(entry.getTooltipKey()),
                Constants.getString("jm.config.default", entry.getBooleanBinding().getDefaultValue()),
                entry.getBooleanBinding().getDefaultValue(), entry.isAdvanced());
    }

    protected static SlotMetadata<Integer> createIntSliderSlot(ExternalSettingEntry entry)
    {
        String name = resolveRequired(entry.getTitleKey());
        BindingIntSliderButton button = new BindingIntSliderButton(name + " : ", entry.getIntBinding());
        button.setDefaultStyle(false);
        button.setDrawBackground(false);
        return new SlotMetadata<Integer>(button, name, resolveOptional(entry.getTooltipKey()),
                Constants.getString("jm.config.default_numeric", entry.getIntBinding().getMinValue(), entry.getIntBinding().getMaxValue(),
                        entry.getIntBinding().getDefaultValue()),
                entry.getIntBinding().getDefaultValue(), entry.isAdvanced());
    }

    protected static SlotMetadata<Double> createDoubleSliderSlot(ExternalSettingEntry entry)
    {
        String name = resolveRequired(entry.getTitleKey());
        BindingDoubleSliderButton button = new BindingDoubleSliderButton(name + " : ", entry.getDoubleBinding());
        button.setDefaultStyle(false);
        button.setDrawBackground(false);
        return new SlotMetadata<Double>(button, name, resolveOptional(entry.getTooltipKey()),
                Constants.getString("jm.config.default_numeric", entry.getDoubleBinding().getMinValue(),
                        entry.getDoubleBinding().getMaxValue(), entry.getDoubleBinding().getDefaultValue()),
                entry.getDoubleBinding().getDefaultValue(), entry.isAdvanced());
    }

    @SuppressWarnings("unchecked")
    protected static SlotMetadata<?> createListSlot(ExternalSettingEntry entry)
    {
        String name = resolveRequired(entry.getTitleKey());
        ListBinding<Object> binding = (ListBinding<Object>) entry.getListBinding();
        BindingListButton<Object> button = new BindingListButton<Object>(name, binding);
        button.setDefaultStyle(false);
        button.setDrawBackground(false);
        SlotMetadata<Object> slotMetadata = new SlotMetadata<Object>(button, name, resolveOptional(entry.getTooltipKey()),
                Constants.getString("jm.config.default", binding.getLabel(binding.getDefaultValue())), binding.getDefaultValue(),
                entry.isAdvanced());
        slotMetadata.setValueList(binding.getOptions());
        return slotMetadata;
    }

    protected static SlotMetadata<String> createStringInputSlot(ExternalSettingEntry entry)
    {
        String name = resolveRequired(entry.getTitleKey());
        BindingStringInputButton button = new BindingStringInputButton(name, entry.getStringBinding());
        return new SlotMetadata<String>(button, name, resolveOptional(entry.getTooltipKey()),
                Constants.getString("jm.config.default", entry.getStringBinding().getDefaultValue()),
                entry.getStringBinding().getDefaultValue(), entry.isAdvanced());
    }

    protected static String resolveOptional(String key)
    {
        if (key == null)
        {
            return null;
        }
        String value = I18n.format(key);
        return value.equals(key) ? null : value;
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
