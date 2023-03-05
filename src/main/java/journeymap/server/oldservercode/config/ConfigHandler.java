/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.config;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import journeymap.common.Journeymap;
import journeymap.server.oldservercode.reference.Controller;
import journeymap.server.oldservercode.util.FileManager;

import java.io.File;
import java.util.UUID;


/**
 * Created by Mysticdrew on 10/8/2014.
 */
public class ConfigHandler
{
    private static final float CONFIG_VERSION = 1.12F;
    private static File configPath;

    public static void init(File configPath)
    {
        ConfigHandler.configPath = configPath;
    }

    /**
     * returns a config based on the world file, if the config file does not exists
     * this method starts the process of generating a new one.
     * If a new config is generated, it will return a new config with default values.
     * If the config exists, it will return it.
     *
     * @param worldName
     * @return
     */
    public static Configuration getConfigByWorldName(String worldName)
    {
        worldName = parseWorldName(worldName);
        Configuration config = loadConfig(worldName);
        if (config != null)
        {
            validateConfigVersion(config, worldName);
            return config;
        }
        return addNewWorldConfig(worldName);
    }

    /*
     * This method initiates the generation of a new config with default
     * values, and saves sends it to be saved.
     * @param worldName
     * @return
     */
    private static Configuration addNewWorldConfig(String worldName)
    {
        worldName = parseWorldName(worldName);
        Journeymap.getLogger().info("Attempting to create new config file for: " + worldName);
        Configuration config = generateDefaultConfig();
        saveWorld(config, worldName);
        return config;
    }

    /*
     * This method generates a new config file with default values.
     * This method does not do the saving.
     * @return fully qualified config with default values.
     */
    private static Configuration generateDefaultConfig()
    {
        Configuration config = new Configuration();
        config.setConfigVersion(CONFIG_VERSION);
        config.setWorldID(UUID.randomUUID().toString());
        config.getRadar().setOpRadar(true);
        config.getRadar().setPlayerRadar(true);
        config.getRadar().setWhiteListRadar("");
        config.getCaveMapping().setOpCaveMapping(true);
        config.getCaveMapping().setPlayerCaveMapping(true);
        config.getCaveMapping().setWhiteListCaveMapping("");
        if (Controller.FORGE.equals(Controller.getController()))
        {
            config.setUsingWorldID(false);
        }
        else
        {
            config.setUsingWorldID(true);
        }
        return config;
    }

    /*
     * This method is for config version validation, I will use this method to add new values
     * to the config as they are needed. I will keep track of this by the version.
     * @param config
     * @param worldName
     */
    private static void validateConfigVersion(Configuration config, String worldName)
    {
        worldName = parseWorldName(worldName);
        float version = config.getConfigVersion();
        if (version != CONFIG_VERSION)
        {
            if (version < 1.1F)
            {
                config.setSaveInWorldFolder(false);
            }

            if (version <= 1.11F && Controller.FORGE.equals(Controller.getController()))
            {
                config.setUsingWorldID(false);
            }

            if (version <= 1.12F && Controller.FORGE.equals(Controller.getController()))
            {
                if (!config.isSaveInWorldFolder())
                {
                    config.setUsingWorldID(true);
                    config.setWorldID(config.getWorldID());
                }
            }

            if (version < CONFIG_VERSION)
            {
                config.setConfigVersion(CONFIG_VERSION);
            }
            saveWorld(config, worldName);
        }
        else
        {
            return;
        }
    }

    /**
     * Saves the world fine in GSON format.
     * The try block tries the bukkit location of GSON, it it does not exist,
     * the catch block uses the forge location of GSON.
     *
     * @param configuration
     * @param worldName
     * @return boolean value if save was successful
     */
    public static boolean saveWorld(Configuration configuration, String worldName)
    {
        String gsonFile;
        worldName = parseWorldName(worldName);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gsonFile = gson.toJson(configuration);


        File config = new File(configPath, String.format("%s.cfg", worldName));
        if (configPath.exists() && configPath.isDirectory())
        {
        }
        else
        {
            configPath.mkdirs();
        }
        return FileManager.writeFile(config, gsonFile);
    }

    /*
     * First attempts to load the file with the bukkit location of GSON.
     * If that fails it loads it with forge's location of GSON.
     * @param worldName
     * @return {@link net.techbrew.journeymapserver.common.config.Configuration}
     */
    private static Configuration loadConfig(String worldName)
    {
        worldName = parseWorldName(worldName);
        Configuration config;
        File configFile = new File(configPath, String.format("%s.cfg", worldName));

        try
        {
            Gson gson = new Gson();
            config = gson.fromJson(FileManager.readFile(configFile), Configuration.class);
        }
        catch (NoClassDefFoundError nce)
        {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            config = gson.fromJson(FileManager.readFile(configFile), Configuration.class);
        }
        return config;
    }

    private static String parseWorldName(String worldName)
    {
        String[] name = worldName.split("/");
        return name[name.length - 1];
    }
}
