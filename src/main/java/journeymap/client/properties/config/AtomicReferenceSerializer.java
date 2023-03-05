/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties.config;

import com.google.gson.*;
import journeymap.common.Journeymap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceSerializer implements JsonSerializer<AtomicReference>, JsonDeserializer<AtomicReference>
{

    AtomicBoolean configFormatChanged;

    public AtomicReferenceSerializer(AtomicBoolean configFormatChanged)
    {
        this.configFormatChanged = configFormatChanged;
    }

    @Override
    public JsonElement serialize(AtomicReference arg0, Type arg1, JsonSerializationContext arg2)
    {
        if (arg0.get() instanceof Enum)
        {
            return new JsonPrimitive(((Enum) arg0.get()).name());
        }
        return new JsonPrimitive(arg0.get().toString());
    }

    @Override
    public AtomicReference deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException
    {
        String value = null;
        if (arg0.isJsonPrimitive())
        {
            value = arg0.getAsString();
        }
        else
        {
            configFormatChanged.set(true);
            value = arg0.getAsJsonObject().get("value").getAsString();
        }

        Class<?> refClass = (Class) ((ParameterizedType) arg1).getActualTypeArguments()[0];
        if (refClass.equals(String.class))
        {
            return new AtomicReference<String>(value);
        }
        else if (Enum.class.isAssignableFrom(refClass))
        {
            try
            {
                Enum enumValue = Enum.valueOf((Class<? extends Enum>) refClass, value);
                return new AtomicReference<Enum>(enumValue);
            }
            catch (Exception t)
            {
                Journeymap.getLogger().warn("Could not get enum value for " + refClass + " using: " + value);
                Enum enumValue = (Enum) EnumSet.allOf((Class<? extends Enum>) refClass).iterator().next();
                return new AtomicReference<Enum>(enumValue);
            }
        }
        else
        {
            Journeymap.getLogger().warn("Could not get AtomicReference type for " + refClass + " using: " + value);
            return new AtomicReference(null);
        }
    }
}