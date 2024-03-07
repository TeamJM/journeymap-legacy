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
        // GameProfile gameProfile = server.getPlayerProfileCache().getProfileByUUID(uuid);
        GameProfile gameProfile = server.func_152358_ax().func_152652_a(uuid);
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
        // server.getPlayerProfileCache().getGameProfileForUsername(playerName);
        GameProfile gameProfile = server.func_152358_ax().func_152655_a(playerName);
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
            // MinecraftServer.getServer().getConfigurationManager().getOppedPlayers();
            UserListOps ops = MinecraftServer.getServer().getConfigurationManager().func_152603_m();
            // for (String name : ops.getKeys())
            for (String name : ops.func_152685_a())
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
        // return server.getConfigurationManager().getPlayerByUsername(name);
        return server.getConfigurationManager().func_152612_a(name);
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
