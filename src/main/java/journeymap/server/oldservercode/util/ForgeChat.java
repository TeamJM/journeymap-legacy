/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.util;

import journeymap.common.Journeymap;
import journeymap.server.oldservercode.chat.IChatHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;


/**
 * Created by Mysticdrew on 11/19/2014.
 */
public class ForgeChat implements IChatHandler
{

    @Override
    public void sendChatMessage(String player, String message)
    {
        ChatComponentText msg = new ChatComponentText(message);
        sendCommandResponse(player, msg);

    }

    private void sendCommandResponse(String sender, ChatComponentText text)
    {
        EntityPlayerMP player = ForgePlayerUtil.instance.getPlayerEntityByName(sender);
        if (player != null)
        {
            player.addChatMessage(text);
        }
        else
        {
            Journeymap.getLogger().info(text.getChatComponentText_TextValue());
        }
    }
}
