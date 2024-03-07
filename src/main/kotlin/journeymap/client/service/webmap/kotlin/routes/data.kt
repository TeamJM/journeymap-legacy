package journeymap.client.service.webmap.kotlin.routes

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import journeymap.client.data.DataCache
import journeymap.client.data.ImagesData
import journeymap.client.model.Waypoint
import journeymap.common.Journeymap
import org.apache.logging.log4j.Logger
import spark.kotlin.RouteHandler


private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
private val logger: Logger = Journeymap.getLogger()


val dataTypesRequiringSince = listOf<String>("all", "images")


internal fun dataGet(handler: RouteHandler): Any
{
    val since: Long? = handler.queryMap("images.since").longValue()
    val type = handler.params("type")

    if (type in dataTypesRequiringSince && since == null)
    {
        logger.warn("Data type '$type' requested without 'images.since' parameter")
        handler.status(400)
        return "Data type '$type' requires 'images.since' parameter."
    }

    val data: Any? = when (type)
    {
        "all"       -> DataCache.instance().getAll(since!!)
        "animals"   -> DataCache.instance().getAnimals(false)
        "mobs"      -> DataCache.instance().getMobs(false)
        "images"    -> ImagesData(since!!)
        "messages"  -> DataCache.instance().getMessages(false)
        "player"    -> DataCache.instance().getPlayer(false)
        "players"   -> DataCache.instance().getPlayers(false)
        "world"     -> DataCache.instance().getWorld(false)
        "villagers" -> DataCache.instance().getVillagers(false)
        "waypoints" ->
        {
            val waypoints: Collection<Waypoint> = DataCache.instance().getWaypoints(false)
            val wpMap = mutableMapOf<String, Waypoint>()

            for (waypoint in waypoints)
            {
                wpMap[waypoint.id] = waypoint
            }

            wpMap.toMap()
        }

        else        -> null
    }

    if (data == null)
    {
        logger.warn("Unknown data type '$type'")
        handler.status(400)
        return "Unknown data type '$type'"
    }

    handler.response.raw().contentType = "application/json"
    return GSON.toJson(data)
}
