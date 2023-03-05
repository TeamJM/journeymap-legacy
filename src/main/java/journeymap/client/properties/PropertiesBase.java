/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import journeymap.client.Constants;
import journeymap.client.io.FileHandler;
import journeymap.client.log.LogFormatter;
import journeymap.client.properties.config.AtomicBooleanSerializer;
import journeymap.client.properties.config.AtomicIntegerSerializer;
import journeymap.client.properties.config.AtomicReferenceSerializer;
import journeymap.client.properties.config.ConfigValidation;
import journeymap.common.Journeymap;
import journeymap.common.version.Version;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base GSON-backed properties class.
 */
public abstract class PropertiesBase
{
    // GSON charset
    protected static final Charset UTF8 = Charset.forName("UTF-8");

    // Flag the serializers can use to signal the file format needs to be updated
    protected static transient final AtomicBoolean configFormatChanged = new AtomicBoolean(false);

    // Headers to output before file
    private static final String[] HEADERS = {
            "// " + Constants.getString("jm.config.file_header_1"),
            "// " + Constants.getString("jm.config.file_header_2", Constants.CONFIG_DIR),
            // "// " + Constants.getString("jm.config.file_header_3", Constants.DATA_DIR + File.separator + "**" + File.separator),
            // "// " + Constants.getString("jm.config.file_header_4"),
            "// " + Constants.getString("jm.config.file_header_5", "http://journeymap.info/help/wiki/Options_Manager")
    };

    // Gson for file persistence
    protected transient final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(AtomicBoolean.class, new AtomicBooleanSerializer(configFormatChanged))
            .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerSerializer(configFormatChanged))
            .registerTypeAdapter(AtomicReference.class, new AtomicReferenceSerializer(configFormatChanged))
            .create();
    // Whether it's disabled
    protected final AtomicBoolean disabled = new AtomicBoolean(false);
    // Version used to create config
    protected Version configVersion = null;
    // Current file reference
    protected transient File sourceFile = null;

    /**
     * Default constructor.
     */
    protected PropertiesBase()
    {
    }

    public static <T extends PropertiesBase> T reload(T properties, Class<T> propertiesClass)
    {
        if (properties != null)
        {
            properties.save();
        }

        T reloadedProperties = null;
        try
        {
            reloadedProperties = propertiesClass.newInstance().load();
            boolean sourceChanged = (properties == null) || properties.isWorldConfig() != reloadedProperties.isWorldConfig();
            if (sourceChanged)
            {
                Journeymap.getLogger().info("Loaded " + propertiesClass.getSimpleName() + " from " + reloadedProperties.getFile());
            }
            return reloadedProperties;
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Failed to reload " + propertiesClass.getName() + ": " + LogFormatter.toString(t));
            return (properties != null) ? properties : reloadedProperties;
        }
    }

    /**
     * Name used in property file
     *
     * @return name
     */
    public abstract String getName();

    /**
     * Gets the property file, looking first in the world config dir,
     * then falling back to look in the standard config dir.
     *
     * @return file
     */
    public File getFile()
    {
        if (sourceFile == null)
        {
            sourceFile = new File(FileHandler.getWorldConfigDir(false), getFileName());
            if (!sourceFile.canRead())
            {
                sourceFile = new File(FileHandler.StandardConfigDirectory, getFileName());
            }
        }
        return sourceFile;
    }

    /**
     * Gets the filename for the instance.
     *
     * @return
     */
    protected String getFileName()
    {
        return String.format("journeymap.%s.config", this.getName());
    }

    /**
     * Whethere the current source file is associated with a specific world.
     *
     * @return
     */
    public boolean isWorldConfig()
    {
        File worldConfigDir = FileHandler.getWorldConfigDir(false);
        return (worldConfigDir != null && worldConfigDir.equals(getFile().getParentFile()));
    }

    /**
     * Whether this config is disabled and shouldn't be used.
     *
     * @return
     */
    public boolean isDisabled()
    {
        return disabled.get();
    }

    /**
     * Set disabled - only works for world configs.
     * Saves after the set.
     *
     * @param disable
     */
    public void setDisabled(boolean disable)
    {
        if (isWorldConfig())
        {
            disabled.set(disable);
            save();
        }
        else
        {
            throw new IllegalStateException("Can't disable standard config.");
        }
    }

    /**
     * Copies standard config to world config.
     *
     * @param overwrite true if current world config should be overwritten
     * @return true if copy succeeded
     */
    public boolean copyToWorldConfig(boolean overwrite)
    {
        if (!isWorldConfig())
        {
            try
            {
                File worldConfig = getFile();
                if (overwrite || !worldConfig.exists())
                {
                    save();
                    Files.copy(sourceFile, worldConfig);
                    return worldConfig.canRead();
                }
            }
            catch (IOException e)
            {
                Journeymap.getLogger().error("Couldn't copy config to world config: " + LogFormatter.toString(e));
            }
            return false;
        }
        else
        {
            throw new IllegalStateException("Can't create World config from itself.");
        }
    }

    /**
     * Copies world config over standard config
     *
     * @return
     */
    public boolean copyToStandardConfig()
    {
        if (isWorldConfig())
        {
            try
            {
                save();
                File standardConfig = new File(FileHandler.StandardConfigDirectory, getFileName());
                Files.copy(sourceFile, standardConfig);
                return standardConfig.canRead();
            }
            catch (IOException e)
            {
                Journeymap.getLogger().error("Couldn't copy config to world config: " + LogFormatter.toString(e));
                return false;
            }
        }
        else
        {
            throw new IllegalStateException("Can't replace standard config with itself.");
        }
    }

    /**
     * Whether the file has the same config version as the current JourneyMap version
     *
     * @return true if current
     */
    public boolean isCurrent()
    {
        return Journeymap.JM_VERSION.equals(configVersion);
    }

    /**
     * Saves the property object to file
     *
     * @return true if saved
     */
    public boolean save()
    {
        synchronized (gson)
        {
            File propFile = null;
            try
            {
                // Write to file
                propFile = getFile();

                if (!propFile.exists())
                {
                    Journeymap.getLogger().info(String.format("Creating config file: %s", propFile));
                    if (!propFile.getParentFile().exists())
                    {
                        propFile.getParentFile().mkdirs();
                    }
                }
                else if (!isCurrent())
                {
                    Journeymap.getLogger().info(String.format("Updating config file from version \"%s\" to \"%s\": %s", configVersion, Journeymap.JM_VERSION, propFile));
                    configVersion = Journeymap.JM_VERSION;
                }

                // Header
                String lineEnding = System.getProperty("line.separator");
                StringBuilder sb = new StringBuilder();
                for (String line : HEADERS)
                {
                    sb.append(line).append(lineEnding);
                }
                String header = sb.toString();

                // Json body
                String json = gson.toJson(this);

                // Write to file
                Files.write(header + json, propFile, UTF8);

                Journeymap.getLogger().debug("Saved " + getFileName());

                return true;
            }
            catch (Exception e)
            {
                Journeymap.getLogger().error(String.format("Can't save config file %s: %s", propFile, LogFormatter.toString(e)));
                return false;
            }
        }
    }

    public boolean toggle(final AtomicBoolean ab)
    {
        ab.set(!ab.get());
        save();
        return ab.get();
    }

    /**
     * Returns an instance with values loaded
     * from file, or itself if the load failed.
     *
     * @param <T> properties default instance
     * @return loaded instance
     */
    public <T extends PropertiesBase> T load()
    {
        T instance = (T) this;
        File propFile = getFile();
        boolean saveNeeded = true;
        try
        {
            if (propFile.canRead())
            {
                instance = gson.fromJson(Files.toString(propFile, UTF8), (Class<T>) getClass());
                saveNeeded = !instance.isCurrent();
                if (saveNeeded)
                {
                    Journeymap.getLogger().info(String.format("Config file needs to be updated: %s", propFile.getName()));
                }
            }
            else
            {
                instance.newFileInit();
            }
        }
        catch (Exception e)
        {
            Journeymap.getLogger().error(String.format("Can't load config file %s: %s", propFile, e.getMessage()));

            try
            {
                File badPropFile = new File(propFile.getParentFile(), propFile.getName() + ".bad");
                propFile.renameTo(badPropFile);
            }
            catch (Exception e3)
            {
                Journeymap.getLogger().error(String.format("Can't rename config file %s: %s", propFile, e3.getMessage()));
            }

        }

        if (instance == null)
        {
            try
            {
                instance = (T) getClass().newInstance();
                instance.newFileInit();
                saveNeeded = true;
            }
            catch (Exception e)
            {
                // This isn't really the reason for this exception, just the root cause of the trouble.
                throw new RuntimeException("Config file corrupted.  Please fix or remove: " + propFile);
            }
        }

        if (instance != null && (instance.validate() || saveNeeded))
        {
            instance.save();
        }

        return instance;
    }

    /**
     * Override if a new file should have special configuration.
     */
    protected void newFileInit()
    {

    }

    /**
     * Should return true if save needed after validation.
     *
     * @return
     */
    protected boolean validate()
    {
        // Use annotations
        boolean saveNeeded = validateConfigs();

        // Only world configs should be disabled.
        if (!isWorldConfig() && isDisabled())
        {
            disabled.set(false);
            saveNeeded = true;
        }

        if (configFormatChanged.get())
        {
            saveNeeded = true;
            configFormatChanged.set(false);
            Journeymap.getLogger().info("File format will be updated for " + this.getFileName());
        }

        return saveNeeded;
    }

    /**
     * Use @Config annotations to validate value ranges
     *
     * @return
     */
    protected boolean validateConfigs()
    {
        return ConfigValidation.validateConfigs(this);
    }

    public void ensureValid()
    {
        validate();
        save();
    }
}
