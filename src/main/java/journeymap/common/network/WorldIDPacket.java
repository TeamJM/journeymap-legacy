/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.common.network;


import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import journeymap.common.Journeymap;
import journeymap.server.oldservercode.config.ConfigHandler;
import net.minecraft.entity.player.EntityPlayerMP;
// 1.8

/**
 * Created by Mysticdrew on 10/8/2014.
 */
public class WorldIDPacket implements IMessage
{
    // Channel name
    public static final String CHANNEL_NAME = "world_info";

    private String worldID;

    public WorldIDPacket()
    {
    }

    public WorldIDPacket(String worldID)
    {
        this.worldID = worldID;
    }

    public String getWorldID()
    {
        return worldID;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        try
        {
            worldID = ByteBufUtils.readUTF8String(buf);
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error(String.format("Failed to read message: %s", t));
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        try
        {
            if (worldID != null)
            {
                ByteBufUtils.writeUTF8String(buf, worldID);
            }
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("[toBytes]Failed to read message: " + t);
        }
    }

    public static class WorldIdListener implements IMessageHandler<WorldIDPacket, IMessage>
    {
        @Override
        public IMessage onMessage(WorldIDPacket message, MessageContext ctx)
        {

            EntityPlayerMP player = null;
            if (ctx.side == Side.SERVER)
            {
                player = ctx.getServerHandler().playerEntity;
                if (ConfigHandler.getConfigByWorldName(player.getEntityWorld().getWorldInfo().getWorldName()).isUsingWorldID())
                {
                    Journeymap.proxy.handleWorldIdMessage(message.getWorldID(), player);
                }
            }
            return null;
        }
    }
}
