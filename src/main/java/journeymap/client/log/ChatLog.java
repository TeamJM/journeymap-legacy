/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.log;

import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.webmap.WebMap;
import journeymap.common.Journeymap;
import journeymap.common.version.VersionCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides messages to both chat GUI and log.
 */
public class ChatLog
{
    // Announcements
    static final List<ChatComponentTranslation> announcements = Collections.synchronizedList(new LinkedList<ChatComponentTranslation>());
    public static boolean enableAnnounceMod = false;
    private static boolean initialized = false;

    /**
     * Announce chat component.
     *
     * @param chat
     */
    public static void queueAnnouncement(IChatComponent chat)
    {
        ChatComponentTranslation wrap = new ChatComponentTranslation("jm.common.chat_announcement", new Object[]{chat});
        announcements.add(wrap);
    }

    /**
     * Announce URL with link.
     *
     * @param message
     * @param url
     */
    public static void announceURL(String message, String url)
    {
        ChatComponentText chat = new ChatComponentText(message);
        chat.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        chat.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(url)));
        //chat.getChatStyle().setUnderlined(false);
        queueAnnouncement(chat);
    }

    /**
     * Announce file with link.
     *
     * @param message
     * @param file
     */
    public static void announceFile(String message, File file)
    {
        ChatComponentText chat = new ChatComponentText(message);
        try
        {
            chat.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getCanonicalPath()));
            chat.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(file.getCanonicalPath())));
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn("Couldn't build ClickEvent for file: " + LogFormatter.toString(e));
        }
        queueAnnouncement(chat);
    }

    /**
     * Queue an announcement to be shown in the UI.
     *
     * @param key   i18n key
     * @param parms message parms (optional)
     */
    public static void announceI18N(String key, Object... parms)
    {
        String text = Constants.getString(key, parms);
        ChatComponentText chat = new ChatComponentText(text);
        queueAnnouncement(chat);
    }

    /**
     * Queue an announcement to be shown in the UI.
     *
     * @param text
     */
    public static void announceError(String text)
    {
        ErrorChat chat = new ErrorChat(text);
        queueAnnouncement(chat);
    }


    /**
     * Show queued announcements in chat and log.
     *
     * @param mc
     */
    public static void showChatAnnouncements(Minecraft mc)
    {

        if (!initialized)
        {
            // Announce mod?
            enableAnnounceMod = JourneymapClient.getCoreProperties().announceMod.get();
            announceMod(false);

            // Check for newer version online
            VersionCheck.getVersionIsCurrent();
            initialized = true;
        }

        while (!announcements.isEmpty())
        {
            ChatComponentTranslation message = announcements.remove(0);
            if (message != null)
            {
                try
                {
                    mc.ingameGUI.getChatGUI().printChatMessage(message);
                }
                catch (Exception e)
                {
                    Journeymap.getLogger().error("Could not display announcement in chat: " + LogFormatter.toString(e));
                }
                finally
                {
                    Level logLevel = message.getFormatArgs()[0] instanceof ErrorChat ? Level.ERROR : Level.INFO;
                    Journeymap.getLogger().log(logLevel, StringUtils.stripControlCodes(message.getUnformattedTextForChat()));
                }
            }
        }
    }

    public static void announceMod(boolean forced)
    {
        if (enableAnnounceMod)
        {
            //ChatLog.announceI18N("jm.common.ready", JourneyMap.MOD_NAME); //$NON-NLS-1$
            if (JourneymapClient.getWebMapProperties().enabled.get())
            {
                try
                {
                    WebMap webServer = JourneymapClient.getInstance().getJmServer();
                    String keyName = Constants.getKeyName(Constants.KB_MAP);
                    String port = webServer.getPort() == 80 ? "" : ":" + webServer.getPort();
                    String message = Constants.getString("jm.common.webserver_and_mapgui_ready", keyName, port); //$NON-NLS-1$
                    ChatLog.announceURL(message, "http://localhost" + port); //$NON-NLS-1$
                }
                catch (Throwable t)
                {
                    Journeymap.getLogger().error("Couldn't check webserver: " + LogFormatter.toString(t));
                }
            }
            else
            {
                String keyName = Constants.getKeyName(Constants.KB_MAP); // Should be KeyCode
                ChatLog.announceI18N("jm.common.mapgui_only_ready", keyName); //$NON-NLS-1$
            }

            if (!JourneymapClient.getCoreProperties().mappingEnabled.get())
            {
                ChatLog.announceI18N("jm.common.enable_mapping_false_text");
            }
            enableAnnounceMod = false; // Only queueAnnouncement mod once per runtime
        }
    }

    /**
     * Decorator to indicate log level should be ERROR.
     */
    private static class ErrorChat extends ChatComponentText
    {

        public ErrorChat(String text)
        {
            super(text);
        }
    }

}
