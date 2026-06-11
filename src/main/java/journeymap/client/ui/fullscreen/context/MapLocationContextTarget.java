package journeymap.client.ui.fullscreen.context;

public class MapLocationContextTarget implements FullscreenContextTarget
{
    private final int x;
    private final int fallbackY;
    private final int z;
    private final Integer displayY;
    private final int dimension;

    public MapLocationContextTarget(int x, int fallbackY, int z, Integer displayY, int dimension)
    {
        this.x = x;
        this.fallbackY = fallbackY;
        this.z = z;
        this.displayY = displayY;
        this.dimension = dimension;
    }

    public int getChunkX()
    {
        return x >> 4;
    }

    public int getChunkZ()
    {
        return z >> 4;
    }

    @Override
    public int getX()
    {
        return x;
    }

    @Override
    public int getResolvedY()
    {
        return displayY == null ? fallbackY : displayY.intValue();
    }

    @Override
    public Integer getDisplayY()
    {
        return displayY;
    }

    @Override
    public int getZ()
    {
        return z;
    }

    public int getDimension()
    {
        return dimension;
    }
}
