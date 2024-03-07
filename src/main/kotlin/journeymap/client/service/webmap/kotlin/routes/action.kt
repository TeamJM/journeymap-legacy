package journeymap.client.service.webmap.kotlin.routes

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import cpw.mods.fml.client.FMLClientHandler
import journeymap.client.JourneymapClient
import journeymap.client.io.FileHandler
import journeymap.client.io.MapSaver
import journeymap.client.model.MapType
import journeymap.client.task.multi.MapRegionTask
import journeymap.client.task.multi.SaveMapTask
import journeymap.common.Journeymap
import net.minecraft.client.Minecraft
import net.minecraft.world.World
import org.apache.logging.log4j.Logger
import spark.kotlin.RouteHandler
import java.io.File


private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
private val logger: Logger = Journeymap.getLogger()


internal fun actionGet(handler: RouteHandler): Any
{
    val minecraft: Minecraft = FMLClientHandler.instance().client
    val world: World? = minecraft.theWorld

    if (world == null)
    {
        logger.warn("Action requested before world loaded")
        handler.status(400)
        return "World not loaded" // TODO: Handle world being unloaded
    }

    if (!JourneymapClient.getInstance().isMapping)
    {
        logger.warn("Action requested before Journeymap started")
        handler.status(400)
        return "JourneyMap is still starting" // TODO: Handle JM not being started
    }

    val type = handler.params("type")

    return when (type)
    {
        "automap" -> autoMap(handler, minecraft, world)
        "savemap" -> saveMap(handler, minecraft, world)

        else      ->
        {
            logger.warn("Unknown action type '$type'")
            handler.status(400)

            "Unknown action type '$type'"
        }
    }
}

internal fun saveMap(handler: RouteHandler, minecraft: Minecraft, world: World): Any
{
    val worldDir: File = FileHandler.getJMWorldDir(minecraft)

    if (!worldDir.exists() || !worldDir.isDirectory)
    {
        logger.warn("JM world directory not found")
        handler.status(500)
        return "Unable to find JourneyMap world directory"
    }

    val dimension: Int = handler.request.queryParamOrDefault("dim", "0").toInt()
    val mapTypeString: String = handler.request.queryParamOrDefault("mapType", MapType.Name.day.name)

    var vSlice: Int? = handler.queryMap("depth").integerValue()
    val mapTypeName: MapType.Name

    try
    {
        mapTypeName = MapType.Name.valueOf(mapTypeString)
    }
    catch (e: IllegalArgumentException)
    {
        logger.warn("Invalid map type '$mapTypeString'")
        handler.status(400)
        return "Invalid map type '$mapTypeString'"
    }

    if (mapTypeName != MapType.Name.underground)
    {
        vSlice = null
    }

    val hardcore: Boolean = world.worldInfo.isHardcoreModeEnabled
    val mapType: MapType = MapType.from(mapTypeName, vSlice, dimension)

    if (mapType.isUnderground && hardcore)
    {
        logger.warn("Cave mapping is not allowed on hardcore servers")
        handler.status(400)
        return "Cave mapping is not allowed on hardcore servers"
    }

    val mapSaver = MapSaver(worldDir, mapType)

    if (!mapSaver.isValid)
    {
        logger.info("No image files to save")
        handler.status(400)
        return "No image files to save"
    }

    JourneymapClient.getInstance().toggleTask(SaveMapTask.Manager::class.java, true, mapSaver)

    val data = mutableMapOf<String, Any>()

    data["filename"] = mapSaver.saveFileName
    handler.response.raw().contentType = "application/json"

    return GSON.toJson(data)
}

internal fun autoMap(handler: RouteHandler, minecraft: Minecraft, world: World): Any
{
    val data = mutableMapOf<String, Any>()
    val enabled: Boolean = JourneymapClient.getInstance().isTaskManagerEnabled(MapRegionTask.Manager::class.java)
    val scope: String = handler.request.queryParamOrDefault("scope", "stop")

    if (scope == "stop" && enabled)
    {
        JourneymapClient.getInstance().toggleTask(MapRegionTask.Manager::class.java, false, false)
        data["message"] = "automap_complete"
    }
    else if (!enabled)
    {
        val doAll: Boolean = scope == "all"
        JourneymapClient.getInstance().toggleTask(MapRegionTask.Manager::class.java, true, doAll)
        data["message"] = "automap_started"
    }
    else
    {
        data["message"] = "automap_already_started"
    }

    handler.response.raw().contentType = "application/json"

    return GSON.toJson(data)
}
