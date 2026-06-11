package journeymap.client.api.settings;

public interface BooleanSettingBinding
{
    boolean get();

    void set(boolean value);

    boolean getDefaultValue();

    default void commit()
    {
    }
}
