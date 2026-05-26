package journeymap.client.mixin.api;

public interface IGuiSelectWorldDeleteFlag
{
    boolean journeymap$shouldDeleteJmData();

    void journeymap$setShouldDeleteJmData(boolean value);
}
