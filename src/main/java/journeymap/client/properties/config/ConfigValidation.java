/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties.config;

import com.google.common.util.concurrent.AtomicDouble;
import journeymap.client.Constants;
import journeymap.client.log.LogFormatter;
import journeymap.client.properties.PropertiesBase;
import journeymap.client.ui.option.StringListProvider;
import journeymap.common.Journeymap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Validation helper methods
 */
public class ConfigValidation
{
    /**
     * Use annotations to validate property values.
     *
     * @param instance instance to validate
     * @return true if save needed
     */
    public static boolean validateConfigs(PropertiesBase instance)
    {
        try
        {
            boolean saveNeeded = validateConfigs(instance.getClass(), instance);
            if (saveNeeded)
            {
                Journeymap.getLogger().warn(instance.getClass().getSimpleName() + " failed validation and has been corrected.");
            }
            return saveNeeded;
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().error("Unexpected error in ConfigValidation: " + LogFormatter.toString(t));
            return false;
        }
    }

    /**
     * Use annotations to validate property values.
     *
     * @param propertiesClass class with annotations
     * @return true if save needed
     */
    private static boolean validateConfigs(Class<? extends PropertiesBase> propertiesClass, PropertiesBase instance) throws Exception
    {
        boolean saveNeeded = false;

        for (Field field : propertiesClass.getDeclaredFields())
        {
            if (field.isAnnotationPresent(Config.class))
            {
                Config config = field.getAnnotation(Config.class);
                Class<?> fieldType = field.getType();
                if (field.get(instance) == null)
                {
                    saveNeeded = true;
                    continue;
                }

                if (fieldType.equals(AtomicBoolean.class))
                {
                    // Nothing to do
                }
                else if (fieldType.equals(AtomicInteger.class))
                {
                    saveNeeded = validateInteger(config, field, instance) || saveNeeded;
                }
                else if (fieldType.equals(AtomicDouble.class))
                {
                    Journeymap.getLogger().error("Validation for AtomicDouble not implemented.");
                }
                else if (fieldType.equals(AtomicReference.class))
                {
                    if (!config.stringListProvider().equals(Config.NoStringProvider.class))
                    {
                        saveNeeded = validateString(config, field, instance) || saveNeeded;
                    }
                    else
                    {
                        saveNeeded = validateEnum(config, field, instance) || saveNeeded;
                    }
                }
            }
        }

        Class parentClass = propertiesClass.getSuperclass();
        if (PropertiesBase.class.isAssignableFrom(parentClass))
        {
            saveNeeded = validateConfigs(parentClass, instance) || saveNeeded;
        }

        return saveNeeded;
    }

    /**
     * Validate an AtomicInteger field using its @Config
     *
     * @param config   the annotation
     * @param field    the field
     * @param instance the owning instance
     */
    private static boolean validateInteger(Config config, Field field, PropertiesBase instance) throws Exception
    {
        boolean saveNeeded = false;

        if (config.minValue() == config.maxValue())
        {
            Journeymap.getLogger().warn(String.format("@Config on %s.%s has no range", instance.getClass().getSimpleName(), field.getName()));
        }
        else
        {
            int defaultValue = (int) config.defaultValue();
            boolean defaultValueUsable = true;
            if (defaultValue < config.minValue() || defaultValue > config.maxValue())
            {
                defaultValueUsable = false;
                Journeymap.getLogger().warn(String.format("@Config on %s.%s defaultValue is out of range", instance.getClass().getSimpleName(), field.getName()));
            }

            AtomicInteger property = (AtomicInteger) field.get(instance);
            int value = property.get();
            int okValue = Math.max((int) config.minValue(), Math.min((int) config.maxValue(), value));
            if (okValue != value)
            {
                if (defaultValueUsable)
                {
                    okValue = defaultValue;
                }
                warnPropertyValue(config, field, value, okValue);
                property.set(okValue);
                saveNeeded = true;
            }
        }
        return saveNeeded;
    }

    /**
     * Validate an AtomicReference<String> field using its @Config
     *
     * @param config   the annotation
     * @param field    the field
     * @param instance the owning instance
     */
    private static boolean validateString(Config config, Field field, PropertiesBase instance) throws Exception
    {
        boolean saveNeeded = false;

        AtomicReference<String> property = (AtomicReference<String>) field.get(instance);
        StringListProvider slp = config.stringListProvider().newInstance();

        if (!slp.getStrings().contains(slp.getDefaultString()))
        {
            Journeymap.getLogger().warn(String.format("@Config on %s.%s has an invalid default String: %s",
                    instance.getClass().getSimpleName(), field.getName(), slp.getDefaultString()));
        }

        String value = property.get();
        if (!slp.getStrings().contains(value))
        {
            String okValue = slp.getDefaultString();
            warnPropertyValue(config, field, value, okValue);
            property.set(okValue);
            saveNeeded = true;
        }

        return saveNeeded;
    }

    /**
     * Validate an AtomicReference<Enum> field using its @Config
     *
     * @param config   the annotation
     * @param field    the field
     * @param instance the owning instance
     */
    private static boolean validateEnum(Config config, Field field, PropertiesBase instance) throws Exception
    {
        boolean saveNeeded = false;

        AtomicReference<Enum> property = (AtomicReference<Enum>) field.get(instance);

        Enum enumValue = property.get();
        Class<? extends Enum> enumClass = property.get().getClass();
        Collection<Enum> enumSet = new ArrayList<Enum>(EnumSet.allOf(enumClass));
        Enum defaultValue = null;

        try
        {
            defaultValue = Enum.valueOf(enumClass, config.defaultEnum());
        }
        catch (Exception e)
        {
            defaultValue = enumSet.iterator().next();
            Journeymap.getLogger().warn(String.format("@Config on %s.%s has an invalid default Enum: %s",
                    instance.getClass().getSimpleName(), field.getName(), config.defaultEnum()));
        }

        Enum value = property.get();
        if (value == null || !enumSet.contains(value))
        {
            Enum okValue = defaultValue;
            warnPropertyValue(config, field, value, okValue);
            property.set(okValue);
            saveNeeded = true;
        }


        return saveNeeded;
    }

    /**
     * Warn a property's value has to be adjusted.
     */
    private static void warnPropertyValue(Config config, Field field, Object oldValue, Object newValue)
    {
        Journeymap.getLogger().warn(String.format("Property %s.%s invalid: %s . Changing to: %s", field.getDeclaringClass().getSimpleName(), field.getName(), oldValue, newValue));
    }

    /**
     * Get the property displayname from the Config
     */
    public static String getName(Config annotation)
    {
        return Constants.getString(annotation.key());
    }

    /**
     * Get the property tooltip from the Config
     */
    public static String getTooltip(Config annotation)
    {
        String tooltipKey = annotation.key() + ".tooltip";
        String tooltip = Constants.getString(tooltipKey);
        if (tooltipKey.equals(tooltip))
        {
            tooltip = null;
        }
        return tooltip;
    }

}
