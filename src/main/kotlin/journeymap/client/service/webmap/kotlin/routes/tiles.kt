package journeymap.client.service.webmap.kotlin.routes

import cpw.mods.fml.client.FMLClientHandler
import journeymap.client.JourneymapClient
import journeymap.client.data.WorldData
import journeymap.client.io.FileHandler
import journeymap.client.io.RegionImageHandler
import journeymap.client.model.MapType
import journeymap.client.render.map.Tile
import journeymap.common.Journeymap
import net.minecraft.client.Minecraft
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.World
import org.apache.logging.log4j.Logger
import org.eclipse.jetty.io.EofException
import spark.kotlin.RouteHandler
import java.awt.image.BufferedImage
import java.io.File
import java.io.OutputStream
import javax.imageio.IIOException
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.roundToInt


private val logger: Logger = Journeymap.getLogger()


internal fun tilesGet(handler: RouteHandler): Any {
    val x: Int = handler.request.queryParamOrDefault("x", "0").toInt()
    var y: Int? = handler.request.queryParamOrDefault("y", "0").toInt()
    val z: Int = handler.request.queryParamOrDefault("z", "0").toInt()

    val dimension: Int = handler.request.queryParamOrDefault("dimension", "0").toInt()
    val mapTypeString: String = handler.request.queryParamOrDefault("mapTypeString", MapType.Name.day.name)
    val zoom: Int = handler.request.queryParamOrDefault("zoom", "0").toInt()

    val minecraft: Minecraft = FMLClientHandler.instance().client
    val world: World? = minecraft.theWorld

    if (world == null) {
        logger.warn("Tiles requested before world loaded")
        handler.status(400)
        return "World not loaded"
    }

    if (!JourneymapClient.getInstance().isMapping) {
        logger.warn("Tiles requested before JourneyMap started")
        handler.status(400)
        return "JourneyMap is still starting"
    }

    val worldDir: File = FileHandler.getJMWorldDir(minecraft)

    try {
        if (!worldDir.exists() || !worldDir.isDirectory) {
            logger.warn("JM world directory not found")
            handler.status(404)
            return "World not found"
        }
    } catch (e: NullPointerException) {
        logger.warn("NPE occurred while locating JM world directory")
        handler.status(404)
        return "World not found"
    }

    val mapTypeName: MapType.Name?

    try {
        mapTypeName = MapType.Name.valueOf(mapTypeString)
    } catch (e: IllegalArgumentException) {
        logger.warn("Invalid map type supplied during tiles request: $mapTypeString")
        handler.status(400)
        return "Invalid map type: $mapTypeString"
    }

    if (mapTypeName != MapType.Name.underground) {
        y = null  // Only underground maps have elevation
    }

    if (mapTypeName == MapType.Name.underground && WorldData.isHardcoreAndMultiplayer()) {
        logger.debug("Blank tile returned for underground view on a hardcore server")
        val output: OutputStream = handler.response.raw().outputStream

        handler.response.raw().contentType = "image/png"
        output.write(RegionImageHandler.getBlank512x512ImageFile().readBytes())
        output.flush()

        return handler.response
    }

    // TODO: Test out this math with Leaflet

    val scale: Int = 2.0.pow(zoom).roundToInt()
    val distance: Int = 32 / scale

    val minChunkX: Int = x * distance
    val minChunkY: Int = z * distance

    val maxChunkX = minChunkX + distance - 1
    val maxChunkY = minChunkY + distance - 1

    val startCoord = ChunkCoordIntPair(minChunkX, minChunkY)
    val endCoord = ChunkCoordIntPair(maxChunkX, maxChunkY)

    val showGrid: Boolean = JourneymapClient.getWebMapProperties().showGrid.get()
    val mapType = MapType(mapTypeName, y, dimension)

    val img: BufferedImage = RegionImageHandler.getMergedChunks(
        worldDir, startCoord, endCoord, mapType, true, null,
        Tile.TILESIZE, Tile.TILESIZE, false, showGrid
    )

    val output: OutputStream = handler.response.raw().outputStream

    try {
        handler.response.raw().contentType = "image/png"
        ImageIO.write(img, "png", output)
        output.flush()
    } catch (e: EofException) {
        logger.info("Connection closed while writing image response. Webmap probably reloaded.")
        return ""
    } catch (e: IIOException) {
        logger.info("Connection closed while writing image response. Webmap probably reloaded.")
        return ""
    }

    // TODO: Profiling, as in the original TileService

    return handler.response
}
