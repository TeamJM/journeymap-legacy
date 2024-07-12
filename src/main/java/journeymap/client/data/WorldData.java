/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.data;

import com.google.common.base.Strings;
import com.google.common.cache.CacheLoader;
import cpw.mods.fml.client.FMLClientHandler;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.feature.Feature;
import journeymap.client.feature.FeatureManager;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.io.IconSetFileHandler;
import journeymap.client.log.JMLogger;
import journeymap.client.log.LogFormatter;
import journeymap.common.Journeymap;
import journeymap.common.version.VersionCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.Display;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

// 1.8
//import net.minecraftforge.fml.client.FMLClientHandler;


/**
 * Provides world properties
 *
 * @author techbrew
 */
public class WorldData extends CacheLoader<Class, WorldData>
{
    String name;
    int dimension;
    long time;
    boolean hardcore;
    boolean singlePlayer;
    Map<Feature, Boolean> features;
    String jm_version;
    String latest_journeymap_version;
    String mc_version;
    String mod_name = JourneymapClient.MOD_NAME;
    String iconSetName;
    String[] iconSetNames;
    int browser_poll;

    public static TIntObjectMap<String> dimNames;

    /**
     * Constructor.
     */
    public WorldData()
    {
    }

    static
    {
        dimNames = new TIntObjectHashMap<>();
        ((IReloadableResourceManager)  Minecraft.getMinecraft()
                .getResourceManager())
                .registerReloadListener(new IResourceManagerReloadListener() {
                    @Override
                    public void onResourceManagerReload(IResourceManager p_110549_1_) { dimNames.clear(); }
                });
    }

    public static boolean isHardcoreAndMultiplayer()
    {
        WorldData world = DataCache.instance().getWorld(false);
        return world.hardcore && !world.singlePlayer;
    }

    private static String getServerName()
    {
        try
        {
            return ForgeHelper.INSTANCE.getRealmsServerName().replace(':', '_');
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Couldn't get service name: " + LogFormatter.toString(t));
            // Fallback
            return getLegacyServerName().replace(':', '_');
        }
    }

    public static String getLegacyServerName()
    {
        try
        {
            NetworkManager netManager = FMLClientHandler.instance().getClientToServerNetworkManager();
            if (netManager != null)
            {
                SocketAddress socketAddress = ForgeHelper.INSTANCE.getSocketAddress(netManager);
                if ((socketAddress != null && socketAddress instanceof InetSocketAddress))
                {
                    InetSocketAddress inetAddr = (InetSocketAddress) socketAddress;
                    return inetAddr.getHostName();
                }
            }
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Couldn't get server name: " + LogFormatter.toString(t));
        }
        return "server";
    }

    /**
     * Get the current world name.
     *
     * @param mc
     * @return
     */
    public static String getWorldName(Minecraft mc, boolean useLegacyName)
    {
        // Get the name
        String worldName = null;
        if (mc.isSingleplayer())
        {
            if (useLegacyName)
            {
                worldName = mc.getIntegratedServer().getWorldName();
            }
            else
            {
                return mc.getIntegratedServer().getFolderName();
            }
        }
        else
        {
            worldName = mc.theWorld.getWorldInfo().getWorldName();
            String serverName = getServerName();

            if (serverName == null)
            {
                return "offline";
            }

            if (!"MpServer".equals(worldName))
            {
                worldName = serverName + "_" + worldName;
            }
            else
            {
                worldName = serverName;
            }
        }

        if (useLegacyName)
        {
            worldName = getLegacyUrlEncodedWorldName(worldName);
        }
        else
        {
            worldName = worldName.trim();
        }

        if (Strings.isNullOrEmpty(worldName.trim()))
        {
            worldName = "unnamed";
        }

        return worldName;
    }

    private static String getLegacyUrlEncodedWorldName(String worldName)
    {
        try
        {
            return URLEncoder.encode(worldName, "UTF-8").replace("+", " ");
        }
        catch (UnsupportedEncodingException e)
        {
            return worldName;
        }
    }

    public static List<WorldProvider> getDimensionProviders(List<Integer> requiredDimensionList)
    {
        try
        {
            HashSet<Integer> requiredDims = new HashSet<Integer>(requiredDimensionList);
            HashMap<Integer, WorldProvider> dimProviders = new HashMap<Integer, WorldProvider>();

            Level logLevel = Level.DEBUG;
            Journeymap.getLogger().log(logLevel, String.format("Required dimensions from waypoints: %s", requiredDimensionList));

            // DimensionIDs works for local servers
            Integer[] dims = DimensionManager.getIDs();
            Journeymap.getLogger().log(logLevel, String.format("DimensionManager has dims: %s", Arrays.asList(dims)));
            requiredDims.addAll(Arrays.asList(dims));

            // StaticDimensionIDs works for remote servers
            dims = DimensionManager.getStaticDimensionIDs();
            Journeymap.getLogger().log(logLevel, String.format("DimensionManager has static dims: %s", Arrays.asList(dims)));
            requiredDims.addAll(Arrays.asList(dims));

            // Use the player's provider
            WorldProvider playerProvider = ForgeHelper.INSTANCE.getClient().thePlayer.worldObj.provider;
            int dimId = ForgeHelper.INSTANCE.getDimension(playerProvider);
            dimProviders.put(dimId, playerProvider);
            requiredDims.remove(dimId);
            Journeymap.getLogger().log(logLevel, String.format("Using player's provider for dim %s: %s", dimId, getSafeDimensionName(playerProvider)));

            // Get a provider for the rest
            for (int dim : requiredDims)
            {
                if (!dimProviders.containsKey(dim))
                {
                    if (DimensionManager.getWorld(dim) != null)
                    {
                        try
                        {
                            WorldProvider dimProvider = DimensionManager.getProvider(dim);
                            dimProvider.getDimensionName(); // Force the name error
                            dimProviders.put(dim, dimProvider);
                            Journeymap.getLogger().log(logLevel, String.format("DimensionManager.getProvider(%s): %s", dim, getSafeDimensionName(dimProvider)));
                        }
                        catch (Throwable t)
                        {
                            JMLogger.logOnce(String.format("Couldn't DimensionManager.getProvider(%s) because of error: %s", dim, t), t);
                        }
                    }
                    else
                    {
                        WorldProvider provider;
                        try
                        {
                            provider = DimensionManager.createProviderFor(dim);
                            provider.getDimensionName(); // Force the name error
                            provider.setDimension(dim);
                            dimProviders.put(dim, provider);
                            Journeymap.getLogger().log(logLevel, String.format("DimensionManager.createProviderFor(%s): %s", dim, getSafeDimensionName(playerProvider)));
                        }
                        catch (Throwable t)
                        {
                            JMLogger.logOnce(String.format("Couldn't DimensionManager.createProviderFor(%s) because of error: %s", dim, t), t);
                        }
                    }
                }
            }

            // Remove required dims that have been found
            requiredDims.removeAll(dimProviders.keySet());

            // Make sure required dimensions are added. Since we got this far without finding providers for them, use fake providers.
            for (int dim : requiredDims)
            {
                if (!dimProviders.containsKey(dim))
                {
                    WorldProvider provider = new FakeDimensionProvider(dim);
                    dimProviders.put(dim, provider);
                    Journeymap.getLogger().warn(String.format("Used FakeDimensionProvider for required dim: %s", dim));
                }
            }

            // Sort by dim and return
            ArrayList<WorldProvider> providerList = new ArrayList<WorldProvider>(dimProviders.values());
            Collections.sort(providerList, new Comparator<WorldProvider>()
            {
                @Override
                public int compare(WorldProvider o1, WorldProvider o2)
                {
                    return Integer.valueOf(ForgeHelper.INSTANCE.getDimension(o1)).compareTo(ForgeHelper.INSTANCE.getDimension(o2));
                }
            });

            return providerList;
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Unexpected error in WorldData.getDimensionProviders(): ", t);
            return Collections.emptyList();
        }
    }

    public static String getSafeDimensionName(WorldProvider worldProvider)
    {
        if (worldProvider == null)
        {
            return null;
        }

        try
        {
            String dimName = dimNames.get(worldProvider.dimensionId);
            if (dimName == null)
            {
                String langKey = String.format("jm.common.dimension.%1$d.name", worldProvider.dimensionId);
                dimName = Constants.getString(langKey);
                if (langKey.equals(dimName))
                {
                    dimName = worldProvider.getDimensionName();
                }
                dimNames.put(worldProvider.dimensionId, dimName);
            }

            return dimName;
        }
        catch (Exception e)
        {
            JMLogger.logOnce(String.format("Failed to retrieve dimension %d error: ", worldProvider.dimensionId), e);
            return Constants.getString("jm.common.dimension", ForgeHelper.INSTANCE.getDimension(worldProvider));
        }
    }

    @Override
    public WorldData load(Class aClass) throws Exception
    {
        Minecraft mc = ForgeHelper.INSTANCE.getClient();
        WorldInfo worldInfo = mc.theWorld.getWorldInfo();

        IntegratedServer server = mc.getIntegratedServer();
        boolean multiplayer = server == null || server.getPublic();

        name = getWorldName(mc, false);
        dimension = ForgeHelper.INSTANCE.getDimension(mc.theWorld.provider);
        hardcore = worldInfo.isHardcoreModeEnabled();
        singlePlayer = !multiplayer;
        time = mc.theWorld.getWorldTime() % 24000L;
        features = FeatureManager.getAllowedFeatures();

        mod_name = JourneymapClient.MOD_NAME;
        jm_version = Journeymap.JM_VERSION.toString();
        latest_journeymap_version = VersionCheck.getVersionAvailable();
        mc_version = Display.getTitle().split("\\s(?=\\d)")[1];
        browser_poll = Math.max(1000, JourneymapClient.getCoreProperties().browserPoll.get());

        iconSetName = JourneymapClient.getFullMapProperties().getEntityIconSetName().get();
        iconSetNames = IconSetFileHandler.getEntityIconSetNames().toArray(new String[0]);

        return this;
    }

    /**
     * Return length of time in millis data should be kept.
     */
    public long getTTL()
    {
        return 1000;
    }

    /**
     * Stand-in for world provider that couldn't be found.
     */
    static class FakeDimensionProvider extends WorldProvider
    {
        FakeDimensionProvider(int dimension)
        {
            this.dimensionId = dimension;
        }

        @Override
        public String getDimensionName()
        {
            return Constants.getString("jm.common.dimension", this.dimensionId);
        }

        // New in 1.8, unused in 1.7
        public String getInternalNameSuffix()
        {
            return "fake";
        }
    }
}
