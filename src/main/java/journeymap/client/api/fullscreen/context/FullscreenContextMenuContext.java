package journeymap.client.api.fullscreen.context;

import journeymap.client.model.Waypoint;

import java.util.Collection;

/**
 * Immutable snapshot of the map target used to build and handle a fullscreen context menu.
 */
public class FullscreenContextMenuContext
{
    private final int x;
    private final int resolvedY;
    private final Integer displayY;
    private final int z;
    private final int chunkX;
    private final int chunkZ;
    private final int dimension;
    private final Collection<Integer> dimensions;
    private final Waypoint waypoint;

    public FullscreenContextMenuContext(int x, int resolvedY, Integer displayY, int z, int chunkX, int chunkZ, int dimension, Collection<Integer> dimensions, Waypoint waypoint)
    {
        this.x = x;
        this.resolvedY = resolvedY;
        this.displayY = displayY;
        this.z = z;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.dimension = dimension;
        this.dimensions = dimensions;
        this.waypoint = waypoint;
    }
public int getX()
    {
        return x;
    }

    public int getResolvedY()
    {
        return resolvedY;
    }

    /**
     * Returns the known terrain height for labels, or null when the target is unexplored.
     */
    public Integer getDisplayY()
    {
        return displayY;
    }

    /**
     * Returns true when the map target has a known terrain height to show in labels.
     */
    public boolean hasDisplayY()
    {
        return displayY != null;
    }

    public int getZ()
    {
        return z;
    }

    public int getChunkX()
    {
        return chunkX;
    }

    public int getChunkZ()
    {
        return chunkZ;
    }

    public int getDimension()
    {
        return dimension;
    }

    public Collection<Integer> getDimensions()
    {
        return dimensions;
    }

    public Waypoint getWaypoint()
    {
        return waypoint;
    }

    public boolean hasWaypoint()
    {
        return waypoint != null;
    }
}
