/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

import journeymap.client.io.RegionImageHandler;
import journeymap.client.render.map.Tile;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * A RegionImageSet contains one or more ImageHolders for Region images
 *
 * @author techbrew
 */
public class RegionImageSet extends ImageSet
{
    protected final Key key;

    public RegionImageSet(Key key)
    {
        super();
        this.key = key;
    }

    @Override
    public ImageHolder getHolder(MapType mapType)
    {
        synchronized (imageHolders)
        {
            ImageHolder imageHolder = imageHolders.get(mapType);
            if (imageHolder == null)
            {
                // Prepare to find image in file
                File imageFile = RegionImageHandler.getRegionImageFile(getRegionCoord(), mapType, false);

                // Add holder
                imageHolder = addHolder(mapType, imageFile);
            }
            return imageHolder;
        }
    }

    public BufferedImage getChunkImage(ChunkMD chunkMd, MapType mapType)
    {
        BufferedImage regionImage = getHolder(mapType).getImage();
        RegionCoord regionCoord = getRegionCoord();
        BufferedImage current = regionImage.getSubimage(
                regionCoord.getXOffset(chunkMd.getCoord().chunkXPos),
                regionCoord.getZOffset(chunkMd.getCoord().chunkZPos),
                16, 16);

//        BufferedImage copy = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
//        Graphics g2D = RegionImageHandler.initRenderingHints(copy.createGraphics());
//        g2D.drawImage(current, 0, 0, null);
//        g2D.dispose();
//        return copy;
        return current;
    }

    public void setChunkImage(ChunkMD chunkMd, MapType mapType, BufferedImage chunkImage)
    {
        ImageHolder holder = getHolder(mapType);
        RegionCoord regionCoord = getRegionCoord();
        holder.partialImageUpdate(chunkImage, regionCoord.getXOffset(chunkMd.getCoord().chunkXPos), regionCoord.getZOffset(chunkMd.getCoord().chunkZPos));
    }

    public boolean hasChunkUpdates()
    {
        for (ImageHolder holder : this.imageHolders.values())
        {
            if (holder.partialUpdate)
            {
                return true;
            }
        }
        return false;
    }

    public void finishChunkUpdates()
    {
        for (ImageHolder holder : this.imageHolders.values())
        {
            holder.finishPartialImageUpdates();
        }
    }

    public RegionCoord getRegionCoord()
    {
        return RegionCoord.fromRegionPos(key.worldDir, key.regionX, key.regionZ, key.dimension);
    }

    public long getOldestTimestamp()
    {
        long time = System.currentTimeMillis();
        synchronized (imageHolders)
        {
            for (ImageHolder holder : imageHolders.values())
            {
                if (holder != null)
                {
                    time = Math.min(time, holder.getImageTimestamp());
                }
            }
        }
        return time;
    }

    @Override
    public int hashCode()
    {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        return key.equals(((RegionImageSet) obj).key);
    }

    @Override
    protected int getImageSize()
    {
        return Tile.TILESIZE;
    }

    public static class Key
    {
        private final File worldDir;
        private final int regionX;
        private final int regionZ;
        private final int dimension;

        private Key(File worldDir, int regionX, int regionZ, int dimension)
        {
            this.worldDir = worldDir;
            this.regionX = regionX;
            this.regionZ = regionZ;
            this.dimension = dimension;
        }

        public static Key from(RegionCoord rCoord)
        {
            return new Key(rCoord.worldDir, rCoord.regionX, rCoord.regionZ, rCoord.dimension);
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

            Key key = (Key) o;

            if (dimension != key.dimension)
            {
                return false;
            }
            if (regionX != key.regionX)
            {
                return false;
            }
            if (regionZ != key.regionZ)
            {
                return false;
            }
            if (!worldDir.equals(key.worldDir))
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = worldDir.hashCode();
            result = 31 * result + regionX;
            result = 31 * result + regionZ;
            result = 31 * result + dimension;
            return result;
        }
    }
}
