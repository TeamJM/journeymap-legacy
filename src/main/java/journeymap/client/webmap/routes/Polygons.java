package journeymap.client.webmap.routes;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.http.ContentType;
import io.javalin.http.Context;

import java.util.List;

public class Polygons
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void get(Context ctx)
    {
        List<String> data = Lists.newArrayList();
        ctx.contentType(ContentType.JSON);
        ctx.result(GSON.toJson(data));
    }
}
