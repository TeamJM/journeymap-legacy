/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.io;


import com.google.common.base.Joiner;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.gson.GsonBuilder;
import cpw.mods.fml.client.FMLClientHandler;
import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.data.WorldData;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.JMLogger;
import journeymap.client.log.LogFormatter;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileHandler
{
    public static final String DEV_MINECRAFT_DIR = "run/";
    public static final String ASSETS_WEBMAP = "/assets/journeymap/web";
    public static final String ASSETS_JOURNEYMAP_UI = "/assets/journeymap/web/img/";
    public static final File MinecraftDirectory = ForgeHelper.INSTANCE.getClient().mcDataDir;
    public static final File JourneyMapDirectory = new File(MinecraftDirectory, Constants.JOURNEYMAP_DIR);
    public static final File StandardConfigDirectory = new File(MinecraftDirectory, Constants.CONFIG_DIR);

    public static File getMinecraftDirectory()
    {
        Minecraft minecraft = FMLClientHandler.instance().getClient();
        if (minecraft != null)
        {
            return minecraft.mcDataDir;
        }
        else
        {
            return new File(DEV_MINECRAFT_DIR);
        }
    }

    public static File getMCWorldDir(Minecraft minecraft)
    {
        if (minecraft.isIntegratedServerRunning())
        {
            String lastMCFolderName = minecraft.getIntegratedServer().getFolderName();
            File lastMCWorldDir = new File(minecraft.mcDataDir, "saves" + File.separator + lastMCFolderName);
            return lastMCWorldDir;
        }
        return null;
    }

    public static File getWorldSaveDir(Minecraft minecraft)
    {
        if (minecraft.isSingleplayer())
        {
            try
            {
                File savesDir = new File(minecraft.mcDataDir, "saves");
                File worldSaveDir = new File(savesDir, minecraft.getIntegratedServer().getFolderName());
                if (minecraft.theWorld.provider.getSaveFolder() != null)
                {
                    File dir = new File(worldSaveDir, minecraft.theWorld.provider.getSaveFolder());
                    dir.mkdirs();
                    return dir;
                }
                else
                {
                    return worldSaveDir;
                }
            }
            catch (Throwable t)
            {
                Journeymap.getLogger().error("Error getting world save dir: %s", t);
            }
        }
        return null;
    }

    public static File getMCWorldDir(Minecraft minecraft, final int dimension)
    {
        File worldDir = getMCWorldDir(minecraft);
        if (worldDir == null)
        {
            return null;
        }
        if (dimension == 0)
        {
            return worldDir;
        }
        else
        {
            final String dimString = Integer.toString(dimension);
            File dimDir = null;

            // Normal dimensions handled this way
            if (dimension == -1 || dimension == 1)
            {
                dimDir = new File(worldDir, "DIM" + dimString); //$NON-NLS-1$
            }

            // Custom dimensions handled this way
            // TODO: Use Forge dimensionhandler to get directory name.  This is a brittle workaround.
            if (dimDir == null || !dimDir.exists())
            {
                File[] dims = worldDir.listFiles(new FilenameFilter()
                {
                    @Override
                    public boolean accept(File dir, String name)
                    {
                        return name.startsWith("DIM") && name.endsWith(dimString) && !name.endsWith("-" + dimString); // this last part prevents negative matches, but may nerf a dumb naming scheme
                    }
                });

                if (dims.length == 0)
                {
                    dimDir = dims[0];
                }
                else
                {
                    // 7 might match DIM7 and DIM17.  Sort and return shortest filename.
                    List<File> list = Arrays.asList(dims);
                    Collections.sort(list, new Comparator<File>()
                    {
                        @Override
                        public int compare(File o1, File o2)
                        {
                            return new Integer(o1.getName().length()).compareTo(o2.getName().length());
                        }
                    });
                    return list.get(0);
                }
            }

            return dimDir;
        }
    }

    public static File getJourneyMapDir()
    {
        return JourneyMapDirectory;
    }

    public static File getJMWorldDir(Minecraft minecraft)
    {
        if (minecraft.theWorld == null)
        {
            return null;
        }

        if (!minecraft.isSingleplayer())
        {
            return getJMWorldDir(minecraft, JourneymapClient.getInstance().getCurrentWorldId());
        }
        else
        {
            return getJMWorldDir(minecraft, null);
        }
    }

    public static synchronized File getJMWorldDir(Minecraft minecraft, String worldId)
    {
        if (minecraft.theWorld == null)
        {
            return null;
        }

        File worldDirectory = null;

        try
        {

            worldDirectory = getJMWorldDirForWorldId(minecraft, worldId);

            if (worldDirectory.exists())
            {
                return worldDirectory;
            }

            File defaultWorldDirectory = FileHandler.getJMWorldDirForWorldId(minecraft, null);

            if (worldId != null && defaultWorldDirectory.exists() && !worldDirectory.exists())
            {
                Journeymap.getLogger().info("Moving default directory to {}", worldDirectory);
                try
                {
                    migrateLegacyFolderName(defaultWorldDirectory, worldDirectory);
                    return worldDirectory;
                }
                catch (Exception e)
                {
                    Journeymap.getLogger().log(Level.ERROR, LogFormatter.toString(e));
                }
            }

            if (!minecraft.isSingleplayer())
            {
                String legacyWorldName;
                File legacyWorldDir;

                boolean migrated = false;

                // Older use of MP server's socket IP/hostname
                legacyWorldName = WorldData.getLegacyServerName() + "_0";
                legacyWorldDir = new File(MinecraftDirectory, Constants.MP_DATA_DIR + legacyWorldName);
                if (legacyWorldDir.exists()
                        && !legacyWorldDir.getName().equals(defaultWorldDirectory.getName())
                        && !legacyWorldDir.getName().equals(worldDirectory.getName()))
                {
                    migrateLegacyFolderName(legacyWorldDir, worldDirectory);
                    migrated = true;
                }

                if (worldId != null)
                {
                    // Newer URL-encoded use of MP server entry provided by user, with world id
                    legacyWorldName = WorldData.getWorldName(minecraft, true) + "_" + worldId;
                    legacyWorldDir = new File(MinecraftDirectory, Constants.MP_DATA_DIR + legacyWorldName);
                    if (legacyWorldDir.exists()
                            && !legacyWorldDir.getName().equals(worldDirectory.getName()))
                    {
                        migrateLegacyFolderName(legacyWorldDir, worldDirectory);
                        migrated = true;
                    }
                }
            }
            else
            {
                File legacyWorldDir = new File(MinecraftDirectory, Constants.SP_DATA_DIR + WorldData.getWorldName(minecraft, true));
                if (!legacyWorldDir.getName().equals(worldDirectory.getName()))
                {
                    if (legacyWorldDir.exists() && worldDirectory.exists())
                    {
                        JMLogger.logOnce(String.format("Found two directories that might be in conflict. Using:  %s , Ignoring: %s", worldDirectory, legacyWorldDir), null);
                    }
                }

                if (legacyWorldDir.exists() && !worldDirectory.exists() && !legacyWorldDir.getName().equals(worldDirectory.getName()))
                {
                    migrateLegacyFolderName(legacyWorldDir, worldDirectory);
                }
            }

            if (!worldDirectory.exists())
            {
                if (!(worldId != null && worldDirectory.getName().equals(defaultWorldDirectory.getName())))
                {
                    worldDirectory.mkdirs();
                }
            }

        }
        catch (Exception e)
        {
            Journeymap.getLogger().log(Level.ERROR, LogFormatter.toString(e));
            throw new RuntimeException(e);
        }

        return worldDirectory;
    }

    public static File getJMWorldDirForWorldId(Minecraft minecraft, String worldId)
    {
        if (minecraft.theWorld == null)
        {
            return null;
        }

        File testWorldDirectory = null;
        try
        {
            if (!minecraft.isSingleplayer())
            {
                if (worldId != null)
                {
                    worldId = worldId.replaceAll("\\W+", "~");
                }
                String suffix = (worldId != null) ? ("_" + worldId) : "";
                testWorldDirectory = new File(MinecraftDirectory, Constants.MP_DATA_DIR + WorldData.getWorldName(minecraft, false) + suffix); //$NON-NLS-1$
            }
            else
            {
                testWorldDirectory = new File(MinecraftDirectory, Constants.SP_DATA_DIR + WorldData.getWorldName(minecraft, false));
            }
        }
        catch (Exception e)
        {
            Journeymap.getLogger().log(Level.ERROR, LogFormatter.toString(e));
        }

        return testWorldDirectory;
    }

    private static void migrateLegacyFolderName(File legacyWorldDir, File worldDir)
    {
        if (legacyWorldDir.getPath().equals(worldDir.getPath()))
        {
            return;
        }

        boolean success = false;
        try
        {
            success = legacyWorldDir.renameTo(worldDir);
            if (!success)
            {
                throw new IllegalStateException("Need to rename legacy folder, but not able to");
            }
            Journeymap.getLogger().info("Migrated legacy folder from {} to {}", legacyWorldDir.getName(), worldDir.getName());
        }
        catch (Exception e)
        {
            JMLogger.logOnce(String.format("Failed to migrate legacy folder from %s to %s", legacyWorldDir.getName(), worldDir.getName()), e);

            String tempName = worldDir.getName() + "__OLD";
            try
            {
                success = legacyWorldDir.renameTo(new File(legacyWorldDir.getParentFile(), tempName));
            }
            catch (Exception e2)
            {
                success = false;
            }
            if (!success)
            {
                JMLogger.logOnce(String.format("Failed to even rename legacy folder from %s to %s", legacyWorldDir.getName(), tempName), e);
            }
        }
    }

    public static File getWaypointDir()
    {
        return getWaypointDir(getJMWorldDir(ForgeHelper.INSTANCE.getClient()));
    }

    public static File getWaypointDir(File jmWorldDir)
    {
        File waypointDir = new File(jmWorldDir, "waypoints");
        if (!waypointDir.isDirectory())
        {
            waypointDir.delete();
        }
        if (!waypointDir.exists())
        {
            waypointDir.mkdirs();
        }
        return waypointDir;
    }

    public static BufferedImage getWebImage(String fileName)
    {
        try
        {
            String png = FileHandler.ASSETS_WEBMAP + "/img/" + fileName;//$NON-NLS-1$
            InputStream is = JourneymapClient.class.getResourceAsStream(png);
            if (is == null)
            {
                Journeymap.getLogger().warn("Image not found: {}", png);
                return null;
            }
            BufferedImage img = ImageIO.read(is);
            is.close();
            return img;
        }
        catch (IOException e)
        {
            String error = "Could not get web image " + fileName + ": " + e.getMessage();
            Journeymap.getLogger().error(error);
            return null;
        }
    }

    public static Properties getLangFile(String fileName)
    {
        try
        {
            InputStream is = JourneymapClient.class.getResourceAsStream("/assets/journeymap/lang/" + fileName);
            if (is == null)
            {
                Journeymap.getLogger().warn("Language file not found: {}", fileName);
                return null;
            }
            Properties properties = new Properties();
            properties.load(is);
            is.close();
            return properties;
        }
        catch (IOException e)
        {
            String error = "Could not get language file " + fileName + ": " + (e.getMessage());
            Journeymap.getLogger().error(error);
            return null;
        }
    }

    public static <M> M getMessageModel(Class<M> model, String filePrefix)
    {
        try
        {
            String lang = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
            InputStream is = getMessageModelInputStream(filePrefix, lang);
            if (is == null && !lang.equals("en_US"))
            {
                is = getMessageModelInputStream(filePrefix, "en_US");
            }
            if (is == null)
            {
                Journeymap.getLogger().warn("Message file not found: {}", filePrefix);
                return null;
            }
            return new GsonBuilder().create().fromJson(new InputStreamReader(is), model);
        }
        catch (Throwable e)
        {
            String error = "Could not get Message model " + filePrefix + ": " + (e.getMessage());
            Journeymap.getLogger().error(error);
            return null;
        }
    }

    public static InputStream getMessageModelInputStream(String filePrefix, String lang)
    {
        String file = String.format("/assets/journeymap/lang/message/%s-%s.json", filePrefix, lang);
        return JourneymapClient.class.getResourceAsStream(file);
    }

    public static File getWorldConfigDir(boolean fallbackToStandardConfigDir)
    {
        File worldDir = getJMWorldDirForWorldId(ForgeHelper.INSTANCE.getClient(), null); // always use the "base" folder for multiplayer
        if (worldDir != null && worldDir.exists())
        {
            File worldConfigDir = new File(worldDir, "config");
            if (worldConfigDir.exists())
            {
                return worldConfigDir;
            }
        }

        return fallbackToStandardConfigDir ? StandardConfigDirectory : null;
    }

    public static BufferedImage getImage(File imageFile)
    {
        try
        {
            if (!imageFile.canRead())
            {
                return null;
            }
            return ImageIO.read(imageFile);
        }
        catch (IOException e)
        {
            String error = "Could not get imageFile " + imageFile + ": " + (e.getMessage());
            Journeymap.getLogger().error(error);
            return null;
        }
    }

    public static boolean isInJar()
    {
        URL location = JourneymapClient.class.getProtectionDomain().getCodeSource().getLocation();
        return "jar".equals(location.getProtocol());
    }

    public static File copyColorPaletteHtmlFile(File toDir, String fileName)
    {
        try
        {
            final File outFile = new File(toDir, fileName);
            String htmlPath = FileHandler.ASSETS_WEBMAP + "/" + fileName;
            InputStream inputStream = JourneymapClient.class.getResource(htmlPath).openStream();

            ByteSink out = new ByteSink()
            {
                @Override
                public OutputStream openStream() throws IOException
                {
                    return new FileOutputStream(outFile);
                }
            };
            out.writeFrom(inputStream);

            return outFile;
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().warn("Couldn't copy color palette html: {}", String.valueOf(t));
            return null;
        }
    }

    public static boolean copyResources(File targetDirectory, String assetsPath, String setName, boolean overwrite)
    {
        return copyResources(targetDirectory, assetsPath, Collections.singletonList(setName), overwrite);
    }

    public static boolean copyResources(File targetDirectory, String assetsPath, Collection<String> setNames, boolean overwrite)
    {
        String fromPath = null;
        try
        {
            URL resourceDir = JourneymapClient.class.getResource(assetsPath);
            boolean inJar = FileHandler.isInJar();
            if (inJar)
            {
                final String resourceDirString = resourceDir.toString();
                final URL jarUrl = new URL(resourceDirString.substring(4, resourceDirString.lastIndexOf('!')));
                fromPath = Paths.get(jarUrl.toURI()).toString();
                FileHandler.copyFromZip(fromPath, assetsPath, setNames, targetDirectory, overwrite);
            }
            else
            {
                for (String setName : setNames)
                {
                    String toPath = assetsPath + "/" + setName;
                    File fromDir = new File(JourneymapClient.class.getResource(toPath).getFile());
                    File toDir = new File(targetDirectory, setName);
                    fromPath = fromDir.getPath();
                    FileHandler.copyFromDirectory(fromDir, toDir, overwrite);
                }
            }
            return true;
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Couldn't unzip resource sets from {} to {}: {}", fromPath, targetDirectory, t);
        }
        return false;
    }

    /**
     * Copies files from zipFilePath/setName/* to targetDirectory/setName/*
     *
     * @param zipFilePath path to a zip file to copy from
     * @param assetsPath path to assets inside the zip file
     * @param iconSetNames folder names located inside assetsPath
     * @param targetDirectory target directory to copy into
     * @param overwrite whether to copy the file if it already exists
     */
    static void copyFromZip(String zipFilePath, String assetsPath, Collection<String> iconSetNames, File targetDirectory, boolean overwrite) throws IOException
    {
        // it begins with '/', but we don't need it here
        assetsPath = assetsPath.substring(1);

        try (ZipFile zipFile = new ZipFile(zipFilePath))
        {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                String path = entry.getName();
                if (entry.isDirectory()) continue;
                if (!path.startsWith(assetsPath)) continue;

                for (String iconSetName : iconSetNames)
                {
                    String iconSetPath = assetsPath + "/" + iconSetName;
                    if (!path.startsWith(iconSetPath)) continue;

                    File destDir = new File(targetDirectory, iconSetName);
                    File toFile = new File(destDir, path.substring(iconSetPath.length()));
                    if (overwrite || !toFile.exists())
                    {
                        Files.createParentDirs(toFile);
                        new ZipEntryByteSource(zipFile, entry).copyTo(Files.asByteSink(toFile));
                    }
                    break;
                }
            }
        }
    }

    /**
     * Copies contents of one directory to another
     */
    static void copyFromDirectory(File fromDir, File toDir, boolean overWrite) throws IOException
    {
        toDir.mkdir();
        File[] files = fromDir.listFiles();

        if (files == null)
        {
            throw new IOException(fromDir + " nas no files");
        }

        for (File from : files)
        {
            File to = new File(toDir, from.getName());
            if (from.isDirectory())
            {
                copyFromDirectory(from, to, overWrite);
            }
            else if (overWrite || !to.exists())
            {
                Files.copy(from, to);
            }
        }
    }

    public static boolean delete(File file)
    {
        if (!file.exists())
        {
            return true;
        }

        if (file.isFile())
        {
            return file.delete();
        }
        else
        {
            try
            {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {}
            return !file.exists();
        }
    }

    public static BufferedImage getIconFromFile(File parentdir, String assetsPath, String setName, String iconPath, BufferedImage defaultImg)
    {
        BufferedImage img = null;
        if (iconPath == null)
        {
            // Will make error messages easier to interpret.
            iconPath = "null";
        }

        File iconFile = null;

        try
        {
            String filePath = Joiner.on(File.separatorChar).join(setName, iconPath.replace('/', File.separatorChar));
            iconFile = new File(parentdir, filePath);


            if (iconFile.exists())
            {
                img = FileHandler.getImage(iconFile);
            }

            if (img == null)
            {
                img = FileHandler.getIconFromResource(assetsPath, setName, iconPath);
                if (img == null && defaultImg != null)
                {
                    img = defaultImg;
                    try
                    {
                        iconFile.getParentFile().mkdirs();
                        ImageIO.write(img, "png", iconFile);
                    }
                    catch (Exception e)
                    {
                        String error = "FileHandler can't write image: " + iconFile + ": " + e;
                        Journeymap.getLogger().error(error);
                    }

                    Journeymap.getLogger().debug("Created image: {}", iconFile);
                }
                else
                {
                    String pngPath = Joiner.on('/').join(assetsPath, setName, iconPath);
                    Journeymap.getLogger().error("Can't get image from file ({}) nor resource ({}) ", iconFile, pngPath);
                }
            }
        }
        catch (Exception e)
        {
            JMLogger.logOnce("Couldn't load iconset file: " + iconFile, e);
        }

        return img;
    }

    public static BufferedImage getIconFromResource(String assetsPath, String setName, String iconPath)
    {
        try
        {
            InputStream is = getIconStream(assetsPath, setName, iconPath);
            if (is == null)
            {
                return null;
            }
            BufferedImage img = ImageIO.read(is);
            is.close();
            return img;
        }
        catch (IOException e)
        {
            String error = String.format("Could not get icon from resource: %s, %s, %s : %s", assetsPath, setName, iconPath, e.getMessage());
            Journeymap.getLogger().error(error);
            return null;
        }
    }

    public static InputStream getIconStream(String assetsPath, String setName, String iconPath)
    {
        try
        {
            String pngPath = Joiner.on('/').join(assetsPath, setName, iconPath);
            InputStream is = JourneymapClient.class.getResourceAsStream(pngPath);
            if (is == null)
            {
                Journeymap.getLogger().warn("Icon Set asset not found: {}", pngPath);
                return null;
            }
            return is;
        }
        catch (Throwable e)
        {
            String error = String.format("Could not get icon stream: %s, %s, %s : %s", assetsPath, setName, iconPath, e.getMessage());
            Journeymap.getLogger().error(error);
            return null;
        }
    }

    private static class ZipEntryByteSource extends ByteSource
    {
        final ZipFile file;
        final ZipEntry entry;

        ZipEntryByteSource(ZipFile file, ZipEntry entry)
        {
            this.file = file;
            this.entry = entry;
        }

        @Override
        public InputStream openStream() throws IOException
        {
            return file.getInputStream(entry);
        }

        @Override
        public String toString()
        {
            return String.format("ZipEntryByteSource( %s / %s )", file, entry);
        }
    }
}
