/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

import com.google.common.base.Objects;
import journeymap.common.Journeymap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An ImageSet contains one or more ImageHolders
 *
 * @author techbrew
 */
public abstract class ImageSet
{
    protected final Map<MapType, ImageHolder> imageHolders;

    public ImageSet()
    {
        imageHolders = Collections.synchronizedMap(new HashMap<MapType, ImageHolder>(8));
    }

    protected abstract ImageHolder getHolder(MapType mapType);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);


    public BufferedImage getImage(MapType mapType)
    {
        return getHolder(mapType).getImage();
    }

    /**
     * Returns the number of imageHolders actually written to disk.
     * @param force write even if image isn't flagged as dirty
     * @return number of images that should be updated if async=true, or
     *         number of images actually updated if async=false
     */
    public int writeToDisk(boolean force, boolean async)
    {
        int count = 0;
        try
        {
            synchronized (imageHolders)
            {
                for (ImageHolder imageHolder : imageHolders.values())
                {
                    if (force || imageHolder.isDirty())
                    {
                        imageHolder.writeToDisk(async);
                        count++;
                    }
                }
            }

        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Error writing ImageSet to disk: " + t);
        }
        return count;
    }

    public boolean updatedSince(MapType mapType, long time)
    {
        synchronized (imageHolders)
        {
            if (mapType == null)
            {
                for (ImageHolder holder : imageHolders.values())
                {
                    if (holder != null && holder.getImageTimestamp() >= time)
                    {
                        return true;
                    }
                }
            }
            else
            {
                ImageHolder imageHolder = imageHolders.get(mapType);
                if (imageHolder != null && imageHolder.getImageTimestamp() >= time)
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void clear()
    {
        synchronized (imageHolders)
        {
            for (ImageHolder imageHolder : imageHolders.values())
            {
                imageHolder.clear();
            }
            imageHolders.clear();
        }
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("imageHolders", imageHolders.entrySet())
                .toString();
    }

    /**
     * ************************
     */

    protected abstract int getImageSize();

    protected ImageHolder addHolder(MapType mapType, File imageFile)
    {
        return addHolder(new ImageHolder(mapType, imageFile, getImageSize()));
    }

    protected ImageHolder addHolder(ImageHolder imageHolder)
    {
        imageHolders.put(imageHolder.mapType, imageHolder);
        return imageHolder;
    }

}
