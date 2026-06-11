package journeymap.client.api.fullscreen.context;

import journeymap.client.model.Waypoint;

import java.util.Collection;

public class FullscreenContextMenuContext
{
    private final FullscreenContextTargetType targetType;
    private final int x;
    private final int resolvedY;
    private final Integer displayY;
    private final int z;
    private final int chunkX;
    private final int chunkZ;
    private final int dimension;
    private final Collection<Integer> dimensions;
    private final Waypoint waypoint;

    public FullscreenContextMenuContext(FullscreenContextTargetType targetType, int x, int resolvedY, Integer displayY, int z, int chunkX, int chunkZ, int dimension, Collection<Integer> dimensions, Waypoint waypoint)
    {
        this.targetType = targetType;
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

    public FullscreenContextTargetType getTargetType()
    {
        return targetType;
    }

    public int getX()
    {
        return x;
    }

    public int getResolvedY()
    {
        return resolvedY;
    }

    public Integer getDisplayY()
    {
        return displayY;
    }

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
