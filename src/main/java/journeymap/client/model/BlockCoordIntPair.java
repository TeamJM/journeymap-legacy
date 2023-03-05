/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

public class BlockCoordIntPair
{

    public int x;
    public int z;

    public BlockCoordIntPair()
    {
        setLocation(0, 0);
    }

    public BlockCoordIntPair(int x, int z)
    {
        setLocation(x, z);
    }

    public void setLocation(int x, int z)
    {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        BlockCoordIntPair that = (BlockCoordIntPair) o;

        if (x != that.x)
        {
            return false;
        }
        if (z != that.z)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = x;
        result = 31 * result + z;
        return result;
    }
}
