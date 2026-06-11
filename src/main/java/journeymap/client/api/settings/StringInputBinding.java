package journeymap.client.api.settings;

public interface StringInputBinding
{
    String get();

    void set(String value);

    String getDefaultValue();

    int getMaxLength();

    default void commit()
    {
    }
}
