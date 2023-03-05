/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package modinfo;

import cpw.mods.fml.common.Loader;
import modinfo.mp.v1.Client;
import modinfo.mp.v1.Message;
import modinfo.mp.v1.Payload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.Locale;
import net.minecraft.server.integrated.IntegratedServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author techbrew 2/18/14.
 */
public class ModInfo
{

    public static final String VERSION = "0.2";
    public static final Logger LOGGER = LogManager.getLogger("modinfo");

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final String trackingId;
    private final String modId;
    private final String modName;
    private final String modVersion;
    private Locale reportingLocale;
    private Config config;
    private Client client;

    public ModInfo(String trackingId, String reportingLanguageCode, String modId, String modName, String modVersion, boolean singleUse)
    {
        this.trackingId = trackingId;
        this.modId = modId;
        this.modName = modName;
        this.modVersion = modVersion;

        try
        {
            this.reportingLocale = getLocale(reportingLanguageCode);
            this.config = Config.getInstance(this.modId);
            this.client = createClient();
            if (singleUse)
            {
                singleUse();
            }
            else if (this.config.isEnabled())
            {
                if (Config.generateStatusString(modId, false).equals(config.getStatus()))
                {
                    optIn();
                }
                else
                {
                    config.confirmStatus();
                }
            }
            else
            {
                optOut();
            }
        }
        catch (Throwable t)
        {
            LOGGER.log(Level.ERROR, "Unable to configure ModInfo", t);
        }
    }

    /**
     * Adapted from UUID.nameUUIDFromBytes();
     *
     * @param parts
     * @return
     */
    static UUID createUUID(String... parts)
    {
        MessageDigest md;
        try
        {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException nsae)
        {
            throw new InternalError("MD5 not supported");
        }

        for (String part : parts)
        {
            md.update(part.getBytes());
        }

        byte[] md5Bytes = md.digest();
        md5Bytes[6] &= 0x0f;  /* clear version        */
        md5Bytes[6] |= 0x30;  /* set to version 3     */
        md5Bytes[8] &= 0x3f;  /* clear variant        */
        md5Bytes[8] |= 0x80;  /* set to IETF variant  */

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++)
        {
            msb = (msb << 8) | (md5Bytes[i] & 0xff);
        }
        for (int i = 8; i < 16; i++)
        {
            lsb = (lsb << 8) | (md5Bytes[i] & 0xff);
        }

        return new UUID(msb, lsb);
    }

    public final boolean isEnabled()
    {
        return client != null;
    }

    public void reportAppView()
    {
        try
        {
            if (isEnabled())
            {
                Payload payload = new Payload(Payload.Type.AppView);
                payload.add(appViewParams());
                payload.add(minecraftParams());
                client.send(payload);
            }
        }
        catch (Throwable t)
        {
            LOGGER.log(Level.ERROR, t.getMessage(), t);
        }
    }

    // TODO: Move to subtype of Message where this work can be done in another thread
    public void reportException(Throwable e)
    {
        try
        {
            if (isEnabled())
            {
                final String category = "Exception: " + e.toString();
                final String lineDelim = " / ";
                final int actionMaxBytes = Payload.Parameter.EventAction.getMaxBytes();
                final int labelMaxBytes = Payload.Parameter.EventLabel.getMaxBytes();
                final int maxBytes = actionMaxBytes + labelMaxBytes;

                // Get encoded lengths of the stack trace lines
                StackTraceElement[] stackTrace = e.getStackTrace();
                ArrayList<Integer> byteLengths = new ArrayList<Integer>(stackTrace.length);
                int total = 0;
                for (int i = 0; i < stackTrace.length; i++)
                {
                    int byteLength = Payload.encode(stackTrace[i].toString() + lineDelim).getBytes().length;
                    if (total + byteLength > maxBytes)
                    {
                        break;
                    }
                    total += byteLength;
                    byteLengths.add(i, byteLength);
                }

                int index = 0;

                // Put as many stack trace lines as possible into EventAction
                StringBuilder action = new StringBuilder(actionMaxBytes / 11);
                {
                    int actionTotal = 0;
                    for (; index < byteLengths.size(); index++)
                    {
                        int byteLength = byteLengths.get(index);
                        if (actionTotal + byteLength > actionMaxBytes)
                        {
                            break;
                        }

                        actionTotal += byteLength;
                        action.append(stackTrace[index].toString() + lineDelim);
                    }
                }

                // Put as many stack trace lines as possible into EventLabel
                StringBuilder label = new StringBuilder(labelMaxBytes / 11);
                {
                    int labelTotal = 0;
                    for (; index < byteLengths.size(); index++)
                    {
                        int byteLength = byteLengths.get(index);
                        if (labelTotal + byteLength > labelMaxBytes)
                        {
                            break;
                        }

                        labelTotal += byteLength;
                        label.append(stackTrace[index].toString() + lineDelim);
                    }
                }

                // Report as an event
                reportEvent(category, action.toString(), label.toString());
            }
        }
        catch (Throwable t)
        {
            LOGGER.log(Level.ERROR, t.getMessage(), t);
        }
    }

    public void reportEvent(String category, String action, String label)
    {
        try
        {
            if (isEnabled())
            {
                Payload payload = new Payload(Payload.Type.Event);
                payload.add(appViewParams());
                payload.put(Payload.Parameter.EventCategory, category);
                payload.put(Payload.Parameter.EventAction, action);
                payload.put(Payload.Parameter.EventLabel, label);
                client.send(payload);
            }
        }
        catch (Throwable t)
        {
            LOGGER.log(Level.ERROR, t.getMessage(), t);
        }
    }

    public void keepAlive()
    {
        try
        {
            if (isEnabled())
            {
                Payload payload = new Payload(Payload.Type.Event);
                payload.put(Payload.Parameter.EventCategory, "ModInfo");
                payload.put(Payload.Parameter.EventAction, "KeepAlive");
                payload.put(Payload.Parameter.NonInteractionHit, "1");
                client.send(payload);
            }
        }
        catch (Throwable t)
        {
            LOGGER.log(Level.ERROR, t.getMessage(), t);
        }
    }

    private Locale getLocale(String languageCode)
    {
        String english = "en_US";
        List<String> langs = Arrays.asList(english);
        if (!english.equals(languageCode))
        {
            langs.add(languageCode);
        }

        Locale locale = new Locale();
        locale.loadLocaleDataFiles(minecraft.getResourceManager(), langs);
        return locale;
    }

    private String I18n(String translationKey, Object... parms)
    {
        return reportingLocale.formatMessage(translationKey, parms);
    }

    private Client createClient()
    {
        String salt = config.getSalt();
        String username = minecraft.getSession().getUsername();
        UUID clientId = createUUID(salt, username, modId);
        return new Client(trackingId, clientId, config, Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode());
    }

    private Map<Payload.Parameter, String> minecraftParams()
    {
        Map<Payload.Parameter, String> map = new HashMap<Payload.Parameter, String>();
        Language language = minecraft.getLanguageManager().getCurrentLanguage();
        map.put(Payload.Parameter.UserLanguage, language.getLanguageCode());

        DisplayMode displayMode = Display.getDesktopDisplayMode();
        map.put(Payload.Parameter.ScreenResolution, displayMode.getWidth() + "x" + displayMode.getHeight());

        StringBuilder desc = new StringBuilder(Loader.MC_VERSION);
        if (minecraft.theWorld != null)
        {
            IntegratedServer server = minecraft.getIntegratedServer();
            boolean multiplayer = server == null || server.getPublic();
            desc.append(", ").append(multiplayer ? this.I18n("menu.multiplayer") : this.I18n("menu.singleplayer"));
        }

        map.put(Payload.Parameter.ContentDescription, desc.toString());
        return map;
    }

    private Map<Payload.Parameter, String> appViewParams()
    {
        Map<Payload.Parameter, String> map = new HashMap<Payload.Parameter, String>();
        map.put(Payload.Parameter.ApplicationName, modName);
        map.put(Payload.Parameter.ApplicationVersion, modVersion);
        return map;
    }

    private void optIn()
    {
        // Send opt-in message and confirm.
        Payload payload = new Payload(Payload.Type.Event);
        payload.put(Payload.Parameter.EventCategory, "ModInfo");
        payload.put(Payload.Parameter.EventAction, "Opt In");

        createClient().send(payload, new Message.Callback()
        {
            public void onResult(Object result)
            {
                if (Boolean.TRUE.equals(result))
                {
                    if (config.isEnabled())
                    {
                        config.confirmStatus();
                        LOGGER.info("ModInfo for " + config.getModId() + " has been re-enabled. Thank you!");
                    }
                }
            }
        });
    }

    public void singleUse()
    {
        if (Config.isConfirmedDisabled(config))
        {
            return;
        }
        else
        {
            reportAppView();
        }
        config.disable();
    }

    private void optOut()
    {
        if (Config.isConfirmedDisabled(config))
        {
            // Disabled and confirmed, do nothing
            LOGGER.info("ModInfo for " + this.modId + " is disabled");
        }
        else if (!config.isEnabled())
        {
            // Disabled.  Send opt-out message and confirm.
            Payload payload = new Payload(Payload.Type.Event);
            payload.put(Payload.Parameter.EventCategory, "ModInfo");
            payload.put(Payload.Parameter.EventAction, "Opt Out");

            createClient().send(payload, new Message.Callback()
            {
                public void onResult(Object result)
                {
                    if (Boolean.TRUE.equals(result))
                    {
                        if (!config.isEnabled())
                        {
                            config.confirmStatus();
                            LOGGER.info("ModInfo for " + config.getModId() + " has been disabled");
                        }
                    }
                }
            });
        }
    }
}
