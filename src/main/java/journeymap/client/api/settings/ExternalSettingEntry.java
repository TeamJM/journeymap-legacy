package journeymap.client.api.settings;

import java.util.concurrent.Callable;

/**
 * Describes one externally provided setting row in the JourneyMap options GUI.
 */
public class ExternalSettingEntry
{
    private String id;
    private ExternalSettingKind kind;
    private String titleKey;
    private String tooltipKey;
    private int order;
    private boolean advanced;
    private Callable<Boolean> visibleSupplier;
    private Callable<Boolean> enabledSupplier;
    private BooleanSettingBinding booleanBinding;
    private IntSettingBinding intBinding;
    private DoubleSettingBinding doubleBinding;
    private ListSettingBinding<?> listBinding;
    private StringInputBinding stringInputBinding;

    protected ExternalSettingEntry(String id, ExternalSettingKind kind, String titleKey, String tooltipKey, int order)
    {
        this.id = id;
        this.kind = kind;
        this.titleKey = titleKey;
        this.tooltipKey = tooltipKey;
        this.order = order;
    }

    public static ExternalSettingEntry label(String id, String titleKey, String tooltipKey, int order)
    {
        return new ExternalSettingEntry(id, ExternalSettingKind.LABEL, titleKey, tooltipKey, order);
    }

    public static ExternalSettingEntry bool(String id, String titleKey, String tooltipKey, int order, BooleanSettingBinding binding)
    {
        ExternalSettingEntry entry = new ExternalSettingEntry(id, ExternalSettingKind.BOOLEAN, titleKey, tooltipKey, order);
        entry.booleanBinding = binding;
        return entry;
    }

    public static ExternalSettingEntry intSlider(String id, String titleKey, String tooltipKey, int order, IntSettingBinding binding)
    {
        ExternalSettingEntry entry = new ExternalSettingEntry(id, ExternalSettingKind.INT_SLIDER, titleKey, tooltipKey, order);
        entry.intBinding = binding;
        return entry;
    }

    public static ExternalSettingEntry doubleSlider(String id, String titleKey, String tooltipKey, int order, DoubleSettingBinding binding)
    {
        ExternalSettingEntry entry = new ExternalSettingEntry(id, ExternalSettingKind.DOUBLE_SLIDER, titleKey, tooltipKey, order);
        entry.doubleBinding = binding;
        return entry;
    }

    public static ExternalSettingEntry enumList(String id, String titleKey, String tooltipKey, int order, ListSettingBinding<?> binding)
    {
        ExternalSettingEntry entry = new ExternalSettingEntry(id, ExternalSettingKind.ENUM_LIST, titleKey, tooltipKey, order);
        entry.listBinding = binding;
        return entry;
    }

    public static ExternalSettingEntry stringList(String id, String titleKey, String tooltipKey, int order, ListSettingBinding<?> binding)
    {
        ExternalSettingEntry entry = new ExternalSettingEntry(id, ExternalSettingKind.STRING_LIST, titleKey, tooltipKey, order);
        entry.listBinding = binding;
        return entry;
    }

    public static ExternalSettingEntry stringInput(String id, String titleKey, String tooltipKey, int order, StringInputBinding binding)
    {
        ExternalSettingEntry entry = new ExternalSettingEntry(id, ExternalSettingKind.STRING_INPUT, titleKey, tooltipKey, order);
        entry.stringInputBinding = binding;
        return entry;
    }

    public ExternalSettingEntry setAdvanced(boolean advanced)
    {
        this.advanced = advanced;
        return this;
    }

    public ExternalSettingEntry setVisibleSupplier(Callable<Boolean> visibleSupplier)
    {
        this.visibleSupplier = visibleSupplier;
        return this;
    }

    public ExternalSettingEntry setEnabledSupplier(Callable<Boolean> enabledSupplier)
    {
        this.enabledSupplier = enabledSupplier;
        return this;
    }

    public String getId()
    {
        return id;
    }

    public ExternalSettingKind getKind()
    {
        return kind;
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

    public boolean isAdvanced()
    {
        return advanced;
    }

    public boolean isVisible()
    {
        return resolveCallable(visibleSupplier, true);
    }

    public boolean isEnabled()
    {
        return resolveCallable(enabledSupplier, true);
    }

    public BooleanSettingBinding getBooleanBinding()
    {
        return booleanBinding;
    }

    public IntSettingBinding getIntBinding()
    {
        return intBinding;
    }

    public DoubleSettingBinding getDoubleBinding()
    {
        return doubleBinding;
    }

    public ListSettingBinding<?> getListBinding()
    {
        return listBinding;
    }

    public StringInputBinding getStringInputBinding()
    {
        return stringInputBinding;
    }

    protected boolean resolveCallable(Callable<Boolean> supplier, boolean fallback)
    {
        if (supplier == null)
        {
            return fallback;
        }
        try
        {
            Boolean value = supplier.call();
            return value == null ? fallback : value;
        }
        catch (Exception ignored)
        {
            // A broken integration should not prevent JourneyMap from opening its options GUI.
            return fallback;
        }
    }
}
