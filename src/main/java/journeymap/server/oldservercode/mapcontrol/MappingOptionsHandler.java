/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.mapcontrol;


import journeymap.server.oldservercode.config.ConfigHandler;
import journeymap.server.oldservercode.config.Configuration;
import journeymap.server.oldservercode.util.PlayerUtil;

/**
 * Created by Mysticdrew on 10/16/2014.
 */
public class MappingOptionsHandler
{

    private Configuration config;

    public MappingOptionsHandler(String worldName)
    {
        config = ConfigHandler.getConfigByWorldName(worldName);
    }

    public boolean disableRadar(String player)
    {
        if (config.getRadar().isPlayerRadar())
        {
            return false;
        }
        else if (PlayerUtil.isOp(player) && config.getRadar().isOpRadar())
        {
            return false;
        }
        else if (config.getRadar().getWhiteListRadar() != null)
        {
            if (isUserInWhiteList(config.getRadar().getWhiteListRadar(), player))
            {
                return false;
            }
        }
        return true;
    }

    public boolean disableCaveMapping(String player)
    {
        if (config.getCaveMapping().isPlayerCaveMapping())
        {
            return false;
        }
        else if (PlayerUtil.isOp(player) && config.getCaveMapping().isOpCaveMapping())
        {
            return false;
        }
        else if (config.getCaveMapping().getWhiteListCaveMapping() != null)
        {
            if (isUserInWhiteList(config.getCaveMapping().getWhiteListCaveMapping(), player))
            {
                return false;
            }
        }
        return true;
    }

    private boolean isUserInWhiteList(String userList, String player)
    {
        userList = userList.replace(" ", "");
        if (userList.contains(","))
        {
            String[] whiteListedUsers = userList.split(",");

            for (int i = 0; i < whiteListedUsers.length; i++)
            {
                if (whiteListedUsers[i].equalsIgnoreCase(player))
                {
                    return true;
                }
            }
        }
        else if (userList.equalsIgnoreCase(player))
        {
            return true;
        }
        return false;
    }
}
