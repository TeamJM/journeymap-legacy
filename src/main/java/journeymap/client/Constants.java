/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client;


import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.LogFormatter;
import journeymap.common.Journeymap;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

// 1.8
//import net.minecraftforge.fml.common.Loader;
//import net.minecraftforge.fml.common.ModContainer;

/**
 * Constants and Keybindings... and other stuff that are squatting here for some reason.
 * TODO: The localization stuff should probably be moved, or possibly removed altogether.
 */
public class Constants
{
    public static final Ordering<String> CASE_INSENSITIVE_NULL_SAFE_ORDER = Ordering.from(String.CASE_INSENSITIVE_ORDER).nullsLast(); // or nullsFirst()
    public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private static final Joiner path = Joiner.on(File.separator).useForNull("");
    private static final String END = null;
    public static String JOURNEYMAP_DIR_LEGACY = "journeyMap";
    public static String JOURNEYMAP_DIR_BACKUP = "journeymap_bak";
    public static String JOURNEYMAP_DIR = "journeymap";
    public static String CONFIG_DIR_LEGACY = path.join(JOURNEYMAP_DIR, "config");
    public static String CONFIG_DIR = path.join(JOURNEYMAP_DIR, "config", Journeymap.JM_VERSION.toMajorMinorString(), END);
    public static String CACHE_DIR = path.join(JOURNEYMAP_DIR, "cache", END);
    public static String DATA_DIR = path.join(JOURNEYMAP_DIR, "data");
    public static String SP_DATA_DIR = path.join(DATA_DIR, WorldType.sp, END);
    public static String MP_DATA_DIR = path.join(DATA_DIR, WorldType.mp, END);
    public static String RESOURCE_PACKS_DEFAULT = "Default";
    public static String CONTROL_KEYNAME_COMBO;
    public static String KEYBINDING_CATEGORY;
    public static KeyBinding KB_MAP;
    public static KeyBinding KB_MAP_ZOOMIN;
    public static KeyBinding KB_MAP_ZOOMOUT;
    public static KeyBinding KB_MAP_SWITCH_TYPE;
    public static KeyBinding KB_MINIMAP_PRESET;
    public static KeyBinding KB_WAYPOINT;
    private static String ICON_DIR = path.join(JOURNEYMAP_DIR, "icon");
    public static String ENTITY_ICON_DIR = path.join(ICON_DIR, "entity", END);
    public static String WAYPOINT_ICON_DIR = path.join(ICON_DIR, "waypoint", END);
    public static String THEME_ICON_DIR = path.join(ICON_DIR, "theme", END);

    public static String WEB_DIR = path.join(JOURNEYMAP_DIR, "web", END);

    // Network Channel IDs

    /**
     * Initialize the keybindings, return them as a list.
     *
     * @return
     */
    public static List<KeyBinding> initKeybindings()
    {
        CONTROL_KEYNAME_COMBO = "Ctrl,";
        KEYBINDING_CATEGORY = Constants.getString("jm.common.hotkeys_keybinding_category", CONTROL_KEYNAME_COMBO);
        KB_MAP = new KeyBinding("key.journeymap.map_toggle", Keyboard.KEY_J, KEYBINDING_CATEGORY);
        KB_MAP_ZOOMIN = new KeyBinding("key.journeymap.zoom_in", Keyboard.KEY_EQUALS, KEYBINDING_CATEGORY);
        KB_MAP_ZOOMOUT = new KeyBinding("key.journeymap.zoom_out", Keyboard.KEY_MINUS, KEYBINDING_CATEGORY);
        KB_MAP_SWITCH_TYPE = new KeyBinding("key.journeymap.minimap_type", Keyboard.KEY_LBRACKET, KEYBINDING_CATEGORY);
        KB_MINIMAP_PRESET = new KeyBinding("key.journeymap.minimap_preset", Keyboard.KEY_BACKSLASH, KEYBINDING_CATEGORY);
        KB_WAYPOINT = new KeyBinding("key.journeymap.create_waypoint", Keyboard.KEY_B, KEYBINDING_CATEGORY);
        return Arrays.asList(KB_MAP, KB_MAP_ZOOMIN, KB_MAP_ZOOMOUT, KB_MAP_SWITCH_TYPE, KB_MINIMAP_PRESET, KB_WAYPOINT);
    }

    /**
     * Get the current locale
     *
     * @return
     */
    public static Locale getLocale()
    {
        Locale locale = Locale.getDefault();
        try
        {
            String lang = ForgeHelper.INSTANCE.getClient().getLanguageManager().getCurrentLanguage().getLanguageCode();
            locale = new Locale(lang);
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn("Couldn't determine locale from game settings, defaulting to " + locale);
        }
        return locale;
    }

    /**
     * Get the localized string for a given key.
     *
     * @param key
     * @return
     */
    public static String getString(String key)
    {
        String result = I18n.format(key);
        if (result.equals(key))
        {
            Journeymap.getLogger().warn("Message key not found: " + key);
        }
        return result;
    }

    /**
     * Get the localized string for a key and parameters.
     *
     * @param key
     * @param params
     * @return
     */
    public static String getString(String key, Object... params)
    {
        String result = I18n.format(key, params);
        if (result.equals(key))
        {
            Journeymap.getLogger().warn("Message key not found: " + key);
        }
        return result;
    }

    /**
     * Get the key name for a binding.
     *
     * @param keyBinding
     * @return
     */
    public static String getKeyName(KeyBinding keyBinding)
    {
        return Keyboard.getKeyName(getKeyCode(keyBinding));
    }

    /**
     * Get the keycode for a binding.
     *
     * @param keyBinding
     * @return
     */
    private static int getKeyCode(KeyBinding keyBinding)
    {
        return keyBinding.getKeyCode();
    }

    /**
     * Whether a keybinding is pressed.
     *
     * @param keyBinding
     * @return
     */
    public static boolean isPressed(KeyBinding keyBinding)
    {
        try
        {
            if (keyBinding.getKeyCode() == 0)
            {
                return false;
            }
            return keyBinding.isPressed() || Keyboard.isKeyDown(getKeyCode(keyBinding));
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().warn("Error checking whether keybinding.isPressed(): " + t);
            return false;
        }
    }

    /**
     * Safely check two strings for case-insensitive equality.
     *
     * @param first
     * @param second
     * @return
     */
    public static boolean safeEqual(String first, String second)
    {
        int result = CASE_INSENSITIVE_NULL_SAFE_ORDER.compare(first, second);
        if (result != 0)
        {
            return false;
        }
        return CASE_INSENSITIVE_NULL_SAFE_ORDER.compare(first, second) == 0;
    }

    /**
     * Get a list of all resource pack names.
     * TODO:  Why did this end up here?
     *
     * @return
     */
    public static String getResourcePackNames()
    {
        ArrayList<ResourcePackRepository.Entry> entries = new ArrayList<ResourcePackRepository.Entry>();

        try
        {
            ResourcePackRepository resourcepackrepository = ForgeHelper.INSTANCE.getClient().getResourcePackRepository();
            entries.addAll(resourcepackrepository.getRepositoryEntries());
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error(String.format("Can't get resource pack names: %s", LogFormatter.toString(t)));
        }

        String packs;
        if (entries.isEmpty())
        {
            packs = RESOURCE_PACKS_DEFAULT;
        }
        else
        {
            ArrayList<String> entryStrings = new ArrayList<String>(entries.size());
            for (ResourcePackRepository.Entry entry : entries)
            {
                entryStrings.add(entry.toString());
            }
            Collections.sort(entryStrings);
            packs = Joiner.on(", ").join(entryStrings);
        }
        return packs;
    }

    /**
     * Get a list of all loaded mod names.
     * TODO:  Why did this end up here?
     *
     * @return
     */
    public static String getModNames()
    {
        ArrayList<String> list = new ArrayList<String>();
        for (ModContainer mod : Loader.instance().getModList())
        {
            if (Loader.isModLoaded(mod.getModId()))
            {
                list.add(String.format("%s:%s", mod.getName(), mod.getVersion()));
            }
        }
        Collections.sort(list);
        return Joiner.on(", ").join(list);
    }

    public enum WorldType
    {
        mp, sp
    }


}
