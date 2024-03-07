package journeymap.client.service.webmap.kotlin.routes

import journeymap.client.JourneymapClient
import journeymap.client.properties.WebMapProperties
import spark.kotlin.RouteHandler
import java.util.concurrent.atomic.AtomicBoolean


val webMapProperties: WebMapProperties = JourneymapClient.getWebMapProperties()
var propertiesMap: Map<String, AtomicBoolean>? = null


internal fun propertiesGet(handler: RouteHandler): Any
{
    handler.response.raw().contentType = "application/json"
    return JourneymapClient.getWebMapProperties().toJsonString()
}


internal fun propertiesPost(handler: RouteHandler): Any
{
    if (propertiesMap == null || propertiesMap!!.isEmpty())
    {
        val properties: WebMapProperties = JourneymapClient.getWebMapProperties()
        val propMap = mutableMapOf<String, AtomicBoolean>()

        propMap["showGrid"] = properties.showGrid
        propMap["showSelf"] = properties.showSelf
        propMap["showWaypoints"] = properties.showWaypoints

        propertiesMap = propMap.toMap()
    }

    for (key in handler.queryMap().toMap().keys)
    {
        if (key in propertiesMap!!)
        {
            (
                    propertiesMap!![key] ?: error("Properties value for $key is null")
                    ).set(handler.queryMap(key).booleanValue())
        }
    }

    return webMapProperties.save()
}
