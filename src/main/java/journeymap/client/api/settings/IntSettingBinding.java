package journeymap.client.api.settings;

public interface IntSettingBinding
{
    int get();

    void set(int value);

    int getDefaultValue();

    int getMinValue();

    int getMaxValue();

    int getStep();

    String getSuffix();

    default void commit()
    {
    }
}
