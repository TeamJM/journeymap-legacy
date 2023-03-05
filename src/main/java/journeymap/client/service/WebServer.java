/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.service;

import journeymap.client.JourneymapClient;
import journeymap.client.log.ChatLog;
import journeymap.client.log.LogFormatter;
import journeymap.client.properties.WebMapProperties;
import journeymap.common.Journeymap;
import journeymap.common.thread.JMThreadFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import se.rupy.http.Daemon;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wraps Rupy Daemon and provides thread management.  Tests webserver_port
 * before starting daemon.
 *
 * @author techbrew
 */
public class WebServer
{
    private final static int MAXPORT = 9990;
    private final static int MAXFAILS = 5;
    private static volatile WebServer instance;

    private final Logger logger = Journeymap.getLogger();

    private Daemon rupy;
    private int port;
    private boolean ready = false;

    private WebServer()
    {
        port = JourneymapClient.getWebMapProperties().port.get();
        validatePort();
    }

    public static void setEnabled(Boolean enable, boolean forceAnnounce)
    {
        WebMapProperties webMapProperties = JourneymapClient.getWebMapProperties();
        webMapProperties.enabled.set(enable);
        webMapProperties.save();

        if (instance != null)
        {
            try
            {
                instance.stop();
            }
            catch (Throwable e)
            {
                Journeymap.getLogger().log(Level.ERROR, LogFormatter.toString(e));
            }
        }

        if (enable)
        {
            try
            {
                instance = new WebServer();
                if (instance.isReady())
                {
                    instance.start();
                }
                else
                {
                    enable = false;
                }
            }
            catch (Throwable e)
            {
                Journeymap.getLogger().log(Level.ERROR, LogFormatter.toString(e));
                enable = false;
            }
            if (!enable)
            {
                Journeymap.getLogger().error("Unexpected error, JMServer couldn't be started.");
            }
        }

        if (forceAnnounce)
        {
            ChatLog.enableAnnounceMod = true;
        }
        ChatLog.announceMod(forceAnnounce);
    }

    public static WebServer getInstance()
    {
        return instance;
    }

    /**
     * Verify port can be bound, try to find another one if not.
     */
    private void validatePort()
    {

        int hardFails = 0;
        int testPort = port;
        final int maxPort = Math.max(MAXPORT, port + 1000);
        boolean validPort = false;

        while (!validPort && hardFails <= MAXFAILS && testPort <= maxPort)
        {
            ServerSocketChannel server = null;
            try
            {
                server = ServerSocketChannel.open();
                server.socket().bind(new InetSocketAddress(testPort));
                validPort = true;
            }
            catch (java.net.BindException e)
            {
                logger.warn("Port " + testPort + " already in use");
                testPort += 10;
            }
            catch (Throwable t)
            {
                logger.error("Error when testing port " + testPort + ": " + t);
                hardFails++;
            }
            finally
            {
                if (server != null)
                {
                    try
                    {
                        server.close();
                    }
                    catch (IOException e)
                    {
                    }
                }
            }
        }

        ready = validPort;

        if (ready && port != testPort)
        {
            logger.info("Webserver will use port " + testPort + " for this session");
            port = testPort;
        }

        if (!ready && hardFails > MAXFAILS)
        {
            logger.error("Gave up finding a port for webserver after " + hardFails + " failures to test ports!");
        }

        if (!ready && testPort > MAXPORT)
        {
            logger.error("Gave up finding a port for webserver after testing ports " + port + " - " + maxPort + " without finding one open!");
        }

    }

    public boolean isReady()
    {
        return ready;
    }

    public int getPort()
    {
        return port;
    }

    public void start() throws Exception
    {

        if (!ready)
        {
            throw new IllegalStateException("Initialization failed");
        }

        // Init properties for daemon
        Properties props = new Properties();

        // Use port from journeymap properties

        props.put("port", Integer.toString(port)); //$NON-NLS-1$
        props.put("delay", Integer.toString(5000)); //$NON-NLS-1$ // socket timeout in ms
        props.put("timeout", Integer.toString(0)); //$NON-NLS-1$ // session timeout, 0 to disable sessions
        props.put("threads", Integer.toString(5)); //$NON-NLS-1$

        // Rupy logging is spammy.  Only enable it if you really need to.
        Level logLevel = Level.toLevel(JourneymapClient.getCoreProperties().logLevel.get(), Level.INFO);
        if (logLevel.intLevel() >= (Level.TRACE.intLevel()))
        {
            props.put("debug", Boolean.TRUE.toString()); //$NON-NLS-1$
        }
        if (logLevel.intLevel() >= (Level.TRACE.intLevel()))
        {
            props.put("verbose", Boolean.TRUE.toString()); //$NON-NLS-1$
        }

        rupy = new Daemon(props);
        rupy.add(new DataService());
        rupy.add(new LogService());
        rupy.add(new TileService());
        rupy.add(new ActionService());
        rupy.add(new FileService());
        rupy.add(new PropertyService());
        rupy.add(new DebugService());
        rupy.add(new MapApiService());

        // Initialize daemon
        rupy.init();

        // Init thread factory
        JMThreadFactory tf = new JMThreadFactory("svr");

        // Run server in own thread
        ExecutorService es = Executors.newSingleThreadExecutor(tf);
        es.execute(rupy);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(tf.newThread(new Runnable()
        {
            @Override
            public void run()
            {
                stop();
            }
        }));

        logger.info("Started webserver on port " + port); //$NON-NLS-1$

    }

    public void stop()
    {
        try
        {
            if (rupy.isAlive())
            {
                rupy.stop();
                logger.info("Stopped webserver without errors"); //$NON-NLS-1$
            }
        }
        catch (Throwable t)
        {
            logger.info("Stopped webserver with error: " + t); //$NON-NLS-1$
        }
    }

}
