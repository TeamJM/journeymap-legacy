/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.common.network;

/**
 * Created by Mysticdrew on 10/8/2014.
 */

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import journeymap.common.Journeymap;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketHandler
{

    public static final SimpleNetworkWrapper WORLD_INFO_CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(WorldIDPacket.CHANNEL_NAME);
    //public static final SimpleNetworkWrapper JM_PERMS = NetworkRegistry.INSTANCE.newSimpleChannel("jm_perms");

    public static void sendAllPlayersWorldID(String worldID)
    {
        WORLD_INFO_CHANNEL.sendToAll(new WorldIDPacket(worldID));
    }

    public static void sendPlayerWorldID(String worldID, EntityPlayerMP player)
    {

        if ((player instanceof EntityPlayerMP) && (player != null))
        {
            String playerName = player.getCommandSenderName();

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


    }

    private static int toInt(boolean val)
    {
        int intVal;
        if (val)
        {
            intVal = 1;
        }
        else
        {
            intVal = 0;
        }
        return intVal;
    }

//    public static void sendPerms(String worldName, EntityPlayerMP player) {
//        MappingOptionsHandler options = new MappingOptionsHandler(worldName);
//        JM_PERMS.sendTo(
//                new PermissionsPacket(
//                        toInt(options.disableRadar(player.getCommandSenderName())),
//                        toInt(options.disableRadar(player.getCommandSenderName())),
//                        toInt(options.disableRadar(player.getCommandSenderName())),
//                        toInt(options.disableRadar(player.getCommandSenderName())),
//                        toInt(options.disableCaveMapping(player.getCommandSenderName()))
//                ), player);
//    }

    public void init(Side side)
    {
        WORLD_INFO_CHANNEL.registerMessage(WorldIDPacket.WorldIdListener.class, WorldIDPacket.class, 0, side);
        //JM_PERMS.registerMessage(PermissionsPacket.PermissionsListener.class, PermissionsPacket.class, 0, Side.SERVER);
    }
}
