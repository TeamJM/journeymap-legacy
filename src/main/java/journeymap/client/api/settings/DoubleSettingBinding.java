package journeymap.client.api.settings;

public interface DoubleSettingBinding
{
    double get();

    void set(double value);

    double getDefaultValue();

    double getMinValue();

    double getMaxValue();

    double getStep();

    int getPrecision();

    String getSuffix();

    default void commit()
    {
    }
}
