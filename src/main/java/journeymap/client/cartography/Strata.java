/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.cartography;

import journeymap.client.JourneymapClient;
import journeymap.client.log.JMLogger;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.common.Journeymap;

import java.util.Stack;

/**
 * Stack of Stratum with simplistic object pooling to avoid heap thrash, since Stratum are used quite a bit.
 */
public class Strata
{
    final String name;
    final int initialPoolSize;
    final int poolGrowthIncrement;
    private final boolean underground;
    private boolean mapCaveLighting = JourneymapClient.getCoreProperties().mapCaveLighting.get();
    private Integer topY = null;
    private Integer bottomY = null;
    private Integer topWaterY = null;
    private Integer bottomWaterY = null;
    private Integer maxLightLevel = null;
    private Integer waterColor = null;
    private Integer renderDayColor = null;
    private Integer renderNightColor = null;
    private Integer renderCaveColor = null;
    private int lightAttenuation = 0;
    private boolean blocksFound = false;
    private Stack<Stratum> unusedStack = new Stack<Stratum>();
    private Stack<Stratum> stack = new Stack<Stratum>();

    public Strata(String name, int initialPoolSize, int poolGrowthIncrement, boolean underground)
    {
        this.name = name;
        this.underground = underground;
        this.initialPoolSize = initialPoolSize;
        this.poolGrowthIncrement = poolGrowthIncrement;
        growFreePool(initialPoolSize);
    }

    private Stratum allocate()
    {
        if (unusedStack.isEmpty())
        {
            int amount = stack.isEmpty() ? initialPoolSize : poolGrowthIncrement;
            growFreePool(amount);
            Journeymap.getLogger().debug(String.format("Grew Strata pool for '%s' by '%s'. Free: %s, Used: %s", name, amount, unusedStack.size(), stack.size()));
        }

        stack.push(unusedStack.pop());
        return stack.peek();
    }

    private void growFreePool(int amount)
    {
        for (int i = 0; i < amount; i++)
        {
            unusedStack.push(new Stratum());
        }
    }

    public void reset()
    {
        setTopY(null);
        setBottomY(null);
        setTopWaterY(null);
        setBottomWaterY(null);
        setMaxLightLevel(null);
        setWaterColor(null);
        setRenderDayColor(null);
        setRenderNightColor(null);
        setRenderCaveColor(null);
        setLightAttenuation(0);
        setBlocksFound(false);

        mapCaveLighting = JourneymapClient.getCoreProperties().mapCaveLighting.get();

        while (!stack.isEmpty())
        {
            release(stack.peek());
        }
    }

    public void release(Stratum stratum)
    {
        if (stratum == null)
        {
            Journeymap.getLogger().warn("Null stratum in pool.");
            return;
        }
        else
        {
            stratum.clear();
            unusedStack.add(0, stack.pop());
        }
    }

    public Stratum push(ChunkMD chunkMd, BlockMD blockMD, int x, int y, int z)
    {
        return push(chunkMd, blockMD, x, y, z, null);
    }

    public Stratum push(ChunkMD chunkMd, BlockMD blockMD, int x, int y, int z, Integer lightLevel)
    {
        try
        {
            // Alocate to stack, and set vars on stratum
            Stratum stratum = allocate();
            stratum.set(chunkMd, blockMD, x, y, z, lightLevel);

            // Update Strata's basic data
            setTopY((getTopY() == null) ? y : Math.max(getTopY(), y));
            setBottomY((getBottomY() == null) ? y : Math.min(getBottomY(), y));
            setMaxLightLevel((getMaxLightLevel() == null) ? stratum.getLightLevel() : Math.max(getMaxLightLevel(), stratum.getLightLevel()));
            setLightAttenuation(getLightAttenuation() + stratum.getLightOpacity());
            setBlocksFound(true);

            // Update Strata's water data
            if (blockMD.isWater())
            {
                setTopWaterY((getTopWaterY() == null) ? y : Math.max(getTopWaterY(), y));
                setBottomWaterY((getBottomWaterY() == null) ? y : Math.min(getBottomWaterY(), y));
                if (getWaterColor() == null)
                {
                    setWaterColor(blockMD.getColor(chunkMd, chunkMd.toWorldX(x), y, chunkMd.toWorldZ(z)));
                }
            }

            return stratum;
        }
        catch (ChunkMD.ChunkMissingException e)
        {
            throw e;
        }
        catch (Throwable t)
        {
            JMLogger.logOnce("Couldn't push Stratum into stack: " + t.getMessage(), t);
            return null;
        }
    }

    public Stratum nextUp(IChunkRenderer renderer, boolean ignoreMiddleWater)
    {
        Stratum stratum = null;
        try
        {
            stratum = stack.peek();
            if (stratum.isUninitialized())
            {
                throw new IllegalStateException("Stratum wasn't initialized for Strata.nextUp()");
            }

            setLightAttenuation(Math.max(0, getLightAttenuation() - stratum.getLightOpacity()));

            // Skip middle water blocks
            if (ignoreMiddleWater && stratum.isWater() && isWaterAbove(stratum) && !stack.isEmpty())
            {
                release(stratum);
                return nextUp(renderer, true);
            }

            renderer.setStratumColors(stratum, getLightAttenuation(), getWaterColor(), isWaterAbove(stratum), isUnderground(), isMapCaveLighting());
            return stratum;
        }
        catch (RuntimeException t)
        {
            throw t;
        }
    }

    int depth()
    {
        return stack.isEmpty() ? 0 : getTopY() - getBottomY() + 1;
    }

    public boolean isEmpty()
    {
        return stack.isEmpty();
    }

    boolean hasWater()
    {
        return getTopWaterY() != null;
    }

    boolean isWaterAbove(Stratum stratum)
    {
        return getTopWaterY() != null && getTopWaterY() > stratum.getY();
    }

    @Override
    public String toString()
    {
        return "Strata{" +
                "name='" + name + '\'' +
                ", initialPoolSize=" + initialPoolSize +
                ", poolGrowthIncrement=" + poolGrowthIncrement +
                ", stack=" + stack.size() +
                ", unusedStack=" + unusedStack.size() +
                ", stack=" + stack.size() +
                ", topY=" + getTopY() +
                ", bottomY=" + getBottomY() +
                ", topWaterY=" + getTopWaterY() +
                ", bottomWaterY=" + getBottomWaterY() +
                ", maxLightLevel=" + getMaxLightLevel() +
                ", waterColor=" + RGB.toString(getWaterColor()) +
                ", renderDayColor=" + RGB.toString(getRenderDayColor()) +
                ", renderNightColor=" + RGB.toString(getRenderNightColor()) +
                ", lightAttenuation=" + getLightAttenuation() +
                '}';
    }

    public boolean isMapCaveLighting()
    {
        return mapCaveLighting;
    }

    public boolean isUnderground()
    {
        return underground;
    }

    public Integer getTopY()
    {
        return topY;
    }

    public void setTopY(Integer topY)
    {
        this.topY = topY;
    }

    public Integer getBottomY()
    {
        return bottomY;
    }

    public void setBottomY(Integer bottomY)
    {
        this.bottomY = bottomY;
    }

    public Integer getTopWaterY()
    {
        return topWaterY;
    }

    public void setTopWaterY(Integer topWaterY)
    {
        this.topWaterY = topWaterY;
    }

    public Integer getBottomWaterY()
    {
        return bottomWaterY;
    }

    public void setBottomWaterY(Integer bottomWaterY)
    {
        this.bottomWaterY = bottomWaterY;
    }

    public Integer getMaxLightLevel()
    {
        return maxLightLevel;
    }

    public void setMaxLightLevel(Integer maxLightLevel)
    {
        this.maxLightLevel = maxLightLevel;
    }

    public Integer getWaterColor()
    {
        return waterColor;
    }

    public void setWaterColor(Integer waterColor)
    {
        this.waterColor = waterColor;
    }

    public Integer getRenderDayColor()
    {
        return renderDayColor;
    }

    public void setRenderDayColor(Integer renderDayColor)
    {
        this.renderDayColor = renderDayColor;
    }

    public Integer getRenderNightColor()
    {
        return renderNightColor;
    }

    public void setRenderNightColor(Integer renderNightColor)
    {
        this.renderNightColor = renderNightColor;
    }

    public Integer getRenderCaveColor()
    {
        return renderCaveColor;
    }

    public void setRenderCaveColor(Integer renderCaveColor)
    {
        this.renderCaveColor = renderCaveColor;
    }

    public int getLightAttenuation()
    {
        return lightAttenuation;
    }

    public void setLightAttenuation(int lightAttenuation)
    {
        this.lightAttenuation = lightAttenuation;
    }

    public boolean isBlocksFound()
    {
        return blocksFound;
    }

    public void setBlocksFound(boolean blocksFound)
    {
        this.blocksFound = blocksFound;
    }
}
