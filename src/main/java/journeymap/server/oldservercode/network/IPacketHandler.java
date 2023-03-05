/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.network;

/**
 * Created by Mysticdrew on 11/12/2014.
 */
public interface IPacketHandler
{
    public void init();

    public void sendAllPlayersWorldID(String worldID);

    public void sendPlayerWorldID(String worldID, String playerName);
}
