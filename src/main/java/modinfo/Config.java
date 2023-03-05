/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package modinfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.util.UUID;

/**
 * @author techbrew 2/20/14.
 */
public class Config implements Serializable
{

    private static final String[] HEADERS = {
            "// ModInfo v%s - Configuration file for %s",
            "// ModInfo is a simple utility which helps the Mod developer support their mod.",
            "// For more information: https://github.com/MCModInfo/modinfo/blob/master/README.md"
    };
    private static final String PARENT_DIR = "config";
    private static final String FILE_PATTERN = "%s_ModInfo.cfg";
    private static final String ENABLED_STATUS_PATTERN = "Enabled (%s)";
    private static final String DISABLED_STATUS_PATTERN = "Disabled (%s)";

    private String modId;
    private Boolean enable;
    private String salt;
    private String status;
    private Boolean verbose;

    private Config()
    {
    }

    public static synchronized Config getInstance(String modId)
    {
        Config config = null;
        File configFile = getFile(modId);
        if (configFile.exists())
        {
            try
            {
                Gson gson = new Gson();
                config = gson.fromJson(new FileReader(configFile), Config.class);
            }
            catch (Exception e)
            {
                ModInfo.LOGGER.log(Level.ERROR, "Can't read file " + configFile, e.getMessage());
                if (configFile.exists())
                {
                    configFile.delete();
                }
            }
        }

        if (config == null)
        {
            config = new Config();
        }

        config.validate(modId);

        return config;
    }

    static boolean isConfirmedDisabled(Config config)
    {
        return config.enable == false && generateStatusString(config).equals(config.status);
    }

    static String generateStatusString(Config config)
    {
        return generateStatusString(config.modId, config.enable);
    }

    static String generateStatusString(String modId, Boolean enable)
    {
        UUID uuid = ModInfo.createUUID(modId, enable.toString());
        String pattern = enable ? ENABLED_STATUS_PATTERN : DISABLED_STATUS_PATTERN;
        return String.format(pattern, uuid.toString());
    }

    private static File getFile(String modId)
    {
        Minecraft minecraft = Minecraft.getMinecraft();
        File dir = new File(minecraft.mcDataDir, PARENT_DIR);
        if (!dir.exists())
        {
            dir.mkdirs();
        }
        return new File(dir, String.format(FILE_PATTERN, modId.replaceAll("%", "_")));
    }

    private void validate(String modId)
    {
        boolean dirty = false;

        if (!modId.equals(this.modId))
        {
            this.modId = modId;
            dirty = true;
        }

        if (enable == null)
        {
            this.enable = Boolean.TRUE;
            dirty = true;
        }

        if (salt == null)
        {
            salt = Long.toHexString(System.currentTimeMillis());
            dirty = true;
        }

        if (verbose == null)
        {
            this.verbose = Boolean.FALSE;
            dirty = true;
        }

        if (dirty)
        {
            save();
        }
    }

    public void save()
    {
        File configFile = getFile(modId);
        try
        {
            // Header
            String lineEnding = System.getProperty("line.separator");
            StringBuilder sb = new StringBuilder();
            for (String line : HEADERS)
            {
                sb.append(line).append(lineEnding);
            }
            String header = String.format(sb.toString(), ModInfo.VERSION, this.modId);

            // Json body
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(this);

            // Write to file
            FileWriter fw = new FileWriter(configFile);
            fw.write(header);
            fw.write(json);
            fw.flush();
            fw.close();

        }
        catch (IOException e)
        {
            ModInfo.LOGGER.log(Level.ERROR, "Can't save file " + configFile, e);
        }
    }

    public String getSalt()
    {
        return salt;
    }

    public String getModId()
    {
        return modId;
    }

    public Boolean isEnabled()
    {
        return enable;
    }

    public Boolean isVerbose()
    {
        return verbose;
    }

    public String getStatus()
    {
        return status;
    }

    void disable()
    {
        this.enable = false;
        confirmStatus();
    }

    public void confirmStatus()
    {
        String newStatus = generateStatusString(this);
        if (!newStatus.equals(status))
        {
            status = newStatus;
            save();
        }
    }

}
