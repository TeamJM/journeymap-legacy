/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.render.texture;

import journeymap.client.io.FileHandler;
import journeymap.client.io.IconSetFileHandler;
import journeymap.client.io.RegionImageHandler;
import journeymap.client.io.ThemeFileHandler;
import journeymap.client.model.MapType;
import journeymap.client.task.main.ExpireTextureTask;
import journeymap.client.ui.theme.Theme;
import journeymap.common.Journeymap;
import journeymap.common.thread.JMThreadFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * TODO:  Make this actually act like a cache.
 * For now it's just a dumping ground for all textures that need to be retained
 *
 * @author techbrew
 */
public class TextureCache
{
    private final Map<Name, TextureImpl> namedTextures = Collections.synchronizedMap(new HashMap<Name, TextureImpl>(Name.values().length + (Name.values().length / 2) + 1));
    //private final Map<String, TextureImpl> customTextures = Collections.synchronizedMap(new HashMap<String, TextureImpl>(3));
    private final Map<String, TextureImpl> playerSkins = Collections.synchronizedMap(new HashMap<String, TextureImpl>());
    private final Map<String, TextureImpl> entityIcons = Collections.synchronizedMap(new HashMap<String, TextureImpl>());
    private final Map<String, TextureImpl> themeImages = Collections.synchronizedMap(new HashMap<String, TextureImpl>());

    private ThreadPoolExecutor texExec = new ThreadPoolExecutor(2, 4, 15L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(8), new JMThreadFactory("texture"), new ThreadPoolExecutor.CallerRunsPolicy());

    private TextureCache()
    {
    }

    public static TextureCache instance()
    {
        return Holder.INSTANCE;
    }

    public Future<TextureImpl> scheduleTextureTask(Callable<TextureImpl> textureTask)
    {
        //JourneyMap.getLogger().info("TextureCache.scheduleTextureTask()");
        return texExec.submit(textureTask);
    }

    /**
     * *********************************************
     */

//    public TextureImpl getCustomTexture(String filename, boolean retain) {
//        synchronized(customTextures)
//        {
//            TextureImpl tex = customTextures.get(filename);
//            if(tex==null || (!tex.hasImage() && retain)) {
//                BufferedImage img = FileHandler.getImage(filename);
//                if(img==null){
//                    img = getUnknownEntity().getImage();
//                }
//                if(img!=null){
//                    if(tex!=null){
//                        tex.queueForDeletion();
//                    }
//                    tex = new TextureImpl(img, retain);
//                    customTextures.put(filename, tex);
//                }
//            }
//            return tex;
//        }
//    }
    private TextureImpl getNamedTexture(Name name, String filename, boolean retain)
    {
        synchronized (namedTextures)
        {
            TextureImpl tex = namedTextures.get(name);
            if (tex == null || (!tex.hasImage() && retain))
            {
                BufferedImage img = FileHandler.getWebImage(filename);
                if (img == null)
                {
                    img = getUnknownEntity().getImage();
                }
                if (img != null)
                {
                    if (tex != null)
                    {
                        tex.queueForDeletion();
                    }
                    tex = new TextureImpl(img, retain);
                    tex.setDescription(String.format("%s (%s)", name, filename));
                    namedTextures.put(name, tex);
                }
            }
            return tex;
        }
    }

    public TextureImpl getMinimapCustomSquare(int size, float alpha)
    {
        size = Math.max(64, Math.min(size, 1024));
        alpha = Math.max(0, Math.min(alpha, 1));

        final String frameImg;
        if (size <= 128)
        {
            frameImg = "minimap/minimap-square-128.png";
        }
        else if (size <= 256)
        {
            frameImg = "minimap/minimap-square-256.png";
        }
        else if (size <= 512)
        {
            frameImg = "minimap/minimap-square-512.png";
        }
        else
        {
            frameImg = "minimap/minimap-square-128.png";
        }

        BufferedImage img = FileHandler.getWebImage(frameImg);
        BufferedImage resizedImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) resizedImg.getGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(img, 0, 0, size, size, null);
        g.dispose();

        TextureImpl tex = namedTextures.get(Name.MinimapCustomSquare);
        if (tex != null)
        {
            tex.queueForDeletion();
        }

        tex = new TextureImpl(resizedImg, false);
        namedTextures.put(Name.MinimapCustomSquare, tex);
        return tex;
    }

    public TextureImpl getWaypoint()
    {
        return getNamedTexture(Name.Waypoint, "waypoint.png", false); //$NON-NLS-1$
    }

    public TextureImpl getWaypointEdit()
    {
        return getNamedTexture(Name.WaypointEdit, "waypoint-edit.png", false); //$NON-NLS-1$
    }

    public TextureImpl getWaypointOffscreen()
    {
        return getNamedTexture(Name.WaypointOffscreen, "waypoint-offscreen.png", false); //$NON-NLS-1$
    }

    public TextureImpl getDeathpoint()
    {
        return getNamedTexture(Name.Deathpoint, "waypoint-death.png", false); //$NON-NLS-1$
    }

    public TextureImpl getLogo()
    {
        return getNamedTexture(Name.Logo, "ico/journeymap60.png", false); //$NON-NLS-1$
    }

    public TextureImpl getPatreonLogo()
    {
        return getNamedTexture(Name.Patreon, "patreon.png", false); //$NON-NLS-1$
    }

    public TextureImpl getHostileLocator()
    {
        return getNamedTexture(Name.LocatorHostile, "locator-hostile.png", false); //$NON-NLS-1$
    }

    public TextureImpl getNeutralLocator()
    {
        return getNamedTexture(Name.LocatorNeutral, "locator-neutral.png", false); //$NON-NLS-1$
    }

    public TextureImpl getOtherLocator()
    {
        return getNamedTexture(Name.LocatorOther, "locator-other.png", false); //$NON-NLS-1$
    }

    public TextureImpl getPetLocator()
    {
        return getNamedTexture(Name.LocatorPet, "locator-pet.png", false); //$NON-NLS-1$
    }

    public TextureImpl getPlayerLocator()
    {
        return getNamedTexture(Name.LocatorPlayer, "locator-player.png", false); //$NON-NLS-1$
    }

    public TextureImpl getPlayerLocatorSmall()
    {
        return getNamedTexture(Name.LocatorPlayerSmall, "locator-player-sm.png", false); //$NON-NLS-1$
    }

    public TextureImpl getColorPicker()
    {
        return getNamedTexture(Name.ColorPicker, "colorpick.png", true); //$NON-NLS-1$
    }

    public TextureImpl getUnknownEntity()
    {
        return getNamedTexture(Name.UnknownEntity, "unknown.png", true); //$NON-NLS-1$
    }

    public TextureImpl getBrick()
    {
        return getNamedTexture(Name.Brick, "brick.png", true); //$NON-NLS-1$
    }

    public TextureImpl getGrid(Name name)
    {
        switch (name)
        {
            case GridCheckers:
                return getNamedTexture(Name.GridCheckers, "grid-checkers.png", true);
            case GridDots:
                return getNamedTexture(Name.GridDots, "grid-dots.png", true);
            default:
                return getNamedTexture(Name.GridSquares, "grid.png", true);
        }
    }

    public TextureImpl getTileSample(MapType mapType)
    {
        if (mapType.isNight())
        {
            return getTileSampleNight();
        }
        else if (mapType.isUnderground())
        {
            return getTileSampleUnderground();
        }
        else
        {
            return getTileSampleDay();
        }
    }

    public TextureImpl getTileSampleDay()
    {
        return getNamedTexture(Name.TileSampleDay, "tile-sample-day.png", true); //$NON-NLS-1$
    }

    public TextureImpl getTileSampleNight()
    {
        return getNamedTexture(Name.TileSampleNight, "tile-sample-night.png", true); //$NON-NLS-1$
    }

    public TextureImpl getTileSampleUnderground()
    {
        return getNamedTexture(Name.TileSampleUnderground, "tile-sample-underground.png", true); //$NON-NLS-1$
    }

    /**
     * *************************************************
     */

    public TextureImpl getEntityIconTexture(String setName, String iconPath)
    {
        String texName = String.format("%s/%s", setName, iconPath);
        synchronized (entityIcons)
        {
            TextureImpl tex = entityIcons.get(texName);
            if (tex == null || (!tex.hasImage() && tex.retainImage))
            {
                File parentDir = IconSetFileHandler.getEntityIconDir();
                String assetPath = IconSetFileHandler.ASSETS_JOURNEYMAP_ICON_ENTITY;
                BufferedImage img = FileHandler.getIconFromFile(parentDir, assetPath, setName, iconPath, getUnknownEntity().getImage()); //$NON-NLS-1$
                if (img != null)
                {
                    if (tex != null)
                    {
                        tex.queueForDeletion();
                    }
                    tex = new TextureImpl(img);
                    entityIcons.put(texName, tex);
                }
            }
            return tex;
        }
    }

    public TextureImpl getThemeTexture(Theme theme, String iconPath)
    {
        return getThemeTexture(theme, iconPath, 0, 0, false, 1f, false);
    }

    public TextureImpl getThemeTexture(Theme theme, String iconPath, int width, int height, boolean resize, float alpha, boolean retainImage)
    {
        String texName = String.format("%s/%s", theme.directory, iconPath);
        synchronized (themeImages)
        {
            TextureImpl tex = themeImages.get(texName);
            if (tex == null || (!tex.hasImage() && tex.retainImage) || (resize && (width != tex.width || height != tex.height)) || tex.alpha != alpha)
            {
                File parentDir = ThemeFileHandler.getThemeIconDir();
                String assetPath = ThemeFileHandler.ASSETS_JOURNEYMAP_ICON_THEME;
                BufferedImage img = FileHandler.getIconFromFile(parentDir, assetPath, theme.directory, iconPath, null); //$NON-NLS-1$
                if (img != null)
                {
                    if (resize || alpha < 1f)
                    {
                        if (alpha < 1f || img.getWidth() != width || img.getHeight() != height)
                        {
                            BufferedImage tmp = new BufferedImage(width, height, img.getType());
                            Graphics2D g = tmp.createGraphics();
                            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g.drawImage(img, 0, 0, width, height, null);
                            g.dispose();
                            img = tmp;
                        }
                    }

                    if (tex != null)
                    {
                        tex.queueForDeletion();
                    }
                    tex = new TextureImpl(img, retainImage);
                    tex.alpha = alpha;
                    themeImages.put(texName, tex);
                }
                else
                {
                    Journeymap.getLogger().error("Unknown theme image: " + texName);
                    return getUnknownEntity();
                }
            }
            return tex;
        }
    }

    public TextureImpl getScaledCopy(String texName, TextureImpl original, int width, int height, float alpha)
    {
        synchronized (themeImages)
        {
            TextureImpl tex = themeImages.get(texName);
            if (tex == null || (!tex.hasImage() && tex.retainImage) || (width != tex.width || height != tex.height) || tex.alpha != alpha)
            {
                BufferedImage img = original.getImage();
                if (img != null)
                {
                    if (alpha < 1f || img.getWidth() != width || img.getHeight() != height)
                    {
                        BufferedImage tmp = new BufferedImage(width, height, img.getType());
                        Graphics2D g = tmp.createGraphics();
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.drawImage(img, 0, 0, width, height, null);
                        g.dispose();
                        img = tmp;
                    }

                    if (tex != null)
                    {
                        tex.queueForDeletion();
                    }
                    tex = new TextureImpl(img);
                    tex.alpha = alpha;
                    themeImages.put(texName, tex);
                }
                else
                {
                    Journeymap.getLogger().error("Unable to get scaled image: " + texName);
                    return getUnknownEntity();
                }
            }
            return tex;
        }
    }

    /**
     * Get the head portion of a player's skin, scaled to 24x24 pixels.
     */
    public TextureImpl getPlayerSkin(final String username)
    {
        TextureImpl tex = null;
        synchronized (playerSkins)
        {
            tex = playerSkins.get(username);
            if (tex != null)
            {
                return tex;
            }
            else
            {
                // Create blank to return immediately
                BufferedImage blank = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
                tex = new TextureImpl(null, blank, true, false);
                playerSkins.put(username, tex);
            }
        }

        final TextureImpl playerSkinTex = tex;

        // Load it async
        texExec.submit(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                BufferedImage img = downloadSkin(username);
                if (img != null)
                {
                    final BufferedImage scaledImage = new BufferedImage(24, 24, img.getType());
                    final Graphics2D g = RegionImageHandler.initRenderingHints(scaledImage.createGraphics());
                    g.drawImage(img, 0, 0, 24, 24, null);
                    g.dispose();
                    playerSkinTex.setImage(scaledImage, true);
                }
                else
                {
                    Journeymap.getLogger().warn("Couldn't get a skin at all for " + username);
                }
                return null;
            }
        });

        return playerSkinTex;
    }

    /**
     * Blocks.  Use this in a thread.
     */
    protected BufferedImage downloadSkin(String username)
    {
        BufferedImage img = null;
        HttpURLConnection conn = null;
        try
        {
            String skinPath = String.format("http://skins.minecraft.net/MinecraftSkins/%s.png", StringUtils.stripControlCodes(username));
            img = downloadImage(new URL(skinPath));
            if (img == null)
            {
                img = downloadImage(new URL("https://minecraft.net/images/steve.png"));
            }
        }
        catch (Throwable e)
        {
            Journeymap.getLogger().warn("Error getting skin image for " + username + ": " + e.getMessage());
        }
        return img;
    }

    private BufferedImage downloadImage(URL imageURL)
    {
        BufferedImage img = null;
        HttpURLConnection conn = null;
        try
        {
            conn = (HttpURLConnection) imageURL.openConnection(Minecraft.getMinecraft().getProxy());
            HttpURLConnection.setFollowRedirects(true);
            conn.setInstanceFollowRedirects(true);
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(1000);
            conn.connect();
            if (conn.getResponseCode() / 100 == 2) // can't get input stream before response code available
            {
                img = ImageIO.read(conn.getInputStream()).getSubimage(8, 8, 8, 8);
            }
            else
            {
                Journeymap.getLogger().warn("Bad Response getting image: " + imageURL + " : " + conn.getResponseCode());
            }
        }
        catch (Throwable e)
        {
            Journeymap.getLogger().warn("Error getting skin image: " + imageURL + " : " + e.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }

        return img;
    }

    public void purge()
    {
        synchronized (namedTextures)
        {
            ExpireTextureTask.queue(namedTextures.values());
            namedTextures.clear();
        }

        synchronized (entityIcons)
        {
            ExpireTextureTask.queue(entityIcons.values());
            entityIcons.clear();
        }

        synchronized (themeImages)
        {
            ExpireTextureTask.queue(themeImages.values());
            themeImages.clear();
        }
    }

    public void purgeThemeImages()
    {
        synchronized (themeImages)
        {
            ExpireTextureTask.queue(themeImages.values());
            themeImages.clear();
        }
    }

    public static enum Name
    {
        MinimapSmallSquare, MinimapMediumSquare, MinimapLargeSquare, MinimapCustomSquare, MinimapSmallCircle,
        MinimapLargeCircle, Waypoint, Deathpoint, WaypointOffscreen, WaypointEdit, Logo, Patreon, LocatorHostile,
        LocatorNeutral, LocatorOther, LocatorPet, LocatorPlayer, LocatorPlayerSmall, ColorPicker, UnknownEntity,
        GridSquares, GridDots, GridCheckers, Brick,
        TileSampleDay, TileSampleNight, TileSampleUnderground
    }

    private static class Holder
    {
        private static final TextureCache INSTANCE = new TextureCache();
    }
}
