/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.util;

/**
 * Created by Mysticdrew on 11/11/2014.
 */
public class PlayerUtil
{
    private static IPlayerUtil playerUtil;

    public static void init(IPlayerUtil playerUtil)
    {
        PlayerUtil.playerUtil = playerUtil;
    }

    public static boolean isOp(String player)
    {
        return playerUtil.isOp(player);
    }
}
