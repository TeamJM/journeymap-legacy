/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package modinfo.mp.v1;

import modinfo.Config;
import modinfo.ModInfo;
import org.apache.logging.log4j.Level;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author techbrew 2/17/14.
 */
public class Client
{

    public static final String ENDPOINT = "http://www.google-analytics.com/collect";

    private final String VERBOSE_PATTERN = "ModInfo (%s): %s";

    private final String trackingId;
    private final UUID clientId;
    private final Config config;
    private final String userAgent;

    private final ExecutorService service;

    private int retries = 5;
    private int connectTimeout = 5000;
    private int readTimeout = 2000;

    private AtomicInteger messageCount = new AtomicInteger(0);

    public Client(String trackingId, UUID clientId, Config config, String defaultUserLanguage)
    {
        this.trackingId = trackingId;
        this.clientId = clientId;
        this.config = config;
        this.userAgent = createUserAgent(defaultUserLanguage);
        this.service = Executors.newFixedThreadPool(2);

        if (config.isVerbose())
        {
            showVerboseMessage("User-Agent: " + this.userAgent);
        }
    }

    public Future send(Payload payload)
    {
        return send(payload, null);
    }

    public Future send(Payload payload, final Message.Callback callback)
    {
        if (config.isEnabled())
        {
            // Ensure required parameters
            payload.put(Payload.Parameter.Version, Payload.VERSION);
            payload.put(Payload.Parameter.TrackingId, trackingId);
            payload.put(Payload.Parameter.ClientId, clientId.toString());
            payload.put(Payload.Parameter.CustomMetric1, Integer.toString(messageCount.incrementAndGet()));

            // Create message and schedule with executor
            final Message message = new Message(ENDPOINT, payload, userAgent, retries, connectTimeout, readTimeout);
            final FutureTask<Void> future = new FutureTask<Void>(getRunnableWrapper(message, payload, callback), null);
            service.submit(future);

            return future;
        }
        return null;
    }

    private Runnable getRunnableWrapper(final Message message, final Payload payload, final Message.Callback callback)
    {
        return new Runnable()
        {
            @Override
            public void run()
            {

                Object result = null;

                try
                {
                    result = message.call();
                }
                catch (Throwable t)
                {
                    ModInfo.LOGGER.log(Level.ERROR, "ModInfo couldn't send message", t);
                }

                try
                {
                    if (config.isVerbose() && Boolean.TRUE.equals(result))
                    {
                        showVerboseMessage(payload.toVerboseString());
                    }
                }
                catch (Throwable t)
                {
                    ModInfo.LOGGER.log(Level.ERROR, "ModInfo couldn't do verbose output", t);
                }

                try
                {
                    if (callback != null)
                    {
                        callback.onResult(result);
                    }
                }
                catch (Throwable t)
                {
                    ModInfo.LOGGER.log(Level.ERROR, "ModInfo couldn't use callback", t);
                }
            }
        };
    }

    private String createUserAgent(String defaultUserLanguage)
    {
        String agent = null;

        try
        {
            // Get system properties
            String os = System.getProperty("os.name");
            if (os == null)
            {
                os = "";
            }

            String version = System.getProperty("os.version");
            if (version == null)
            {
                version = "";
            }

            String arch = System.getProperty("os.arch");
            if (arch == null)
            {
                arch = "";
            }
            if (arch.equals("amd64"))
            {
                arch = "WOW64";
            }

            String lang = String.format("%s_%s", System.getProperty("user.language"), System.getProperty("user.country"));
            if (lang.contains("null"))
            {
                lang = defaultUserLanguage;
            }

            // Build user agent string
            if (os.startsWith("Mac")) // Mac OS X, x86_64, ?
            {
                version = version.replace(".", "_");
                agent = String.format("Mozilla/5.0 (Macintosh; U; Intel Mac OS X %s; %s)", version, lang);
            }
            else if (os.startsWith("Win")) // Windows 7, amd64, 6.1
            {
                agent = String.format("Mozilla/5.0 (Windows; U; Windows NT %s; %s; %s)", version, arch, lang);
            }
            else if (os.startsWith("Linux")) // Linux, os.version = kernel version, os.arch = amd64
            {
                agent = String.format("Mozilla/5.0 (Linux; U; Linux %s; %s; %s)", version, arch, lang);
            }
            else
            {
                agent = String.format("Mozilla/5.0 (%s; U; %s %s; %s, %s)", os, os, version, arch, lang);
            }
        }
        catch (Throwable t)
        {
            ModInfo.LOGGER.log(Level.ERROR, "ModInfo couldn't create useragent string", t);
            agent = "Mozilla/5.0 (Unknown)";
        }

        return agent;
    }

    private void showVerboseMessage(String message)
    {
        System.out.println(String.format(VERBOSE_PATTERN, config.getModId(), message));
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        if (this.service != null)
        {
            this.service.shutdown();
        }
    }
}
