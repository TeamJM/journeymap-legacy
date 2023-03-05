/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListOps;

import java.util.UUID;

/**
 * Created by Mysticdrew on 10/8/2014.
 */
public class ForgePlayerUtil implements IPlayerUtil
{

    public static ForgePlayerUtil instance = new ForgePlayerUtil();

    /**
     * Gets the player's GameProfile by their UUID.
     *
     * @param uuid {@Link UUID}
     * @return {@link com.mojang.authlib.GameProfile}
     */
    public GameProfile getPlayerInfoById(UUID uuid)
    {
        MinecraftServer server = MinecraftServer.getServer();
        // 1.8
        GameProfile gameProfile = server.getPlayerProfileCache().func_152652_a(uuid);

        // 1.8.8
        // GameProfile gameProfile = server.getPlayerProfileCache().getProfileByUUID(uuid);
        return gameProfile;
    }

    /**
     * Gets the player's GameProfile by their UUID.
     *
     * @param playerName
     * @return {@link com.mojang.authlib.GameProfile}
     */
    public GameProfile getPlayerProfileByName(String playerName)
    {
        MinecraftServer server = MinecraftServer.getServer();
        GameProfile gameProfile = server.getPlayerProfileCache().getGameProfileForUsername(playerName);
        return gameProfile;
    }

    /**
     * Checks the config manager to see if the user is an op or not.
     *
     * @param playerName
     * @return boolean if the user is an op or not
     */
    public boolean isOp(String playerName)
    {
        EntityPlayerMP player = getPlayerEntityByName(playerName);
        if (player instanceof EntityPlayerMP)
        {
            UserListOps ops = MinecraftServer.getServer().getConfigurationManager().getOppedPlayers();
            for (String name : ops.getKeys())
            {
                if (playerName.equals(name))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the EntityPlayerMP by a the Player's Name.
     *
     * @param name
     * @return {@link net.minecraft.entity.player.EntityPlayerMP}
     */
    public EntityPlayerMP getPlayerEntityByName(String name)
    {
        MinecraftServer server = MinecraftServer.getServer();
        return server.getConfigurationManager().getPlayerByUsername(name);
    }

    /**
     * Gets the EntityPlayerMP by a the Player's Name.
     *
     * @param uuid
     * @return {@link net.minecraft.entity.player.EntityPlayerMP}
     */
    public EntityPlayerMP getPlayerEntityByUUID(UUID uuid)
    {
        MinecraftServer server = MinecraftServer.getServer();
        return server.getConfigurationManager().createPlayerForUser(getPlayerInfoById(uuid));
    }
}
