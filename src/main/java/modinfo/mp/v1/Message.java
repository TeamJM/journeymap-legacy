/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package modinfo.mp.v1;

import modinfo.ModInfo;
import org.apache.logging.log4j.Level;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * @author techbrew 2/18/14.
 */
public class Message implements Callable<Object>
{
    private final String endpoint;
    private final Payload payload;
    private final String userAgent;
    private final int retries;
    private final int connectionTimeout;
    private final int readTimeout;

    Message(String endpoint, Payload payload, String userAgent, int retries, int connectionTimeout, int readTimeout)
    {
        this.endpoint = endpoint;
        this.payload = payload;
        this.userAgent = userAgent;
        this.retries = retries;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }


    @Override
    public Object call()
    {
        String defaultAgent = System.setProperty("http.agent", "");
        int remainingRetries = Math.max(1, retries);

        Exception exception = null;
        Integer responseCode = null;
        while (responseCode == null && remainingRetries > 0)
        {
            try
            {
                String payloadString = payload.toUrlEncodedString();

                URL url = new URL(endpoint);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", userAgent);
                con.setConnectTimeout(connectionTimeout);
                con.setReadTimeout(readTimeout);
                con.setUseCaches(false);

                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(payloadString);
                wr.flush();
                wr.close();

                responseCode = con.getResponseCode();
            }
            catch (MalformedURLException ex)
            {
                exception = ex;
                ModInfo.LOGGER.log(Level.ERROR, "ModInfo got a bad URL: " + endpoint);
                break;
            }
            catch (IOException ex)
            {
                exception = ex;
                ModInfo.LOGGER.log(Level.ERROR, "ModInfo can't send message", ex);
            }
            finally
            {
                remainingRetries--;
            }
        }

        if (defaultAgent != null && defaultAgent.length() > 0)
        {
            System.setProperty("http.agent", defaultAgent);
        }

        if (responseCode != null)
        {
            return Boolean.TRUE;
        }
        else if (exception == null)
        {
            exception = new Exception("ModInfo got a null response from endpoint");
        }
        return exception;
    }

    public interface Callback
    {
        public void onResult(Object result);
    }
}
