/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.ui.option;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AtomicDouble;
import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.cartography.RGB;
import journeymap.client.properties.PropertiesBase;
import journeymap.client.properties.config.Config;
import journeymap.client.ui.component.*;
import journeymap.common.Journeymap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Mark on 9/29/2014.
 */
public class OptionSlotFactory
{
    protected static final Charset UTF8 = Charset.forName("UTF-8");
    protected static BufferedWriter docWriter;
    protected static File docFile;
    protected static boolean generateDocs = false;

    public static List<CategorySlot> getSlots(Map<Config.Category, List<SlotMetadata>> toolbars)
    {
        HashMap<Config.Category, List<SlotMetadata>> mergedMap = new HashMap<Config.Category, List<SlotMetadata>>();

        addSlots(mergedMap, Config.Category.MiniMap1, JourneymapClient.getMiniMapProperties1());
        addSlots(mergedMap, Config.Category.MiniMap2, JourneymapClient.getMiniMapProperties2());
        addSlots(mergedMap, Config.Category.FullMap, JourneymapClient.getFullMapProperties());
        addSlots(mergedMap, Config.Category.WebMap, JourneymapClient.getWebMapProperties());
        addSlots(mergedMap, Config.Category.Waypoint, JourneymapClient.getWaypointProperties());
        addSlots(mergedMap, Config.Category.Advanced, JourneymapClient.getCoreProperties());

        List<CategorySlot> categories = new ArrayList<CategorySlot>();
        for (Map.Entry<Config.Category, List<SlotMetadata>> entry : mergedMap.entrySet())
        {
            Config.Category category = entry.getKey();
            CategorySlot categorySlot = new CategorySlot(category);
            for (SlotMetadata val : entry.getValue())
            {
                categorySlot.add(new ButtonListSlot(categorySlot).add(val));
            }

            if (toolbars.containsKey(category))
            {
                ButtonListSlot toolbarSlot = new ButtonListSlot(categorySlot);
                for (SlotMetadata toolbar : toolbars.get(category))
                {
                    toolbarSlot.add(toolbar);
                }
                categorySlot.add(toolbarSlot);
            }

            categories.add(categorySlot);
        }

        Collections.sort(categories);

        int count = 0;
        for (CategorySlot categorySlot : categories)
        {
            count += categorySlot.size();
        }

        if (generateDocs)
        {
            ensureDocFile();

            for (ScrollListPane.ISlot rootSlot : categories)
            {
                CategorySlot categorySlot = (CategorySlot) rootSlot;

                if (categorySlot.category == Config.Category.MiniMap2)
                {
                    continue;
                }
                doc(categorySlot);
                docTable(true);

                categorySlot.sort();
                for (SlotMetadata childSlot : categorySlot.getAllChildMetadata())
                {
                    doc(childSlot, categorySlot.getCategory() == Config.Category.Advanced);
                }
                docTable(false);
            }

            endDoc();
        }

        return categories;
    }

    protected static void addSlots(HashMap<Config.Category, List<SlotMetadata>> mergedMap, Config.Category inheritedCategory, PropertiesBase properties)
    {
        Class<? extends PropertiesBase> propertiesClass = properties.getClass();
        for (Map.Entry<Config.Category, List<SlotMetadata>> entry : buildSlots(null, inheritedCategory, propertiesClass, properties).entrySet())
        {
            Config.Category category = entry.getKey();
            if (category == Config.Category.Inherit)
            {
                category = inheritedCategory;
            }

            List<SlotMetadata> slotMetadataList = null;
            if (mergedMap.containsKey(category))
            {
                slotMetadataList = mergedMap.get(category);
            }
            else
            {
                slotMetadataList = new ArrayList<SlotMetadata>();
                mergedMap.put(category, slotMetadataList);
            }

            slotMetadataList.addAll(entry.getValue());
        }
    }

    protected static HashMap<Config.Category, List<SlotMetadata>> buildSlots(HashMap<Config.Category, List<SlotMetadata>> map, Config.Category inheritedCategory, Class<? extends PropertiesBase> propertiesClass, PropertiesBase properties)
    {
        if (map == null)
        {
            map = new HashMap<Config.Category, List<SlotMetadata>>();
        }
        for (Field field : propertiesClass.getDeclaredFields())
        {
            if (field.isAnnotationPresent(Config.class))
            {
                Config config = field.getAnnotation(Config.class);
                SlotMetadata slotMetadata = null;

                if (field.getType().equals(AtomicBoolean.class))
                {
                    slotMetadata = getBooleanSlotMetadata(properties, field);
                }
                else if (field.getType().equals(AtomicInteger.class))
                {
                    slotMetadata = getIntegerSlotMetadata(properties, field);
                }
                else if (field.getType().equals(AtomicDouble.class))
                {
                    slotMetadata = getDoubleSlotMetadata(properties, field);
                }
                else if (field.getType().equals(AtomicReference.class))
                {
                    if (!config.stringListProvider().equals(Config.NoStringProvider.class))
                    {
                        slotMetadata = getStringSlotMetadata(properties, field);
                    }
                    else
                    {
                        slotMetadata = getEnumSlotMetadata(properties, field);
                    }
                }

                if (slotMetadata != null)
                {
                    // Set sort order
                    slotMetadata.setOrder(config.sortOrder());

                    // Determine category
                    Config.Category category = config.category();
                    if (category == Config.Category.Inherit)
                    {
                        category = inheritedCategory;
                    }

                    List<SlotMetadata> list = map.get(category);
                    if (list == null)
                    {
                        list = new ArrayList<SlotMetadata>();
                        map.put(category, list);
                    }
                    list.add(slotMetadata);
                }
                else
                {
                    Journeymap.getLogger().warn(String.format("Unable to create config gui for %s.%s using %s", properties.getClass().getSimpleName(), field.getName(), config));
                }
            }
        }

        // Check for parent class
        Class parentClass = propertiesClass.getSuperclass();
        if (PropertiesBase.class.isAssignableFrom(parentClass))
        {
            map = buildSlots(map, inheritedCategory, (Class<? extends PropertiesBase>) parentClass, properties);
        }

        return map;
    }

    static String getName(Config annotation)
    {
        return Constants.getString(annotation.key());
    }

    static String getTooltip(Config annotation)
    {
        String tooltipKey = annotation.key() + ".tooltip";
        String tooltip = Constants.getString(tooltipKey);
        if (tooltipKey.equals(tooltip))
        {
            tooltip = null;
        }
        return tooltip;
    }

    /**
     * Create a slot for a boolean property
     *
     * @param properties
     * @param field
     * @return
     */
    static SlotMetadata<Boolean> getBooleanSlotMetadata(PropertiesBase properties, Field field)
    {
        Config annotation = field.getAnnotation(Config.class);
        try
        {
            AtomicBoolean property = (AtomicBoolean) field.get(properties);
            String name = getName(annotation);
            String tooltip = getTooltip(annotation);
            String defaultTip = Constants.getString("jm.config.default", annotation.defaultBoolean());
            boolean advanced = annotation.category() == Config.Category.Advanced;

            CheckBox button = new CheckBox(name, property, properties);
            SlotMetadata<Boolean> slotMetadata = new SlotMetadata<Boolean>(button, name, tooltip, defaultTip, annotation.defaultBoolean(), advanced);
            slotMetadata.setMasterPropertyForCategory(annotation.master());
            if (annotation.master())
            {
                button.setLabelColors(RGB.CYAN_RGB, null, null);
            }
            return slotMetadata;
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a slot for an Integer property
     *
     * @param properties
     * @param field
     * @return
     */
    static SlotMetadata<Integer> getIntegerSlotMetadata(PropertiesBase properties, Field field)
    {
        Config annotation = field.getAnnotation(Config.class);
        try
        {
            AtomicInteger property = (AtomicInteger) field.get(properties);
            String name = getName(annotation);
            String tooltip = getTooltip(annotation);
            String defaultTip = Constants.getString("jm.config.default_numeric", (int) annotation.minValue(), (int) annotation.maxValue(), (int) annotation.defaultValue());
            boolean advanced = annotation.category() == Config.Category.Advanced;

            IntSliderButton button = new IntSliderButton(properties, property, name + " : ", "", (int) annotation.minValue(), (int) annotation.maxValue(), true);
            button.setDefaultStyle(false);
            button.setDrawBackground(false);
            SlotMetadata<Integer> slotMetadata = new SlotMetadata<Integer>(button, name, tooltip, defaultTip, (int) annotation.defaultValue(), advanced);
            return slotMetadata;
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a slot for an Integer property
     *
     * @param properties
     * @param field
     * @return
     */
    static SlotMetadata<Double> getDoubleSlotMetadata(PropertiesBase properties, Field field)
    {
        Config annotation = field.getAnnotation(Config.class);
        try
        {
            AtomicDouble property = (AtomicDouble) field.get(properties);
            String name = getName(annotation);
            String tooltip = getTooltip(annotation);
            String defaultTip = Constants.getString("jm.config.default_numeric", annotation.minValue(), annotation.maxValue(), annotation.defaultValue());
            boolean advanced = annotation.category() == Config.Category.Advanced;

            DoubleSliderButton button = new DoubleSliderButton(properties, property, name + " : ", "", (double) annotation.minValue(), (double) annotation.maxValue(), true);
            button.setDefaultStyle(false);
            button.setDrawBackground(false);
            SlotMetadata<Double> slotMetadata = new SlotMetadata<Double>(button, name, tooltip, defaultTip, (double) annotation.defaultValue(), advanced);
            return slotMetadata;
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a slot for a bound list of strings property
     *
     * @param properties
     * @param field
     * @return
     */
    static SlotMetadata<String> getStringSlotMetadata(PropertiesBase properties, Field field)
    {
        Config annotation = field.getAnnotation(Config.class);
        try
        {
            AtomicReference<String> property = (AtomicReference<String>) field.get(properties);
            String name = getName(annotation);
            String tooltip = getTooltip(annotation);
            StringListProvider slp = annotation.stringListProvider().newInstance();
            boolean advanced = annotation.category() == Config.Category.Advanced;

            ListPropertyButton<String> button = null;
            String defaultTip = null;

            // Exception: LocationProperty gets its own button
            if (slp instanceof LocationFormat.IdProvider)
            {
                button = new LocationFormat.Button(properties, property);
                defaultTip = Constants.getString("jm.config.default", ((LocationFormat.Button) button).getLabel(slp.getDefaultString()));
            }
            else if (slp instanceof TimeFormat.IdProvider)
            {
                button = new TimeFormat.Button(properties, property);
                defaultTip = Constants.getString("jm.config.default", ((TimeFormat.Button) button).getLabel(slp.getDefaultString()));
            }
            else
            {
                button = new ListPropertyButton<String>(slp.getStrings(), name, properties, property);
                defaultTip = Constants.getString("jm.config.default", slp.getDefaultString());
            }
            button.setDefaultStyle(false);
            button.setDrawBackground(false);
            SlotMetadata<String> slotMetadata = new SlotMetadata<String>(button, name, tooltip, defaultTip, slp.getDefaultString(), advanced);
            slotMetadata.setValueList(slp.getStrings());
            return slotMetadata;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    static SlotMetadata<Enum> getEnumSlotMetadata(PropertiesBase properties, Field field)
    {
        Config annotation = field.getAnnotation(Config.class);
        try
        {
            AtomicReference<Enum> property = (AtomicReference<Enum>) field.get(properties);
            String name = getName(annotation);
            String tooltip = getTooltip(annotation);
            Class<? extends Enum> enumClass = property.get().getClass();
            ArrayList<Enum> enumSet = new ArrayList<Enum>(EnumSet.allOf(enumClass));
            Enum defaultEnumValue = enumSet.get(0);
            try
            {
                defaultEnumValue = Enum.valueOf(enumClass, annotation.defaultEnum());
            }
            catch (Exception e)
            {
                Journeymap.getLogger().warn("Bad enumeration value for " + name + " default: " + annotation.defaultEnum());
            }
            String defaultTip = Constants.getString("jm.config.default", defaultEnumValue);
            boolean advanced = annotation.category() == Config.Category.Advanced;

            ListPropertyButton<Enum> button = new ListPropertyButton<Enum>(enumSet, name, properties, property);
            button.setDefaultStyle(false);
            button.setDrawBackground(false);
            SlotMetadata<Enum> slotMetadata = new SlotMetadata<Enum>(button, name, tooltip, defaultTip, defaultEnumValue, advanced);
            slotMetadata.setValueList(enumSet);
            return slotMetadata;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    static void ensureDocFile()
    {
        if (docFile == null)
        {
            docFile = new File(Constants.JOURNEYMAP_DIR, "journeymap-options-wiki.txt");

            try
            {
                if (docFile.exists())
                {
                    docFile.delete();
                }
                Files.createParentDirs(docFile);

                docWriter = Files.newWriter(docFile, UTF8);
                docWriter.append(String.format("<!-- Generated %s -->", new Date()));
                docWriter.newLine();
                docWriter.append("=== Overview ===");
                docWriter.newLine();
                docWriter.append("{{version|5.0.0|page}}");
                docWriter.newLine();
                docWriter.append("This page lists all of the available options which can be configured in-game using the JourneyMap [[Options Manager]].");
                docWriter.append("(Note: All of this information can also be obtained from the tooltips within the [[Options Manager]] itself.) <br clear/> <br clear/>");
                docWriter.newLine();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    static void doc(CategorySlot categorySlot)
    {
        try
        {
            docWriter.newLine();
            docWriter.append(String.format("==%s==", categorySlot.name.replace("Preset 1", "Preset (1 and 2)")));
            docWriter.newLine();
            docWriter.append(String.format("''%s''", categorySlot.getMetadata().iterator().next().tooltip.replace("Preset 1", "Preset (1 and 2)")));
            docWriter.newLine();
            docWriter.newLine();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static void docTable(boolean start)
    {
        try
        {
            if (start)
            {
                docWriter.append("{| class=\"wikitable\" style=\"cellpadding=\"4\"");
                docWriter.newLine();
                docWriter.append("! scope=\"col\" | Option");
                docWriter.newLine();
                docWriter.append("! scope=\"col\" | Purpose");
                docWriter.newLine();
                docWriter.append("! scope=\"col\" | Range / Default Value");
                docWriter.newLine();
                docWriter.append("|-");
                docWriter.newLine();
            }
            else
            {
                docWriter.append("|}");
                docWriter.newLine();
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static void doc(SlotMetadata slotMetadata, boolean advanced)
    {
        try
        {
            String color = advanced ? "red" : "black";
            docWriter.append(String.format("| style=\"text-align:right; white-space: nowrap; font-weight:bold; padding:6px; color:%s\" | %s", color, slotMetadata.getName()));
            docWriter.newLine();
            docWriter.append(String.format("| %s ", slotMetadata.tooltip));
            if (slotMetadata.getValueList() != null)
            {
                docWriter.append(String.format("<br/><em>Choices available:</em> <code>%s</code>", Joiner.on(", ").join(slotMetadata.getValueList())));
            }
            docWriter.newLine();
            docWriter.append(String.format("| <code>%s</code>", slotMetadata.range.replace("[", "").replace("]", "").trim()));
            docWriter.newLine();
            docWriter.append("|-");
            docWriter.newLine();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static void endDoc()
    {
        try
        {
            docFile = null;
            docWriter.flush();
            docWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
