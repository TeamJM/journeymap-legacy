/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.render.map;

import com.google.common.base.Objects;
import journeymap.client.cartography.RGB;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.forge.helper.IRenderHelper;
import journeymap.client.io.RegionImageHandler;
import journeymap.client.log.StatTimer;
import journeymap.client.model.*;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.texture.TextureCache;
import journeymap.client.render.texture.TextureImpl;
import journeymap.client.task.main.ExpireTextureTask;
import journeymap.common.Journeymap;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Draw a map tile via the grid renderer.
 */
public class TileDrawStep
{
    private static final Integer bgColor = 0x222222;
    private static final Logger logger = Journeymap.getLogger();
    private static final IRenderHelper renderHelper = ForgeHelper.INSTANCE.getRenderHelper();
    private static final RegionImageCache regionImageCache = RegionImageCache.instance();

    private final boolean debug = logger.isDebugEnabled();
    private final RegionCoord regionCoord;
    private final MapType mapType;
    private final Integer zoom;
    private final boolean highQuality;
    private final StatTimer drawTimer;
    private final StatTimer updateRegionTimer = StatTimer.get("TileDrawStep.updateRegionTexture", 5, 50);
    private final StatTimer updateScaledTimer = StatTimer.get("TileDrawStep.updateScaledTexture", 5, 50);
    private final int theHashCode;
    private final String theCacheKey;
    private final RegionImageSet.Key regionImageSetKey;
    private int sx1, sy1, sx2, sy2;
    private volatile TextureImpl scaledTexture;
    private volatile Future<TextureImpl> regionFuture;
    private volatile Future<TextureImpl> scaledFuture;
    private int lastTextureFilter;
    private int lastTextureWrap;


    public TileDrawStep(RegionCoord regionCoord, final MapType mapType, Integer zoom, boolean highQuality, int sx1, int sy1, int sx2, int sy2)
    {
        this.mapType = mapType;
        this.regionCoord = regionCoord;
        this.regionImageSetKey = RegionImageSet.Key.from(regionCoord);
        this.zoom = zoom;
        this.sx1 = sx1;
        this.sx2 = sx2;
        this.sy1 = sy1;
        this.sy2 = sy2;
        this.highQuality = highQuality && zoom != 0; // todo change when zoom can be < zero or 0 no longer 1:1
        this.drawTimer = (this.highQuality) ? StatTimer.get("TileDrawStep.draw(high)") : StatTimer.get("TileDrawStep.draw(low)");

        theCacheKey = toCacheKey(regionCoord, mapType, zoom, highQuality, sx1, sy1, sx2, sy2);
        theHashCode = theCacheKey.hashCode();
        updateRegionTexture();
        if (highQuality)
        {
            updateScaledTexture();
        }
    }

    public static String toCacheKey(RegionCoord regionCoord, final MapType mapType, Integer zoom, boolean highQuality, int sx1, int sy1, int sx2, int sy2)
    {
        return regionCoord.cacheKey() + mapType.toCacheKey() + zoom + highQuality + sx1 + "," + sy1 + "," + sx2 + "," + sy2;
    }

    ImageHolder getRegionTextureHolder()
    {
        return regionImageCache.getRegionImageSet(regionImageSetKey).getHolder(mapType);
    }

    boolean draw(final TilePos pos, final double offsetX, final double offsetZ, float alpha, int textureFilter, int textureWrap, GridSpec gridSpec)
    {
        boolean regionUpdatePending = updateRegionTexture();
        if (highQuality)
        {
            updateScaledTexture();
        }

        //boolean scaledUpdatePending = !regionUpdatePending && highQuality && updateScaledTexture();
        Integer textureId = -1;
        boolean useScaled = false;

        if (highQuality && scaledTexture != null)
        {
            textureId = scaledTexture.getGlTextureId();
            useScaled = true;
        }
        else if (!regionUpdatePending)
        {
            textureId = getRegionTextureHolder().getTexture().getGlTextureId();
        }
        else
        {
            textureId = -1;
        }

        if (textureFilter != lastTextureFilter)
        {
            lastTextureFilter = textureFilter;
        }

        if (textureWrap != lastTextureWrap)
        {
            lastTextureWrap = textureWrap;
        }

        // Draw already!
        drawTimer.start();

        final double startX = offsetX + pos.startX;
        final double startY = offsetZ + pos.startZ;
        final double endX = offsetX + pos.endX;
        final double endY = offsetZ + pos.endZ;
        final double z = 0D;

        final double size = Tile.TILESIZE;

        final double startU = useScaled ? 0D : sx1 / size;
        final double startV = useScaled ? 0D : sy1 / size;
        final double endU = useScaled ? 1D : sx2 / size;
        final double endV = useScaled ? 1D : sy2 / size;

        // Background
        DrawUtil.drawRectangle(startX, startY, endX - startX, endY - startY, bgColor, 200);
        renderHelper.glEnableBlend();
        renderHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        renderHelper.glEnableTexture2D();

        // Tile
        if (textureId != -1)
        {
            renderHelper.glBindTexture(textureId);
            renderHelper.glColor4f(1, 1, 1, alpha);

            // http://gregs-blog.com/2008/01/17/opengl-texture-filter-parameters-explained/
            renderHelper.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, textureFilter); // GL11.GL_LINEAR_MIPMAP_NEAREST
            renderHelper.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, textureFilter); // GL11.GL_NEAREST
            renderHelper.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, textureWrap);
            renderHelper.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, textureWrap);
            DrawUtil.drawBoundTexture(startU, startV, startX, startY, z, endU, endV, endX, endY);
        }

        // Grid
        if (gridSpec != null)
        {
            gridSpec.beginTexture(textureWrap, alpha);
            DrawUtil.drawBoundTexture(sx1 / size, sy1 / size, startX, startY, z, sx2 / size, sy2 / size, endX, endY);
            gridSpec.finishTexture();
        }

        if (debug)
        {
            int debugX = (int) startX;
            int debugY = (int) startY;
            DrawUtil.drawRectangle(debugX, debugY, 3, endV * 512, RGB.GREEN_RGB, 200);
            DrawUtil.drawRectangle(debugX, debugY, endU * 512, 3, RGB.RED_RGB, 200);
            DrawUtil.drawLabel(this.toString(), debugX + 5, debugY + 10, DrawUtil.HAlign.Right, DrawUtil.VAlign.Below, RGB.WHITE_RGB, 255, RGB.BLUE_RGB, 255, 1.0, false);
            DrawUtil.drawLabel(String.format("Tile Render Type: %s, Scaled: %s", Tile.debugGlSettings, useScaled), debugX + 5, debugY + 20, DrawUtil.HAlign.Right, DrawUtil.VAlign.Below, RGB.WHITE_RGB, 255, RGB.BLUE_RGB, 255, 1.0, false);
            long imageTimestamp = useScaled ? scaledTexture.getLastImageUpdate() : getRegionTextureHolder().getImageTimestamp();
            long age = (System.currentTimeMillis() - imageTimestamp) / 1000;
            DrawUtil.drawLabel(mapType + " tile age: " + age + " seconds old", debugX + 5, debugY + 30, DrawUtil.HAlign.Right, DrawUtil.VAlign.Below, RGB.WHITE_RGB, 255, RGB.BLUE_RGB, 255, 1.0, false);
        }

        renderHelper.glColor4f(1, 1, 1, 1);
        renderHelper.glClearColor(1, 1, 1, 1f); // defensive against shaders

        drawTimer.stop();

        int glErr = GL11.glGetError();
        if (glErr != GL11.GL_NO_ERROR)
        {
            Journeymap.getLogger().warn("GL Error in TileDrawStep: " + glErr);
            clearTexture();
        }

        return textureId != 1;
    }


    public void clearTexture()
    {
        ExpireTextureTask.queue(scaledTexture);
        scaledTexture = null;
        if (scaledFuture != null && !scaledFuture.isDone())
        {
            scaledFuture.cancel(true);
        }
        scaledFuture = null;
        if (regionFuture != null && !regionFuture.isDone())
        {
            regionFuture.cancel(true);
        }
        regionFuture = null;
    }

    public MapType getMapType()
    {
        return mapType;
    }

    public Integer getZoom()
    {
        return zoom;
    }

    public String cacheKey()
    {
        return theCacheKey;
    }

    @Override
    public int hashCode()
    {
        return theHashCode;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("rc", regionCoord)
                .add("type", mapType)
                .add("high", highQuality)
                .add("zoom", zoom)
                .add("sx1", sx1)
                .add("sy1", sy1)
                .toString();
    }

    boolean hasTexture(MapType mapType)
    {
        if (!Objects.equal(this.mapType, mapType))
        {
            return false;
        }
        if (highQuality)
        {
            return scaledTexture != null && scaledTexture.isBound();
        }
        else
        {
            return getRegionTextureHolder().getTexture().isBound();
        }
    }

    private boolean updateRegionTexture()
    {
        updateRegionTimer.start();

        if (regionFuture != null)
        {
            if (!regionFuture.isDone())
            {
                updateRegionTimer.stop();
                return true;
            }
            regionFuture = null;
        }

        ImageHolder imageHolder = getRegionTextureHolder();
        if (imageHolder.hasTexture())
        {
            if (imageHolder.getTexture().isBindNeeded())
            {
                imageHolder.getTexture().bindTexture();
            }
            updateRegionTimer.stop();
            return false;
        }

        regionFuture = TextureCache.instance().scheduleTextureTask(new Callable<TextureImpl>()
        {
            @Override
            public TextureImpl call() throws Exception
            {
                return getRegionTextureHolder().getTexture();
            }
        });

        updateRegionTimer.stop();

        return true;
    }

    private boolean updateScaledTexture()
    {
        updateScaledTimer.start();

        if (scaledFuture != null)
        {
            if (!scaledFuture.isDone())
            {
                updateScaledTimer.stop();
                return true;
            }
            try
            {
                scaledTexture = scaledFuture.get();
                scaledTexture.bindTexture();
            }
            catch (Throwable e)
            {
                logger.error(e);
            }
            scaledFuture = null;
            updateScaledTimer.stop();
            return false;
        }

        if (scaledTexture == null)
        {
            scaledFuture = TextureCache.instance().scheduleTextureTask(new Callable<TextureImpl>()
            {
                @Override
                public TextureImpl call() throws Exception
                {
                    TextureImpl temp = new TextureImpl(null, getScaledRegionArea(), false, false);
                    temp.setDescription("scaled for " + TileDrawStep.this);
                    return temp;
                }
            });
        }
        else if (scaledTexture.getLastImageUpdate() < getRegionTextureHolder().getImageTimestamp())
        {
            final TextureImpl temp = scaledTexture;
            scaledFuture = TextureCache.instance().scheduleTextureTask(new Callable<TextureImpl>()
            {
                @Override
                public TextureImpl call() throws Exception
                {
                    temp.setImage(getScaledRegionArea(), false);
                    return temp;
                }
            });
        }
        updateScaledTimer.stop();
        return true;
    }

    public BufferedImage getScaledRegionArea()
    {
        int scale = (int) Math.pow(2, zoom);
        int scaledSize = Tile.TILESIZE / scale;

        try
        {
            BufferedImage subImage = this.getRegionTextureHolder().getTexture().getImage().getSubimage(sx1, sy1, scaledSize, scaledSize);
            BufferedImage scaledImage = new BufferedImage(Tile.TILESIZE, Tile.TILESIZE, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = RegionImageHandler.initRenderingHints(scaledImage.createGraphics());
            g.drawImage(subImage, 0, 0, Tile.TILESIZE, Tile.TILESIZE, null);
            g.dispose();
            return scaledImage;
        }
        catch (Throwable e)
        {
            logger.error(e);
            return null;
        }
    }
}
