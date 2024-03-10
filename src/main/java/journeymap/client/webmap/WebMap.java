package journeymap.client.webmap;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.staticfiles.Location;
import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.client.io.FileHandler;
import journeymap.client.webmap.routes.Data;
import journeymap.client.webmap.routes.Logs;
import journeymap.client.webmap.routes.Polygons;
import journeymap.client.webmap.routes.Properties;
import journeymap.client.webmap.routes.Resource;
import journeymap.client.webmap.routes.Skin;
import journeymap.client.webmap.routes.Status;
import journeymap.client.webmap.routes.Tiles;
import journeymap.common.Journeymap;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public class WebMap
{
    public static final Logger logger = Journeymap.getLogger();
    private int port = 0;
    private boolean started = false;
    private Javalin app;

    private static WebMap instance;


    public static WebMap getInstance()
    {
        if (instance == null)
        {
            instance = new WebMap();
        }
        return instance;
    }

    public void start()
    {
        if (!started)
        {
            findPort(true);
            initialise();
            started = true;
            logger.info("Webmap is now listening on port: {}.", port);
        }
    }

    private void initialise()
    {
        try
        {
            app = Javalin.create(config -> {
                        String assetsRootProperty = System.getProperty("journeymap.webmap.assets_root", null);
                        File testFile = new File("../src/main/resources" + FileHandler.ASSETS_WEBMAP);

                        if (assetsRootProperty != null)
                        {
                            logger.info("Detected 'journeymap.webmap.assets_root' property, serving static files from: {}", assetsRootProperty);
                            config.addStaticFiles(assetsRootProperty, Location.EXTERNAL);
                            if (testFile.exists())
                            {
                                try
                                {
                                    String assets = testFile.getCanonicalPath();
                                    logger.info("Development environment detected, serving static files from the filesystem.: {}", assets);
                                    config.addStaticFiles(testFile.getCanonicalPath(), Location.EXTERNAL);
                                }
                                catch (IOException e)
                                {
                                    logger.error("Webmap error finding local assets path", e);
                                }
                            }
                        }
                        else
                        {
                            File dir = new File(FileHandler.getMinecraftDirectory(), Constants.WEB_DIR);
                            if (!dir.exists())
                            {
                                ResourceLocation location = new ResourceLocation(Journeymap.MOD_ID, "web");
                                String assetPath = String.format("/assets/%s/%s", location.getResourceDomain(), location.getResourcePath());
                                logger.info("Attempting to copy web content to {}", dir);
                                boolean created = FileHandler.copyResources(dir, assetPath, "", false);
                                logger.info("Web content copied successfully: {}", created);
                            }

                            if (dir.exists())
                            {
                                logger.info("Loading web content from local: {}", dir.getPath());
                                config.addStaticFiles(dir.getPath(), Location.EXTERNAL);
                            }
                            else
                            {
                                logger.info("Loading web content from jar: {}", FileHandler.ASSETS_WEBMAP);
                                config.addStaticFiles(FileHandler.ASSETS_WEBMAP, Location.CLASSPATH);
                            }
                        }

                    }).before(ctx -> {
                        ctx.header("Access-Control-Allow-Origin", "*");
                        ctx.header("Cache-Control", "no-cache");
                    })
                    .get("/data/{type}", Data::get)
                    .get("/logs", Logs::get)
                    .get("/properties", Properties::get)
                    .get("/resources", Resource::get)
                    .get("/skin/{uuid}", Skin::get)
                    .get("/status", Status::get)
                    .get("/tiles/tile.png", Tiles::get)
                    .get("/polygons", Polygons::get)
                    .post("/properties", Properties::post)
                    .start();

        }
        catch (Exception e)
        {
            logger.error("Failed to start server:", e);
            stop();
        }
    }

    public void stop()
    {
        if (started)
        {
            app.stop();
            started = false;
            logger.info("Webmap stopped.");
        }
    }

    private void findPort(boolean tryCurrentPort)
    {
        if (port == 0)
        {
            // the client may be null due to class loading issues in dev
            // this just suppresses the exception in dev. it does not fix the class loading issue or the webmap
            if (JourneymapClient.getInstance() == null || JourneymapClient.getWebMapProperties() == null)
            {
                port = 8080;
            }
            else
            {
                // We set this here because we need to get it again if the user changes the setting
                port = JourneymapClient.getWebMapProperties().port.get();
                logger.info("port found, set to {}", port);
            }
        }

        if (tryCurrentPort)
        {
            try
            {
                ServerSocket socket = new ServerSocket(port);
                port = socket.getLocalPort();
                socket.close();
            }
            catch (IOException e)
            {
                logger.warn("Configured port {} could not be bound: ", port, e);
                findPort(false);
            }

            logger.info("Configured port {} is available.", port);
        }
        else
        {
            try
            {
                ServerSocket socket = new ServerSocket(0);
                port = socket.getLocalPort();
                socket.close();
                logger.info("New port {} assigned by ServerSocket.", port);
            }
            catch (IOException e)
            {
                logger.error("Configured port {} could not be bound on second attempt, failing: ", port, e);
                stop();
            }
        }
    }

    public int getPort()
    {
        return port;
    }
}
