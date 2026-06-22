package journeymap.client.api.fullscreen.context;

import journeymap.client.model.Waypoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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

    /**
     * Creates a context snapshot for the clicked fullscreen map location.
     *
     * @param x block x coordinate
     * @param resolvedY y coordinate used for actions; falls back to the player y when terrain height is unknown
     * @param displayY known terrain height for labels, or null when the target is unexplored
     * @param z block z coordinate
     */
    public FullscreenContextMenuContext(int x, int resolvedY, Integer displayY, int z, int chunkX, int chunkZ,
                                        int dimension, Collection<Integer> dimensions, Waypoint waypoint)
    {
        this.x = x;
        this.resolvedY = resolvedY;
        this.displayY = displayY;
        this.z = z;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.dimension = dimension;
        this.dimensions = dimensions == null ? Collections.<Integer>emptyList()
                : Collections.unmodifiableList(new ArrayList<Integer>(dimensions));
        this.waypoint = waypoint;
    }

    public int getX()
    {
        return x;
    }

    /**
     * Returns the y coordinate used for actions such as teleporting.
     */
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

    /**
     * Returns the block z coordinate.
     */
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
