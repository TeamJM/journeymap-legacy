/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.chat;

/**
 * Created by Mysticdrew on 11/19/2014.
 */
public class ChatHandler
{
    private static IChatHandler handler;

    public static void init(IChatHandler handler)
    {
        ChatHandler.handler = handler;
    }

    public static void sendMessage(String player, String message)
    {
        handler.sendChatMessage(player, message);
    }
}
