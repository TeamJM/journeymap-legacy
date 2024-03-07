package journeymap.client.service.webmap.kotlin.routes

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import journeymap.client.JourneymapClient
import journeymap.client.service.webmap.kotlin.enums.WebmapStatus
import journeymap.client.ui.minimap.MiniMap
import net.minecraft.client.Minecraft
import spark.kotlin.RouteHandler


private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()


internal fun statusGet(handler: RouteHandler): Any {
    val data: MutableMap<String, Any> = mutableMapOf()

    var status = when {
        Minecraft.getMinecraft().theWorld == null -> WebmapStatus.NO_WORLD
        !JourneymapClient.getInstance().isMapping -> WebmapStatus.STARTING

        else -> WebmapStatus.READY
    }

    if (status == WebmapStatus.READY) {
        val mapState = MiniMap.state()

        data["mapType"] = mapState.getCurrentMapType().name()

        val allowedMapTypes: Map<String, Boolean> = mapOf(
            "cave" to (mapState.isCaveMappingAllowed)
        )

        if (allowedMapTypes.filterValues { it }.isEmpty()) {
            status = WebmapStatus.DISABLED
        }

        data["allowedMapTypes"] = allowedMapTypes
    }

    data["status"] = status.status

    return GSON.toJson(data)
}
