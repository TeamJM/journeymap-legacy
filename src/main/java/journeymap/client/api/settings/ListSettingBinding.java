package journeymap.client.api.settings;

import java.util.List;

public interface ListSettingBinding<T>
{
    T get();

    void set(T value);

    T getDefaultValue();

    List<T> getOptions();

    String getLabel(T value);
}
