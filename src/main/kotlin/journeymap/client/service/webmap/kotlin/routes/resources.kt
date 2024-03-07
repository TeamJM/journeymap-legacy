package journeymap.client.service.webmap.kotlin.routes

import journeymap.client.io.FileHandler.ASSETS_JOURNEYMAP_UI
import journeymap.client.service.webmap.Webmap
import journeymap.common.Journeymap
import journeymap.common.kotlin.extensions.getResourceAsStream
import net.minecraft.util.ResourceLocation
import org.apache.logging.log4j.Logger
import org.eclipse.jetty.io.EofException
import spark.kotlin.RouteHandler
import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import javax.imageio.IIOException
import javax.imageio.ImageIO


private val logger: Logger = Journeymap.getLogger()


internal fun resourcesGet(handler: RouteHandler): Any
{
    val img: BufferedImage
    val resource = handler.request.queryParams("resource")
    val resourceLocation = ResourceLocation(resource)

    var extension = resource.split('.').last()

    if (":" in extension)
    {
        // So we can actually get the filetype from extension
        extension = extension.split(":").first()
    }

    img = try
    {
        ImageIO.read(resourceLocation.getResourceAsStream())
    }
    catch (e: FileNotFoundException)
    {
        logger.warn("File at resource location not found: $resource")
        handler.status(404)

        ImageIO.read(Webmap.javaClass.getResource("$ASSETS_JOURNEYMAP_UI/img/marker-dot-32.png"))
    }
    catch (e: EofException)
    {
        logger.info("Connection closed while writing image response. Webmap probably reloaded.")
        return ""
    }
    catch (e: IIOException)
    {
        logger.info("Connection closed while writing image response. Webmap probably reloaded.")
        return ""
    }
    catch (e: Exception)
    {
        logger.error("Exception thrown while retrieving resource at location: $resource", e)
        handler.status(500)

        ImageIO.read(Webmap.javaClass.getResource("$ASSETS_JOURNEYMAP_UI/img/marker-dot-32.png"))
    }

    handler.response.raw().contentType = "image/${extension}"
    ImageIO.write(img, extension, handler.response.raw().outputStream)
    handler.response.raw().outputStream.flush()

    return handler.response
}
