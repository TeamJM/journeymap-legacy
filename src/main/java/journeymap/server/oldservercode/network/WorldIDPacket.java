/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.network;

// 1.7.10

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import journeymap.common.Journeymap;
import journeymap.server.oldservercode.config.ConfigHandler;


/**
 * Created by Mysticdrew on 10/8/2014.
 */
public class WorldIDPacket implements IMessage
{

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
            if (ctx.side == Side.SERVER)
            {
                if (ConfigHandler.getConfigByWorldName(ctx.getServerHandler().playerEntity.getEntityWorld().getWorldInfo().getWorldName()).isUsingWorldID())
                {
                    String worldName = ctx.getServerHandler().playerEntity.getEntityWorld().getWorldInfo().getWorldName();
                    String worldID = ConfigHandler.getConfigByWorldName(worldName).getWorldID();
                    PacketManager.instance.sendPlayerWorldID(worldID, ctx.getServerHandler().playerEntity.getCommandSenderName());
                }
            }

            return null;
        }
    }
}
