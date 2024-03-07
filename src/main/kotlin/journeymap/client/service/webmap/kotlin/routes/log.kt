package journeymap.client.service.webmap.kotlin.routes

import journeymap.client.log.JMLogger
import journeymap.common.Journeymap
import org.apache.logging.log4j.Logger
import spark.kotlin.RouteHandler
import java.io.File


private val logger: Logger = Journeymap.getLogger()


internal fun logGet(handler: RouteHandler): Any
{
    val logFile: File = JMLogger.getLogFile()

    return if (logFile.exists())
    {
        handler.response.raw().addHeader("Content-Disposition", "inline; filename=\"journeymap.log\"")
        handler.response.raw().outputStream.write(logFile.readBytes())
        handler.response.raw().outputStream.flush()

        handler.response
    }
    else
    {
        logger.warn("Unable to find JourneyMap logfile")
        handler.status(404)

        "Not found: ${logFile.path}"
    }
}
