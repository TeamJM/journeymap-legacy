/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.service;

import com.google.common.io.CharStreams;
import journeymap.client.data.DataCache;
import journeymap.client.log.JMLogger;
import journeymap.client.log.LogFormatter;
import journeymap.client.log.StatTimer;
import se.rupy.http.Event;

import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Serve debug goodness
 *
 * @author techbrew
 */
public class DebugService extends FileService
{

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public DebugService()
    {
    }

    @Override
    public String path()
    {
        return "/debug"; //$NON-NLS-1$
    }

    /**
     * Serve it.
     */
    @Override
    public void filter(Event event) throws Event, Exception
    {
        ResponseHeader.on(event).contentType(ContentType.html).noCache();

        // TODO:  JSON this stuff and don't be a html-generating loser.

        StringBuilder sb = new StringBuilder();
        sb.append(LogFormatter.LINEBREAK).append("<div id='accordion'>");

        sb.append(LogFormatter.LINEBREAK).append("<h1>Performance Metrics</h1>");
        sb.append(LogFormatter.LINEBREAK).append("<div><b>Stat Timers:</b><pre>").append(StatTimer.getReport()).append("</pre>");
        sb.append(LogFormatter.LINEBREAK).append(DataCache.instance().getDebugHtml()).append("</div>");

        sb.append(LogFormatter.LINEBREAK).append("<h1>Properties</h1><div>");
        sb.append(LogFormatter.LINEBREAK).append(JMLogger.getPropertiesSummary().replaceAll(LogFormatter.LINEBREAK, "<p>")).append("</div>");

        sb.append(LogFormatter.LINEBREAK).append("</div> <!-- /accordion -->");

        String debug = null;

        // Use wrapper from file
        InputStream debugHtmlStream = getStream("/debug.html", null);
        if (debugHtmlStream != null)
        {
            String debugHtml = CharStreams.toString(new InputStreamReader(debugHtmlStream, "UTF-8"));
            debug = debugHtml.replace("<output/>", sb.toString());
        }
        else
        {
            // Uh oh
            debug = sb.toString();
        }
        gzipResponse(event, debug);
    }
}
