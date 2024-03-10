package journeymap.client.webmap.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.JourneymapClient;
import journeymap.client.model.MapState;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.webmap.enums.WebmapStatus;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

public class Status
{
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void get(Context ctx)
    {
        Map<String, Object> data = new HashMap<>();
        WebmapStatus status;
        if (Minecraft.getMinecraft().theWorld == null)
        {
            status = WebmapStatus.NO_WORLD;
        }
        else if (!JourneymapClient.getInstance().isMapping())
        {
            status = WebmapStatus.STARTING;
        }
        else
        {
            status = WebmapStatus.READY;
        }
        if (status == WebmapStatus.READY)
        {
            MapState mapState = MiniMap.state();
            data.put("mapType", mapState.getCurrentMapType().name());
            Map<String, Boolean> allowedMapTypes = new HashMap<>();
            allowedMapTypes.put("cave", mapState.isCaveMappingAllowed());
            if (allowedMapTypes.values().stream().noneMatch(Boolean::booleanValue))
            {
                status = WebmapStatus.DISABLED;
            }
            data.put("allowedMapTypes", allowedMapTypes);
        }
        ctx.contentType(ContentType.APPLICATION_JSON);
        data.put("status", status.getStatus());
        ctx.result(gson.toJson(data));
    }
}
