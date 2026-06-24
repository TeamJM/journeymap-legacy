package journeymap.client.api.fullscreen.context;

import journeymap.client.model.Waypoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Immutable snapshot of the fullscreen map target used to build and handle a context menu.
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
     * Keeps construction behind the builder so callers do not have to order several adjacent coordinates correctly.
     */
    private FullscreenContextMenuContext(Builder builder)
    {
        this.x = builder.x;
        this.resolvedY = builder.resolvedY;
        this.displayY = builder.displayY;
        this.z = builder.z;
        this.chunkX = builder.chunkX;
        this.chunkZ = builder.chunkZ;
        this.dimension = builder.dimension;
        // Providers receive an immutable snapshot even if the caller reuses its source collection later.
        this.dimensions = builder.dimensions == null ? Collections.<Integer>emptyList()
                : Collections.unmodifiableList(new ArrayList<Integer>(builder.dimensions));
        this.waypoint = builder.waypoint;
    }

    /**
     * Creates a builder for a block location on the fullscreen map.
     */
    public static Builder builder(int x, int z)
    {
        return new Builder(x, z);
    }

    /**
     * Returns the block x coordinate under the cursor.
     */
    public int getX()
    {
        return x;
    }

    /**
     * Returns the y coordinate consumers should use for actions.
     * This falls back to a caller-provided value, such as the player y, when displayY is unknown.
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
     * Returns the block z coordinate under the cursor.
     */
    public int getZ()
    {
        return z;
    }

    /**
     * Returns the chunk x coordinate under the cursor.
     */
    public int getChunkX()
    {
        return chunkX;
    }

    /**
     * Returns the chunk z coordinate under the cursor.
     */
    public int getChunkZ()
    {
        return chunkZ;
    }

    /**
     * Returns the current player dimension when the menu was opened.
     */
    public int getDimension()
    {
        return dimension;
    }

    /**
     * Returns the loaded waypoint dimensions available when the menu was opened.
     */
    public Collection<Integer> getDimensions()
    {
        return dimensions;
    }

    /**
     * Returns the waypoint under the cursor, or null when the target is a map location.
     */
    public Waypoint getWaypoint()
    {
        return waypoint;
    }

    public boolean hasWaypoint()
    {
        return waypoint != null;
    }

    /**
     * Builder for the target snapshot passed to registered context menu providers.
     */
    public static class Builder
    {
        private final int x;
        private final int z;
        private int resolvedY;
        private Integer displayY;
        private int chunkX;
        private int chunkZ;
        private int dimension;
        private Collection<Integer> dimensions;
        private Waypoint waypoint;

        private Builder(int x, int z)
        {
            this.x = x;
            this.z = z;
            this.chunkX = x >> 4;
            this.chunkZ = z >> 4;
        }

        public Builder resolvedY(int resolvedY)
        {
            this.resolvedY = resolvedY;
            return this;
        }

        public Builder displayY(Integer displayY)
        {
            this.displayY = displayY;
            return this;
        }

        public Builder chunk(int chunkX, int chunkZ)
        {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            return this;
        }

        public Builder dimension(int dimension)
        {
            this.dimension = dimension;
            return this;
        }

        public Builder dimensions(Collection<Integer> dimensions)
        {
            this.dimensions = dimensions;
            return this;
        }

        public Builder waypoint(Waypoint waypoint)
        {
            this.waypoint = waypoint;
            return this;
        }

        public FullscreenContextMenuContext build()
        {
            return new FullscreenContextMenuContext(this);
        }
    }
}
