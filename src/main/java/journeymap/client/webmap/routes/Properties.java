package journeymap.client.webmap.routes;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.JourneymapClient;
import journeymap.client.properties.WebMapProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Properties
{
    private static WebMapProperties webMapProperties = JourneymapClient.getWebMapProperties();
    private static Map<String, AtomicBoolean> propertiesMap = null;

    public static void get(Context ctx)
    {
        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result(JourneymapClient.getWebMapProperties().toJsonString());
    }

    public static void post(Context ctx)
    {
        if (propertiesMap == null || propertiesMap.isEmpty())
        {
            WebMapProperties properties = JourneymapClient.getWebMapProperties();
            Map<String, AtomicBoolean> propMap = new HashMap<>();
            propMap.put("showGrid", properties.showGrid);
            propMap.put("showSelf", properties.showSelf);
            propMap.put("showWaypoints", properties.showWaypoints);
            propertiesMap = propMap;
        }
        for (String key : ctx.queryParamMap().keySet())
        {
            if (propertiesMap.containsKey(key))
            {
                AtomicBoolean property = propertiesMap.get(key);
                if (property == null)
                {
                    throw new NullPointerException("Properties value for " + key + " is null");
                }
                property.set(Boolean.parseBoolean(ctx.queryParam(key)));
            }
        }
        webMapProperties.save();
    }
}
