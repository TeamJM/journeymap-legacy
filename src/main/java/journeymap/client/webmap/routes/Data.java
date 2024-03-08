package journeymap.client.webmap.routes;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.data.DataCache;
import journeymap.client.data.ImagesData;
import journeymap.client.model.Waypoint;
import journeymap.common.Journeymap;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = Journeymap.getLogger();
    private static final List<String> dataTypesRequiringSince = Lists.newArrayList("all", "images");

    public static void get(Context ctx)
    {
        String since = ctx.queryParam("images.since");
        String type = ctx.pathParam("type");
        if (dataTypesRequiringSince.contains(type) && since == null)
        {
            logger.warn("Data type '" + type + "' requested without 'images.since' parameter");
            ctx.status(400);
            ctx.result("Data type '" + type + "' requires 'images.since' parameter.");
            return;
        }

        long sinceTime = since == null ? 0 : Long.parseLong(since);
        Object data = null;
        switch (type)
        {
            case "all":
                data = DataCache.instance().getAll(sinceTime);
                break;
            case "animals":
                data = DataCache.instance().getAnimals(false);
                break;
            case "mobs":
                data = DataCache.instance().getMobs(false);
                break;
            case "images":
                data = new ImagesData(sinceTime);
                break;
            case "messages":
                data = DataCache.instance().getMessages(false);
                break;
            case "player":
                data = DataCache.instance().getPlayer(false);
                break;
            case "players":
                data = DataCache.instance().getPlayers(false);
                break;
            case "world":
                data = DataCache.instance().getWorld(false);
                break;
            case "villagers":
                data = DataCache.instance().getVillagers(false);
                break;
            case "waypoints":
                Collection<Waypoint> waypoints = DataCache.instance().getWaypoints(false);
                Map<String, Waypoint> wpMap = new HashMap<>();
                for (Waypoint waypoint : waypoints)
                {
                    wpMap.put(waypoint.getId(), waypoint);
                }
                data = wpMap;
                break;
            default:
                break;
        }
        if (data == null)
        {
            logger.warn("Unknown data type '" + type + "'");
            ctx.status(400);
            ctx.result("Unknown data type '" + type + "'");
        }
        ctx.contentType(ContentType.APPLICATION_JSON);
        ctx.result(GSON.toJson(data));
    }
}
