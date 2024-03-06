/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.command;

import com.mojang.authlib.GameProfile;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.LogFormatter;
import journeymap.client.model.Waypoint;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.ServerConfigurationManager;

/**
 * @author techbrew
 * @date 4/8/2014.
 */
public class CmdTeleportWaypoint
{
    final Minecraft mc = ForgeHelper.INSTANCE.getClient();
    final Waypoint waypoint;

    public CmdTeleportWaypoint(Waypoint waypoint)
    {
        this.waypoint = waypoint;
    }

    public static boolean isPermitted(Minecraft mc)
    {
        if (mc.getIntegratedServer() != null)
        {
            IntegratedServer mcServer = mc.getIntegratedServer();
            ServerConfigurationManager configurationManager = null;
            GameProfile profile = null;
            try
            {
                profile = new GameProfile(mc.thePlayer.getUniqueID(), ForgeHelper.INSTANCE.getEntityName(mc.thePlayer));
                configurationManager = mcServer.getConfigurationManager();

                // 1.7
                return configurationManager.func_152596_g(profile);

                // 1.8
                //return configurationManager.canSendCommands(profile);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                try
                {
                    if (profile != null && configurationManager != null)
                    {
                        return mcServer.isSinglePlayer()
                                && mcServer.worldServers[0].getWorldInfo().areCommandsAllowed()
                                && mcServer.getServerOwner().equalsIgnoreCase(profile.getName());
                    }
                    else
                    {
                        Journeymap.getLogger().warn("Failed to check teleport permission both ways: " + LogFormatter.toString(e) + ", and profile or configManager were null.");
                    }
                }
                catch (Exception e2)
                {
                    Journeymap.getLogger().warn("Failed to check teleport permission. Both ways failed: " + LogFormatter.toString(e) + ", and " + LogFormatter.toString(e2));
                }
            }
        }

        return true;
    }

    public void run()
    {
        mc.thePlayer.sendChatMessage(String.format("/tp %s %s %s %s", ForgeHelper.INSTANCE.getEntityName(mc.thePlayer), waypoint.getX(), waypoint.getY(), waypoint.getZ()));
    }
}
