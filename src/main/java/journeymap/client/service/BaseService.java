/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import journeymap.client.Constants;
import journeymap.client.log.JMLogger;
import journeymap.common.Journeymap;
import org.apache.logging.log4j.Logger;
import se.rupy.http.Event;
import se.rupy.http.Query;
import se.rupy.http.Reply;
import se.rupy.http.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;

/**
 * Provides common functionality for Rupy service implementations.
 *
 * @author techbrew
 */
public abstract class BaseService extends Service
{

    public static final Charset UTF8 = Charset.forName("UTF-8"); //$NON-NLS-1$
    public static final String CALLBACK_PARAM = "callback";  //$NON-NLS-1$
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected String path;

    /**
     * Log and throw a Rupy Event exception.
     *
     * @param code
     * @param message
     * @param event
     */
    protected void throwEventException(int code, String message, Event event, boolean isError) throws Event, Exception
    {

        Logger logger = Journeymap.getLogger();

        // Log the issue depending on severity
        String out = code + " " + message; //$NON-NLS-1$ //$NON-NLS-2$
        if (isError)
        {
            Exception ex = null;
            if (code != 404)
            {
                ex = new Exception(debugRequestHeaders(event));
            }
            JMLogger.logOnce(this.getClass().getName() + ": " + out, ex);
        }
        else if (logger.isTraceEnabled())
        {
            Journeymap.getLogger().trace(out);
        }

        // Set the error code on the response
        try
        {
            if (code == 404)
            {
                event.reply().code("404 Not Found"); //$NON-NLS-1$
            }
            else
            {
                event.reply().code(out);
            }
        }
        catch (IOException e)
        {
            logger.warn("Can't set response code: " + out); //$NON-NLS-1$
        }
        throw event;
    }

    /**
     * Get request headers and remote address for a request Event.
     *
     * @param event
     * @return
     */
    protected String debugRequestHeaders(Event event) throws Exception
    {

        StringBuffer sb = new StringBuffer("HTTP Request:"); //$NON-NLS-1$

        if (event.query() != null)
        {
            event.query().parse();
            sb.append("\n request=").append(event.query().path());
            if (event.query().parameters() != null)
            {
                sb.append("?").append(event.query().parameters());
            }
            HashMap headers = event.query().header();
            for (Object name : headers.keySet())
            {
                Object value = headers.get(name);
                sb.append("\n ").append(name).append("=").append(value); //$NON-NLS-1$ //$NON-NLS-2$
            }
            sb.append("\n Remote Address:").append(event.remote());
        }

        return sb.toString();
    }

    /**
     * Log a bad request coming from the browser.
     *
     * @param event
     * @throws Event
     */
    protected void reportMalformedRequest(Event event) throws Event, Exception
    {
        String error = "Bad Request: " + event.query().path(); //$NON-NLS-1$
        Journeymap.getLogger().error(error);
        throwEventException(400, error, event, false);
    }

    /**
     * Get a request parameter String value or return the default provided.
     *
     * @param map
     * @param key
     * @param defaultValue
     * @return
     */
    protected String getParameter(Query map, String key, String defaultValue)
    {
        Object val = map.get(key);
        return (val != null) ? val.toString() : defaultValue;
    }

    /**
     * Get a request parameter int value or return the default provided.
     *
     * @param map
     * @param key
     * @param defaultValue
     * @return
     */
    protected Integer getParameter(Map<String, String[]> map, String key, Integer defaultValue)
    {
        Object val = map.get(key);
        Integer intVal = null;
        if (val != null)
        {
            try
            {
                intVal = Integer.parseInt((String) val);
            }
            catch (NumberFormatException e)
            {
                Journeymap.getLogger().warn("Didn't get numeric query parameter for '" + key + "': " + val);
            }
        }
        return (intVal != null) ? intVal : defaultValue;
    }

    /**
     * Get a request parameter int value or return the default provided.
     *
     * @param map
     * @param key
     * @param defaultValue
     * @return
     */
    protected Long getParameter(Map<String, String[]> map, String key, Long defaultValue)
    {
        Object val = map.get(key);
        Long longVal = null;
        if (val != null)
        {
            try
            {
                longVal = Long.parseLong((String) val);
            }
            catch (NumberFormatException e)
            {
                Journeymap.getLogger().warn("Didn't get numeric query parameter for '" + key + "': " + val);
            }
        }
        return (longVal != null) ? longVal : defaultValue;
    }

    /**
     * Attempt to output the data in gzip format, setting headers accordingly.
     * Falls back to sending plain text otherwise.
     *
     * @param event
     * @param data
     */
    protected void gzipResponse(Event event, String data) throws Exception
    {

        byte[] bytes = gzip(data);
        if (bytes != null)
        {
            ResponseHeader.on(event).setHeader("Content-encoding", "gzip");    //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            bytes = data.getBytes(UTF8);
        }

        ResponseHeader.on(event).contentLength(bytes.length);
        event.output().write(bytes);
    }

    /**
     * Attempt to output the data in gzip format, setting headers accordingly.
     * Falls back to sending without gzip otherwise.
     *
     * @param event
     * @param data
     */
    protected void gzipResponse(Event event, byte[] data) throws Exception
    {

        byte[] bytes = gzip(data);
        if (bytes != null)
        {
            ResponseHeader.on(event).setHeader("Content-encoding", "gzip");    //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            bytes = data;
        }

        ResponseHeader.on(event).contentLength(bytes.length);
        event.output().write(bytes);
    }

    /**
     * Respond with a JSON-encoded Map.  Uses JSONP if needed.
     *
     * @param event
     * @param responseObj
     * @throws Exception
     */
    protected void respondJson(Event event, Map responseObj) throws Exception
    {

        Query query = event.query();

        // Build the response string
        StringBuffer jsonData = new StringBuffer();

        // Check for callback to determine Json or JsonP
        boolean useJsonP = query.containsKey(CALLBACK_PARAM);
        if (useJsonP)
        {
            try
            {
                jsonData.append(URLEncoder.encode(query.get(CALLBACK_PARAM).toString(), UTF8.name()));
            }
            catch (UnsupportedEncodingException e)
            {
                jsonData.append(query.get(CALLBACK_PARAM).toString());
            }
            jsonData.append("("); //$NON-NLS-1$
        }
        else
        {
            jsonData.append("data="); //$NON-NLS-1$
        }

        // Append the data
        jsonData.append(GSON.toJson(responseObj));

        // Finish function call for JsonP if needed
        if (useJsonP)
        {
            jsonData.append(")"); //$NON-NLS-1$
        }

        // Optimize headers for JSONP
        ResponseHeader.on(event).noCache().contentType(ContentType.jsonp);

        // Gzip response
        gzipResponse(event, jsonData.toString());
    }

    /**
     * Gzip encode a string and return the byte array.
     *
     * @param data
     * @return
     */
    protected byte[] gzip(String data)
    {
        ByteArrayOutputStream bout = null;
        try
        {
            return gzip(data.getBytes(UTF8));
        }
        catch (Exception ex)
        {
            Journeymap.getLogger().warn("Failed to UTF8 encode: " + data);
            return null;
        }
    }

    /**
     * Gzip encode a byte array.
     *
     * @param data
     * @return
     */
    protected byte[] gzip(byte[] data)
    {
        ByteArrayOutputStream bout = null;
        try
        {
            bout = new ByteArrayOutputStream();
            GZIPOutputStream output = new GZIPOutputStream(bout);
            output.write(data);
            output.flush();
            output.close();
            bout.flush();
            bout.close();
            return bout.toByteArray();
        }
        catch (Exception ex)
        {
            Journeymap.getLogger().warn("Failed to gzip encode: " + Arrays.toString(data));
            return null;
        }
    }

    protected void serveImage(Event event, BufferedImage img) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        baos.flush();
        byte[] bytes = baos.toByteArray();
        baos.close();
        event.output().write(bytes);
        //gzipResponse(event, bytes);
        return;
    }

    /**
     * Enum to encapsulate knowledge of the
     * MIME types for given file extensions.
     *
     * @author techbrew
     */
    enum ContentType
    {

        css("text/css; charset=utf-8"), //$NON-NLS-1$
        gif("image/gif"), //$NON-NLS-1$
        ico("image/x-icon"), //$NON-NLS-1$
        htm("text/html; charset=utf-8"), //$NON-NLS-1$
        html("text/html; charset=utf-8"), //$NON-NLS-1$
        js("application/javascript; charset=utf-8"), //$NON-NLS-1$
        json("application/json; charset=utf-8"), //$NON-NLS-1$
        jsonp("application/javascript; charset=utf-8"), //$NON-NLS-1$
        png("image/png"), //$NON-NLS-1$
        jpeg("image/jpeg"), //$NON-NLS-1$
        jpg("image/jpeg"), //$NON-NLS-1$
        log("text/plain; charset=utf-8"), //$NON-NLS-1$
        txt("text/plain; charset=utf-8"), //$NON-NLS-1$
        UNKNOWN("application/x-unknown"); //$NON-NLS-1$
        static final EnumSet htmlTypes = EnumSet.of(ContentType.htm, ContentType.html, ContentType.txt);
        private final String mime;

        private ContentType(String mime)
        {
            this.mime = mime;
        }

        static ContentType fromFileName(String fileName)
        {
            String name = fileName.toLowerCase(Locale.ENGLISH);
            String ext = name.substring(name.lastIndexOf('.') + 1); //$NON-NLS-1$
            try
            {
                return ContentType.valueOf(ext);
            }
            catch (Exception e)
            {
                Journeymap.getLogger().warn("No ContentType match for file: " + name); //$NON-NLS-1$
                return null;
            }
        }

        String getMime()
        {
            return mime;
        }
    }

    /**
     * Encapsulate knowledge about setting HTTP headers
     * on the Event response. Builder pattern allows for
     * convenient method chaining.
     *
     * @author techbrew
     */
    static class ResponseHeader
    {

        /**
         * Date format pattern used to parse HTTP date headers in RFC 1123 format.
         */
        protected static SimpleDateFormat dateFormat;

        static
        {
            dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Constants.getLocale()); //$NON-NLS-1$
            dateFormat.setTimeZone(Constants.GMT);
        }

        private Reply reply;

        private ResponseHeader(Event event)
        {
            this.reply = event.reply();
        }

        static ResponseHeader on(Event event)
        {
            return new ResponseHeader(event);
        }

        ResponseHeader setHeader(String name, String value)
        {
            if (reply == null)
            {
                throw new IllegalStateException("ResponseHeader builder already cleared."); //$NON-NLS-1$
            }
            reply.header(name, value);
            return this;
        }

        /**
         * Set headers to prevent browser caching of the response.
         *
         * @return
         */
        ResponseHeader noCache()
        {
            setHeader("Cache-Control", "no-cache"); //HTTP 1.1 //$NON-NLS-1$ //$NON-NLS-2$
            setHeader("Pragma", "no-cache"); //HTTP 1.0 //$NON-NLS-1$ //$NON-NLS-2$
            setHeader("Expires", "0"); //prevents caching by a proxy server  //$NON-NLS-1$ //$NON-NLS-2$
            return this;
        }

        /**
         * Set content headers for the file to be returned.
         *
         * @param file
         * @return
         */
        ResponseHeader content(File file)
        {
            contentType(ContentType.fromFileName(file.getName()));
            contentLength(file.length());
            return contentModified(file.lastModified());
        }

        /**
         * Set content headers for the ZipEntry-based file to be returned.
         *
         * @param zipEntry
         * @return
         */
        ResponseHeader content(ZipEntry zipEntry)
        {
            contentType(ContentType.fromFileName(zipEntry.getName()));
            long size = zipEntry.getSize();
            long time = zipEntry.getTime();
            if (size > -1)
            {
                contentLength(size);
            }
            if (time > -1)
            {
                contentModified(time);
            }
            return this;
        }

        /**
         * Set content length for the file to be returned.
         *
         * @param input
         * @return
         */
        ResponseHeader contentLength(FileInputStream input)
        {
            try
            {
                contentLength(input.getChannel().size()); //$NON-NLS-1$
            }
            catch (IOException e)
            {
                Journeymap.getLogger().warn("Couldn't get content length for FileInputStream"); //$NON-NLS-1$
            }
            return this;
        }

        /**
         * Set content last=modified timestamp.
         *
         * @param timestamp
         * @return
         */
        ResponseHeader contentModified(long timestamp)
        {
            return setHeader("Last-Modified", dateFormat.format(new Date(timestamp))); //$NON-NLS-1$
        }

        /**
         * Set content length for the file to be returned.
         *
         * @param fileSize
         * @return
         */
        ResponseHeader contentLength(long fileSize)
        {
            return setHeader("Content-Length", Long.toString(fileSize)); //$NON-NLS-1$
        }

        /**
         * Set expires of data to be returned.
         *
         * @param timestamp
         * @return
         */
        ResponseHeader expires(long timestamp)
        {
            return setHeader("Expires", dateFormat.format(new Date(timestamp))); //$NON-NLS-1$
        }

        /**
         * Set MIME content type for the file to be returned. Also sets the language if
         * the type is HTML or TEXT.
         *
         * @param type
         * @return
         */
        ResponseHeader contentType(ContentType type)
        {
            if (type != null)
            {
                reply.type(type.getMime());
                if (ContentType.htmlTypes.contains(type))
                {
                    contentLanguage(Constants.getLocale());
                }
            }
            return this;
        }

        /**
         * Set language for the file to be returned.
         *
         * @param locale
         * @return
         */
        ResponseHeader contentLanguage(Locale locale)
        {
            setHeader("Content-Language", locale.toString());
            return this;
        }

        /**
         * Set the inline content filename of the data being returned.
         *
         * @param name
         * @return
         */
        ResponseHeader inlineFilename(String name)
        {
            return setHeader("Content-Disposition", "inline; filename=\"" + name + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        /**
         * Clear the object reference to the reply.
         * This really shouldn't be necessary if GC is doing its job.
         */
        void done()
        {
            this.reply = null;
        }

    }

}
