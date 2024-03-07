package journeymap.client.service.webmap.kotlin

import journeymap.client.log.LogFormatter
import journeymap.common.Journeymap
import org.apache.logging.log4j.Logger
import spark.kotlin.RouteHandler


// Variable/value declarations
private val logger: Logger = Journeymap.getLogger()


internal fun wrapForError(function: (RouteHandler) -> Any): (RouteHandler) -> Any
{
    fun wrapper(handler: RouteHandler): Any
    {
        return try
        {
            function(handler)
        }
        catch (t: Throwable)
        {
            logger.error(LogFormatter.toString(t))
            handler.response.status(500)

            t.localizedMessage
        }
    }

    return ::wrapper
}
