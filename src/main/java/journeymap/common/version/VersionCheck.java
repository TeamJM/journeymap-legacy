/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.common.version;

import com.google.common.io.CharStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.common.Loader;
import journeymap.common.Journeymap;
import journeymap.common.thread.JMThreadFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 1.8
//import net.minecraftforge.fml.common.Loader;

/**
 * Checks to see if a newer released version of JourneyMap is available.
 */
public class VersionCheck
{
    private static volatile ExecutorService executorService;
    private static volatile Boolean updateCheckEnabled = Journeymap.proxy.isUpdateCheckEnabled();
    private static volatile Boolean versionIsCurrent = true;
    private static volatile Boolean versionIsChecked;
    private static volatile String versionAvailable;
    private static volatile String downloadUrl;

    /**
     * Whether this build is the current version available.
     *
     * @return
     */
    public static Boolean getVersionIsCurrent()
    {
        if (versionIsChecked == null)
        {
            checkVersion();
        }
        return versionIsCurrent;
    }

    /**
     * Whether the current version available has been checked.
     *
     * @return
     */
    public static Boolean getVersionIsChecked()
    {
        if (versionIsChecked == null)
        {
            checkVersion();
        }
        return versionIsChecked;
    }

    /**
     * Gets the current version available.
     *
     * @return
     */
    public static String getVersionAvailable()
    {
        if (versionIsChecked == null)
        {
            checkVersion();
        }
        return versionAvailable;
    }

    /**
     * Gets the current version available.
     *
     * @return
     */
    public static String getDownloadUrl()
    {
        if (versionIsChecked == null)
        {
            checkVersion();
        }
        return downloadUrl;
    }

    /**
     * Check for the current version by querying a JSON file that is
     * manually updated when a new version is released.
     */
    private static synchronized void checkVersion()
    {
        versionIsChecked = false;
        versionIsCurrent = true;
        versionAvailable = "0"; //$NON-NLS-1$

        if (!updateCheckEnabled)
        {
            Journeymap.getLogger().info("Update check disabled in properties file."); //$NON-NLS-1$
        }
        else
        {
            executorService = Executors.newSingleThreadExecutor(new JMThreadFactory("VersionCheck"));
            executorService.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    InputStreamReader in = null;
                    HttpURLConnection connection = null;
                    String rawResponse = null;
                    try
                    {
                        URL uri = URI.create(Journeymap.VERSION_URL).toURL();
                        connection = (HttpURLConnection) uri.openConnection();
                        connection.setConnectTimeout(6000);
                        connection.setReadTimeout(6000);
                        connection.setRequestMethod("GET");
                        in = new InputStreamReader(uri.openStream());
                        rawResponse = CharStreams.toString(in);

                        String currentVersion = Journeymap.JM_VERSION.toString();
                        boolean currentIsRelease = Journeymap.JM_VERSION.isRelease();

                        JsonObject project = new JsonParser().parse(rawResponse).getAsJsonObject();
                        JsonElement version = project.get("versions").getAsJsonObject().get(Loader.MC_VERSION);
                        if(version==null)
                        {
                            Journeymap.getLogger().warn("No versions found online for " + Loader.MC_VERSION);
                        }
                        else
                        {
                            Iterator<JsonElement> files = version.getAsJsonArray().iterator();
                            while (files.hasNext())
                            {
                                JsonObject file = files.next().getAsJsonObject();
                                try
                                {
                                    // {"id":2264681,"url":"http:\/\/curse.com\/mc-mods\/minecraft\/journeymap-32274\/2264681","name":"journeymap-1.7.10-5.1.1b9-unlimited.jar","type":"beta","version":"1.7.10","downloads":1435,"created_at":"2015-10-31T02:50:09+0000"}
                                    JsonElement type = file.get("type");
                                    if (currentIsRelease && !("release".equals(type.getAsString())))
                                    {
                                        continue;
                                    }

                                    String name = file.get("name").getAsString().split(Loader.MC_VERSION)[1];
                                    if (!name.contains("-"))
                                    {
                                        continue;
                                    }
                                    String fileVersion = name.split("-")[1];
                                    String url = Journeymap.DOWNLOAD_URL + file.get("id").getAsString();
                                    if (!isCurrent(currentVersion, fileVersion))
                                    {
                                        downloadUrl = url;
                                        versionAvailable = fileVersion;
                                        versionIsCurrent = false;
                                        versionIsChecked = true;
                                        Journeymap.getLogger().info(String.format("Newer version online: JourneyMap %s for Minecraft %s on %s", versionAvailable, Loader.MC_VERSION, downloadUrl));
                                        break;
                                    }
                                }
                                catch (Exception e)
                                {
                                    Journeymap.getLogger().error("Could not parse download info: " + file, e); //$NON-NLS-1$
                                }
                            }
                        }

                        if (!versionIsChecked)
                        {
                            versionAvailable = currentVersion;
                            versionIsCurrent = true;
                            versionIsChecked = true;
                            downloadUrl = Journeymap.DOWNLOAD_URL;
                        }
                    }
                    catch (Throwable e)
                    {
                        Journeymap.getLogger().error("Could not check version URL", e); //$NON-NLS-1$
                        updateCheckEnabled = false;
                    }
                    finally
                    {
                        if (in != null)
                        {
                            try
                            {
                                in.close();
                                executorService.shutdown();
                                executorService = null;
                            }
                            catch (IOException e)
                            {
                            }
                        }
                    }

                    // Log newer version
                    if (!versionIsCurrent)
                    {
                        // TODO show once in chat when fullscreen map opened?
                    }
                }
            });
        }
    }

    /**
     * Whether this instance's version is current (equal or newer) to one available.
     *
     * @param thisVersionStr
     * @param availableVersionStr
     * @return
     */
    private static boolean isCurrent(String thisVersionStr, String availableVersionStr)
    {
        if (thisVersionStr.equals(availableVersionStr))
        {
            return true;
        }

        Version thisVersion = Version.from(thisVersionStr, null);
        Version availableVersion = Version.from(availableVersionStr, null);

        return !availableVersion.isNewerThan(thisVersion);
    }
}
