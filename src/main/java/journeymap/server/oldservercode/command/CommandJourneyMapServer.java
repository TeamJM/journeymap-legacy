/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.command;


import journeymap.common.network.PacketHandler;
import journeymap.server.oldservercode.config.ConfigHandler;
import journeymap.server.oldservercode.config.Configuration;
import journeymap.server.oldservercode.util.TabCompletionHelper;

import java.util.List;
import java.util.UUID;

import static journeymap.server.oldservercode.chat.ChatHandler.sendMessage;

/**
 * Created by Mysticdrew on 11/19/2014.
 */
public class CommandJourneyMapServer
{


    public void processCommand(String sender, String world, String[] args)
    {
        String s = args[0].toLowerCase();
        if (s.equals("worldid"))
        {
            if (ConfigHandler.getConfigByWorldName(world).isUsingWorldID())
            {
                processWorldID(sender, world, args);
            }
            else
            {
                sendMessage(sender, "\u00a74WorldID settings are disabled.");
            }

        }
        else if (s.equals("help"))
        {
            processJMServerHelp(sender);

        }
    }

    private void processJMServerHelp(String sender)
    {
        sendMessage(sender, "\u00a7b--- JourneyMapServer Commands ---");
        sendMessage(sender, "\u00a7b--------------------------------");
        sendMessage(sender, "\u00a72worldid : Displays current World ID.\u00a74(Has Sub-Commands)");
        sendMessage(sender, "\u00a74/jmserver <command> help for more info about sub-commands");
    }

    private void processWorldID(String sender, String world, String[] args)
    {
        if (args.length > 1)
        {
            String s = args[1].toLowerCase();
            if (s.equals("help"))
            {
                processWorldIDHelp(sender);

            }
            else if (s.equals("set"))
            {
                processSetWorldID(sender, world, args[2]);

            }
            else if (s.equals("setrandom"))
            {
                processSetWorldID(sender, world, UUID.randomUUID().toString());

            }
            else if (s.equals("resync"))
            {
                sendMessage(sender, "Re-Syncing all clients!");
                PacketHandler.sendAllPlayersWorldID(world);

            }
            else
            {
                sendMessage(sender, "/jmserver worldid help|set|setrandom|resync|(blank)");
            }
        }
        else
        {
            sendMessage(sender, String.format("World ID: %s", ConfigHandler.getConfigByWorldName(world).getWorldID()));
        }
    }

    private void processSetWorldID(String sender, String world, String worldID)
    {
        Configuration config = ConfigHandler.getConfigByWorldName(world);
        config.setWorldID(worldID);
        ConfigHandler.saveWorld(config, world);
        PacketHandler.sendAllPlayersWorldID(worldID);
        sendMessage(sender, String.format("WorldID set to %s", worldID));
    }

    private void processWorldIDHelp(String sender)
    {
        sendMessage(sender, "\u00a7b------- WorldID Commands -------");
        sendMessage(sender, "\u00a7b------------------------------");
        sendMessage(sender, "\u00a72set <name> : Sets the custom WorldID");
        sendMessage(sender, "\u00a72setrandom : Sets the World ID to a random hash UUID");
        sendMessage(sender, "\u00a72resync : Re-syncs all clients with the current World ID");
    }

    public List<String> retrieveTabCompleteValues(String[] args)
    {

        if (args.length == 1)
        {
            return TabCompletionHelper.getListOfStringsMatchingLastWord(args, "help", "worldid");
        }

        if (args.length == 2 && "worldid".equalsIgnoreCase(args[0]))
        {
            return TabCompletionHelper.getListOfStringsMatchingLastWord(args, "help", "set", "setrandom", "resync");
        }
        return null;
    }
}
