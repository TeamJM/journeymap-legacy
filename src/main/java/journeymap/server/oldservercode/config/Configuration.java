/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.config;


import journeymap.server.nbt.WorldNbtIDSaveHandler;

/**
 * Created by Mysticdrew on 11/11/2014.
 */
public class Configuration
{

    private String WorldID;
    private boolean UseWorldID;
    private boolean SaveInWorldFolder;
    private CaveMapping cave;
    private Radar radar;
    private float ConfigVersion;


    public Configuration()
    {
        this.cave = new CaveMapping();
        this.radar = new Radar();
    }

    public boolean isSaveInWorldFolder()
    {
        return SaveInWorldFolder;
    }

    public void setSaveInWorldFolder(boolean saveInWorldFolder)
    {
        SaveInWorldFolder = saveInWorldFolder;
    }

    public Radar getRadar()
    {
        return this.radar;
    }

    public CaveMapping getCaveMapping()
    {
        return this.cave;
    }

    public boolean isUsingWorldID()
    {
        return UseWorldID;
    }

    public void setUsingWorldID(boolean useWorldID)
    {
        UseWorldID = useWorldID;
    }

    public String getWorldID()
    {
        if (this.SaveInWorldFolder)
        {
            WorldNbtIDSaveHandler worldSaveHandler = new WorldNbtIDSaveHandler();
            return worldSaveHandler.getWorldID();
        }
        return WorldID;
    }

    public void setWorldID(String worldID)
    {
        if (this.SaveInWorldFolder)
        {
            WorldNbtIDSaveHandler worldSaveHandler = new WorldNbtIDSaveHandler();
            worldSaveHandler.setWorldID(worldID);
            WorldID = null;
            return;
        }
        WorldID = worldID;
    }

    public float getConfigVersion()
    {
        return ConfigVersion;
    }

    public void setConfigVersion(float configVersion)
    {
        this.ConfigVersion = configVersion;
    }

    public static class CaveMapping
    {
        private boolean PlayerCaveMapping;
        private boolean OpCaveMapping;
        private String WhiteListCaveMapping;

        public boolean isPlayerCaveMapping()
        {
            return PlayerCaveMapping;
        }

        public void setPlayerCaveMapping(boolean playerCaveMapping)
        {
            PlayerCaveMapping = playerCaveMapping;
        }

        public boolean isOpCaveMapping()
        {
            return OpCaveMapping;
        }

        public void setOpCaveMapping(boolean opCaveMapping)
        {
            OpCaveMapping = opCaveMapping;
        }

        public String getWhiteListCaveMapping()
        {
            return WhiteListCaveMapping;
        }

        public void setWhiteListCaveMapping(String whiteListCaveMapping)
        {
            WhiteListCaveMapping = whiteListCaveMapping;
        }
    }

    public static class Radar
    {
        private boolean PlayerRadar;
        private boolean OpRadar;
        private String WhiteListRadar;

        public boolean isPlayerRadar()
        {
            return PlayerRadar;
        }

        public void setPlayerRadar(boolean playerRadar)
        {
            PlayerRadar = playerRadar;
        }

        public boolean isOpRadar()
        {
            return OpRadar;
        }

        public void setOpRadar(boolean opRadar)
        {
            OpRadar = opRadar;
        }

        public String getWhiteListRadar()
        {
            return WhiteListRadar;
        }

        public void setWhiteListRadar(String whiteListRadar)
        {
            WhiteListRadar = whiteListRadar;
        }
    }
}
