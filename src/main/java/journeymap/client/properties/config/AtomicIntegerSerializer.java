/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties.config;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerSerializer implements JsonSerializer<AtomicInteger>, JsonDeserializer<AtomicInteger>
{

    AtomicBoolean configFormatChanged;

    public AtomicIntegerSerializer(AtomicBoolean configFormatChanged)
    {
        this.configFormatChanged = configFormatChanged;
    }

    @Override
    public JsonElement serialize(AtomicInteger arg0, Type arg1, JsonSerializationContext arg2)
    {
        return new JsonPrimitive(arg0.get());
    }

    @Override
    public AtomicInteger deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException
    {
        if (arg0.isJsonPrimitive())
        {
            return new AtomicInteger(arg0.getAsInt());
        }
        else
        {
            configFormatChanged.set(true);
            return new AtomicInteger(arg0.getAsJsonObject().get("value").getAsInt());
        }
    }
}