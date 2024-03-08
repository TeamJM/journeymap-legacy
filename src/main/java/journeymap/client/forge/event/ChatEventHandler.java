/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.forge.event;

import com.google.common.base.Strings;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.data.DataCache;
import journeymap.client.feature.FeatureManager;
import journeymap.client.log.LogFormatter;
import journeymap.client.ui.UIManager;
import journeymap.common.Journeymap;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author techbrew 1/29/14.
 */
@SideOnly(Side.CLIENT)
public class ChatEventHandler implements EventHandlerManager.EventHandler
{

    Set<String> featureControlCodes = FeatureManager.instance().getControlCodes();

    @Override
    public EnumSet<EventHandlerManager.BusType> getBus()
    {
        return EnumSet.of(EventHandlerManager.BusType.MinecraftForgeBus);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void invoke(ClientChatReceivedEvent event)
    {
        if (event.message != null)
        {
            try
            {
                String text = event.message.getFormattedText();
                if (!Strings.isNullOrEmpty(text))
                {
                    checkForControlCode(text.replaceAll(EnumChatFormatting.RESET.toString(), ""));
                }
            }
            catch (Exception e)
            {
                Journeymap.getLogger().warn("Unexpected exception on ClientChatReceivedEvent: " + LogFormatter.toString(e));
            }
        }
    }

    private void checkForControlCode(String text)
    {
        if (text.contains("\u00a7"))
        {
            boolean resetRequired = false;
            for (String code : featureControlCodes)
            {
                if (text.contains(code))
                {
                    FeatureManager.instance().handleControlCode(code);
                    resetRequired = true;
                }
            }
            if (resetRequired)
            {
                DataCache.instance().purge();
                UIManager.getInstance().reset();
            }
        }
    }
}
