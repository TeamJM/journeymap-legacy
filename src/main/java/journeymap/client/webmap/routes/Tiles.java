package journeymap.client.webmap.routes;

import cpw.mods.fml.client.FMLClientHandler;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.JourneymapClient;
import journeymap.client.data.WorldData;
import journeymap.client.io.FileHandler;
import journeymap.client.io.RegionImageHandler;
import journeymap.client.model.MapType;
import journeymap.client.render.map.Tile;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class Tiles
{
    private static final Logger logger = Journeymap.getLogger();

    public static void get(Context ctx)
    {
        int x = ctx.queryParam("x") == null ? 0 : Integer.parseInt(ctx.queryParam("x"));
        Integer y = ctx.queryParam("y") == null ? null : Integer.parseInt(ctx.queryParam("y"));
        int z = ctx.queryParam("z") == null ? 0 : Integer.parseInt(ctx.queryParam("z"));
        int dimension = ctx.queryParam("dimension") == null ? 0 : Integer.parseInt(ctx.queryParam("dimension"));
        String mapTypeString = ctx.queryParam("mapTypeString") == null ? MapType.Name.day.name() : ctx.queryParam("mapTypeString");
        int zoom = ctx.queryParam("zoom") == null ? 0 : Integer.parseInt(ctx.queryParam("zoom"));

        Minecraft minecraft = FMLClientHandler.instance().getClient();

        World world = minecraft.theWorld;
        if (world == null)
        {
            logger.warn("Tiles requested before world loaded");
            ctx.status(400);
            ctx.result("World not loaded");
        }
        if (!JourneymapClient.getInstance().isMapping())
        {
            logger.warn("Tiles requested before JourneyMap started");
            ctx.status(400);
            ctx.result("JourneyMap is still starting");
        }
        File worldDir = FileHandler.getJMWorldDir(minecraft);
        try
        {
            if (!worldDir.exists() || !worldDir.isDirectory())
            {
                logger.warn("JM world directory not found");
                ctx.status(404);
                ctx.result("World not found");
            }
        }
        catch (NullPointerException e)
        {
            logger.warn("NPE occurred while locating JM world directory");
            ctx.status(404);
            ctx.result("World not found");
        }
        MapType.Name mapTypeName = null;
        try
        {
            mapTypeName = MapType.Name.valueOf(mapTypeString);
        }
        catch (IllegalArgumentException e)
        {
            logger.warn("Invalid map type supplied during tiles request: " + mapTypeString);
            ctx.status(400);
            ctx.result("Invalid map type: " + mapTypeString);
        }
        if (mapTypeName != MapType.Name.underground)
        {
            y = null;  // Only underground maps have elevation
        }
        if (mapTypeName == MapType.Name.underground && WorldData.isHardcoreAndMultiplayer())
        {
            logger.debug("Blank tile returned for underground view on a hardcore server");

            try
            {
                OutputStream output = ctx.res.getOutputStream();
                ctx.contentType(ContentType.IMAGE_PNG);
                output.write(Files.readAllBytes(RegionImageHandler.getBlank512x512ImageFile().toPath()));
                output.flush();
            }
            catch (IOException e)
            {
                logger.info("Connection closed while writing image response. Webmap probably reloaded.");
                ctx.result("Connection closed while writing image response. Webmap probably reloaded.");
            }
            return;
        }
        int scale = (int) Math.pow(2, zoom);
        int distance = 32 / scale;
        int minChunkX = x * distance;
        int minChunkY = z * distance;
        int maxChunkX = minChunkX + distance - 1;
        int maxChunkY = minChunkY + distance - 1;
        ChunkCoordIntPair startCoord = new ChunkCoordIntPair(minChunkX, minChunkY);
        ChunkCoordIntPair endCoord = new ChunkCoordIntPair(maxChunkX, maxChunkY);
        boolean showGrid = JourneymapClient.getWebMapProperties().showGrid.get();
        MapType mapType = new MapType(mapTypeName, y, dimension);
        BufferedImage img = RegionImageHandler.getMergedChunks(
                worldDir, startCoord, endCoord, mapType, true, null,
                Tile.TILESIZE, Tile.TILESIZE, false, showGrid
        );

        try
        {
            OutputStream output = ctx.res.getOutputStream();
            ctx.contentType(ContentType.IMAGE_PNG);
            ImageIO.write(img, "png", output);
            output.flush();
        }
        catch (IOException e)
        {
            logger.info("Connection closed while writing image response. Webmap probably reloaded.");
            ctx.result("Connection closed while writing image response. Webmap probably reloaded.");
        }

    }
}
