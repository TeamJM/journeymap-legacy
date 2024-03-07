package journeymap.client.service.webmap

import journeymap.client.JourneymapClient
import journeymap.client.io.FileHandler
import journeymap.client.service.webmap.kotlin.routes.*
import journeymap.client.service.webmap.kotlin.wrapForError
import journeymap.common.Journeymap
import org.apache.logging.log4j.Logger
import spark.Filter
import spark.Spark.init
import spark.Spark.initExceptionHandler
import spark.kotlin.*
import java.io.File
import java.io.IOException
import java.net.ServerSocket

object Webmap
{
    val logger: Logger = Journeymap.getLogger()

    var port: Int = 0
    var started: Boolean = false

    fun start()
    {
        if (!this.started)
        {
            this.findPort()
            port(this.port)

            this.initialise()

            this.started = true
            this.logger.info("Webmap is now listening on port ${this.port}.")
        }
    }

    private fun initialise()
    {
        initExceptionHandler {
            this.logger.error("Failed to start server: $it")
            this.stop()
        }

        val assetsRootProperty: String? = System.getProperty("journeymap.webmap.assets_root", null)
        val testFile = File("../src/main/resources" + FileHandler.ASSETS_WEBMAP)

        when
        {
            assetsRootProperty != null ->
            {
                logger.info("Detected 'journeymap.webmap.assets_root' property, serving static files from: $assetsRootProperty")
                staticFiles.externalLocation(assetsRootProperty)
            }
            testFile.exists()          ->
            {
                logger.info("Development environment detected, serving static files from the filesystem.")
                staticFiles.externalLocation(testFile.canonicalPath)
            }
            else                       ->
            {
                staticFiles.location(FileHandler.ASSETS_WEBMAP)
            }
        }

        before(Filter { _, response ->
            // Allow access from all origins (eg for Apiary access)
            response.header("Access-Control-Allow-Origin", "*")

            // Prevent caching of all routes
            response.header("Cache-Control", "no-cache")
        })

        get("/data/:type", function = wrapForError(::dataGet))
        get("/logs", function = wrapForError(::logGet))
        get("/properties", function = wrapForError(::propertiesGet))
        get("/resources", function = wrapForError(::resourcesGet))
        get("/skin/:uuid", function = wrapForError(::skinGet))
        get("/status", function = wrapForError(::statusGet))
        get("/tiles/tile.png", function = wrapForError(::tilesGet))

        post("/properties", function = wrapForError(::propertiesPost))

        init()
    }

    fun stop()
    {
        if (this.started)
        {
            spark.kotlin.stop()
            this.started = false
            this.logger.info("Webmap stopped.")
        }
    }

    private fun findPort(tryCurrentPort: Boolean = true)
    {
        if (this.port == 0)
        {
            // We set this here because we need to get it again if the user changes the setting
            this.port = JourneymapClient.getWebMapProperties().port.get()
        }

        if (tryCurrentPort)
        {
            try
            {
                val socket = ServerSocket(this.port)
                this.port = socket.localPort
                socket.close()
            }
            catch (e: IOException)
            {
                this.logger.warn("Configured port ${this.port} could not be bound: $e")
                findPort(false)
            }

            this.logger.info("Configured port ${this.port} is available.")
        }
        else
        {
            val socket = ServerSocket(0)
            this.port = socket.localPort
            socket.close()

            this.logger.info("New port ${this.port} assigned by ServerSocket.")
        }
    }
}
