package journeymap.client.webmap.routes;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.JourneymapClient;
import journeymap.client.io.FileHandler;
import journeymap.client.io.IconSetFileHandler;
import journeymap.client.render.texture.TextureCache;
import journeymap.client.webmap.Resources;
import journeymap.client.webmap.WebMap;
import journeymap.common.Journeymap;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static journeymap.client.io.FileHandler.ASSETS_JOURNEYMAP_UI;

public class Resource
{
    public static final Logger logger = Journeymap.getLogger();

    public static void get(Context ctx)
    {
        BufferedImage img;
        String resource = ctx.queryParam("resource");
        if (resource == null || resource.equals("undefined"))
        {
            return;
        }

        ResourceLocation resourceLocation = new ResourceLocation(resource);
        String extension = resource.split("\\.").length > 1 ? resource.split("\\.")[resource.split("\\.").length - 1] : "";
        if (extension.contains(":"))
        {
            // So we can actually get the filetype from extension
            extension = extension.split(":")[0];
        }
        try
        {
            if (!resourceLocation.getResourceDomain().contains("journeymap"))
            {
                img = TextureCache.instance().getEntityIconTexture(JourneymapClient.getWebMapProperties().getEntityIconSetName().get(), resource).getImage();

                InputStream is = FileHandler.getIconStream(IconSetFileHandler.ASSETS_JOURNEYMAP_ICON_ENTITY, JourneymapClient.getWebMapProperties().getEntityIconSetName().get(), resource);
                if (img == null && is != null)
                {

                    ctx.contentType(ContentType.IMAGE_PNG);
                    ctx.result(is);
                    return;
                }
            }
            else
            {
                img = ImageIO.read(Resources.getResourceAsStream(resourceLocation));
            }
        }
        catch (FileNotFoundException e)
        {
            logger.warn("File at resource location not found: " + resource);
            ctx.status(404);
            try
            {
                img = ImageIO.read(WebMap.class.getResource(ASSETS_JOURNEYMAP_UI + "marker-dot-32.png"));
            }
            catch (Exception ex)
            {
                logger.warn("image not found {}", ASSETS_JOURNEYMAP_UI + "marker-dot-32.png");
                return;
            }
        }
        catch (IOException e)
        {
            logger.info("Connection closed while writing image response. Webmap probably reloaded.");
            return;
        }
        catch (Exception e)
        {
            logger.error("Exception thrown while retrieving resource at location: " + resource, e);
            ctx.status(500);
            try
            {
                img = ImageIO.read(WebMap.class.getResource(ASSETS_JOURNEYMAP_UI + "marker-dot-32.png"));
            }
            catch (Exception ex)
            {
                logger.warn("image not found {}", ASSETS_JOURNEYMAP_UI + "marker-dot-32.png");
                return;
            }
        }
        ctx.contentType("image/" + extension);
        try
        {
            ImageIO.write(img, extension, ctx.res.getOutputStream());
            ctx.res.getOutputStream().flush();
        }
        catch (Exception e)
        {
            logger.warn("image not found {}", resource);
        }

    }
}
