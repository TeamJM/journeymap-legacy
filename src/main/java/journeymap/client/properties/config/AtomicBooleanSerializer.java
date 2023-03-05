/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.properties.config;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicBooleanSerializer implements JsonSerializer<AtomicBoolean>, JsonDeserializer<AtomicBoolean>
{

    AtomicBoolean configFormatChanged;

    public AtomicBooleanSerializer(AtomicBoolean configFormatChanged)
    {
        this.configFormatChanged = configFormatChanged;
    }

    @Override
    public JsonElement serialize(AtomicBoolean arg0, Type arg1, JsonSerializationContext arg2)
    {
        return new JsonPrimitive(arg0.get());
    }

    @Override
    public AtomicBoolean deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException
    {
        if (arg0.isJsonPrimitive())
        {
            return new AtomicBoolean(arg0.getAsBoolean());
        }
        else
        {
            configFormatChanged.set(true);
            return new AtomicBoolean(arg0.getAsJsonObject().get("value").getAsBoolean());
        }
    }
}