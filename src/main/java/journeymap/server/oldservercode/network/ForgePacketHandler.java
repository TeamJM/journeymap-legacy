/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.network;

/**
 * Created by Mysticdrew on 10/8/2014.
 */

// 1.7.10

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import journeymap.common.Journeymap;
import journeymap.server.oldservercode.util.ForgePlayerUtil;
import net.minecraft.entity.player.EntityPlayerMP;
// 1.8
//import net.minecraftforge.fml.common.network.NetworkRegistry;
//import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
//import net.minecraftforge.fml.relauncher.Side;

public class ForgePacketHandler implements IPacketHandler
{

    public static final SimpleNetworkWrapper WORLD_INFO_CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(journeymap.common.network.WorldIDPacket.CHANNEL_NAME);

    public void init()
    {
        WORLD_INFO_CHANNEL.registerMessage(WorldIDPacket.WorldIdListener.class, WorldIDPacket.class, 0, Side.SERVER);
    }

    public void sendAllPlayersWorldID(String worldID)
    {
        WORLD_INFO_CHANNEL.sendToAll(new WorldIDPacket(worldID));
    }

    public void sendPlayerWorldID(String worldID, String playerName)
    {
        EntityPlayerMP player = ForgePlayerUtil.instance.getPlayerEntityByName(playerName);
        if ((player instanceof EntityPlayerMP) && (player != null))
        {
            //Journeymap.getLogger().info(playerName + " is an EntityPlayerMP attempting to send the worldId packet");
            try
            {
                WORLD_INFO_CHANNEL.sendTo(new WorldIDPacket(worldID), player);
            }
            catch (RuntimeException rte)
            {
                Journeymap.getLogger().error(playerName + " is not a real player. WorldID:" + worldID + " Error: " + rte);
            }
            catch (Exception e)
            {
                Journeymap.getLogger().error("Unknown Exception - PlayerName:" + playerName + " WorldID:" + worldID + " Exception " + e);
            }
        }
        else
        {
            //Journeymap.getLogger().info(playerName + " is not an EntityPlayerMP");
        }
    }
}
