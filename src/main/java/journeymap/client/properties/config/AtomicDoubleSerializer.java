/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties.config;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicDoubleSerializer implements JsonSerializer<AtomicDouble>, JsonDeserializer<AtomicDouble>
{
    AtomicBoolean configFormatChanged;

    public AtomicDoubleSerializer(AtomicBoolean configFormatChanged)
    {
        this.configFormatChanged = configFormatChanged;
    }

    @Override
    public JsonElement serialize(AtomicDouble arg0, Type arg1, JsonSerializationContext arg2)
    {
        return new JsonPrimitive(arg0.get());
    }

    @Override
    public AtomicDouble deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException
    {
        if (arg0.isJsonPrimitive())
        {
            return new AtomicDouble(arg0.getAsDouble());
        }
        else
        {
            configFormatChanged.set(true);
            return new AtomicDouble(arg0.getAsJsonObject().get("value").getAsDouble());
        }
    }
}
