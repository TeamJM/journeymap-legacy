package journeymap.client.api.settings;

import journeymap.common.Journeymap;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Describes one externally provided setting row in the JourneyMap options GUI.
 */
public class ExternalSettingEntry
{
    private static final Logger LOGGER = Journeymap.getLogger();

    public enum Kind
    {
        LABEL,
        BOOLEAN,
        INT_SLIDER,
        DOUBLE_SLIDER,
        LIST,
        STRING_INPUT
    }

    public static interface BooleanBinding
    {
        boolean get();

        void set(boolean value);

        boolean getDefaultValue();
    }

    public static interface IntBinding
    {
        int get();

        void set(int value);

        int getDefaultValue();

        int getMinValue();

        int getMaxValue();

        int getStep();

        String getSuffix();
    }

    public static interface DoubleBinding
    {
        double get();

        void set(double value);

        double getDefaultValue();

        double getMinValue();

        double getMaxValue();

        double getStep();

        int getPrecision();

        String getSuffix();
    }

    public static interface ListBinding<T>
    {
        T get();

        void set(T value);

        T getDefaultValue();

        List<T> getOptions();

        String getLabel(T value);
    }

    public static interface StringBinding
    {
        String get();

        void set(String value);

        String getDefaultValue();

        int getMaxLength();
    }

    private String id;
    private Kind kind;
    private String titleKey;
    private String tooltipKey;
    private int order;
    private boolean advanced;
    private Callable<Boolean> visibleSupplier;
    private Callable<Boolean> enabledSupplier;
    private BooleanBinding booleanBinding;
    private IntBinding intBinding;
    private DoubleBinding doubleBinding;
    private ListBinding<?> listBinding;
    private StringBinding stringInputBinding;

    protected ExternalSettingEntry(String id, Kind kind, String titleKey, String tooltipKey, int order)
    {
        this.id = id;
        this.kind = kind;
        this.titleKey = titleKey;
        this.tooltipKey = tooltipKey;
        this.order = order;
    }

    public static ExternalSettingEntry label(String id, String titleKey, String tooltipKey, int order)
    {
        return new ExternalSettingEntry(id, Kind.LABEL, titleKey, tooltipKey, order);
    }

    public static ExternalSettingEntry bool(String id, String titleKey, String tooltipKey, int order, BooleanBinding binding)
    {
        requireBinding(id, binding);
        ExternalSettingEntry entry = new ExternalSettingEntry(id, Kind.BOOLEAN, titleKey, tooltipKey, order);
        entry.booleanBinding = binding;
        return entry;
    }

    public static ExternalSettingEntry intSlider(String id, String titleKey, String tooltipKey, int order, IntBinding binding)
    {
        requireBinding(id, binding);
        ExternalSettingEntry entry = new ExternalSettingEntry(id, Kind.INT_SLIDER, titleKey, tooltipKey, order);
        entry.intBinding = binding;
        return entry;
    }

    public static ExternalSettingEntry doubleSlider(String id, String titleKey, String tooltipKey, int order, DoubleBinding binding)
    {
        requireBinding(id, binding);
        ExternalSettingEntry entry = new ExternalSettingEntry(id, Kind.DOUBLE_SLIDER, titleKey, tooltipKey, order);
        entry.doubleBinding = binding;
        return entry;
    }

    public static ExternalSettingEntry list(String id, String titleKey, String tooltipKey, int order, ListBinding<?> binding)
    {
        requireBinding(id, binding);
        ExternalSettingEntry entry = new ExternalSettingEntry(id, Kind.LIST, titleKey, tooltipKey, order);
        entry.listBinding = binding;
        return entry;
    }

    public static ExternalSettingEntry enumList(String id, String titleKey, String tooltipKey, int order, ListBinding<?> binding)
    {
        return list(id, titleKey, tooltipKey, order, binding);
    }

    public static ExternalSettingEntry stringList(String id, String titleKey, String tooltipKey, int order, ListBinding<?> binding)
    {
        return list(id, titleKey, tooltipKey, order, binding);
    }

    public static ExternalSettingEntry stringInput(String id, String titleKey, String tooltipKey, int order, StringBinding binding)
    {
        requireBinding(id, binding);
        ExternalSettingEntry entry = new ExternalSettingEntry(id, Kind.STRING_INPUT, titleKey, tooltipKey, order);
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

    public Kind getKind()
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

    public BooleanBinding getBooleanBinding()
    {
        return booleanBinding;
    }

    public IntBinding getIntBinding()
    {
        return intBinding;
    }

    public DoubleBinding getDoubleBinding()
    {
        return doubleBinding;
    }

    public ListBinding<?> getListBinding()
    {
        return listBinding;
    }

    public StringBinding getStringBinding()
    {
        return stringInputBinding;
    }

    public void validate()
    {
        switch (kind)
        {
            case BOOLEAN:
                requireBinding(id, booleanBinding);
                return;
            case INT_SLIDER:
                requireBinding(id, intBinding);
                return;
            case DOUBLE_SLIDER:
                requireBinding(id, doubleBinding);
                return;
            case LIST:
                requireBinding(id, listBinding);
                return;
            case STRING_INPUT:
                requireBinding(id, stringInputBinding);
                return;
            default:
        }
    }

    protected static void requireBinding(String id, Object binding)
    {
        if (binding == null)
        {
            throw new IllegalArgumentException("External setting entry '" + id + "' requires a binding");
        }
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
        catch (Exception exception)
        {
            LOGGER.warn("External setting entry '{}' supplier failed", id, exception);
            return fallback;
        }
    }
}
