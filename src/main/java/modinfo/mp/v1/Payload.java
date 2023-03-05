/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package modinfo.mp.v1;

import modinfo.ModInfo;
import org.apache.logging.log4j.Level;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author techbrew 2/19/14.
 */
public class Payload
{
    public static final String VERSION = "1";

    public static Comparator<Parameter> ParameterOrdinalSort = new Comparator<Parameter>()
    {
        @Override
        public int compare(Parameter o1, Parameter o2)
        {
            //return Integer.compare(o1.ordinal(), o2.ordinal());
            return o1.compareTo(o2);
        }
    };
    private TreeMap<Parameter, String> params = new TreeMap<Parameter, String>(ParameterOrdinalSort);

    public Payload(Type type)
    {
        params.put(Parameter.HitType, type.getHitName());
    }

    /**
     * URL-encodes a String without throwing an exception, which
     * should never happen anyway. If there is an exception, the value is returned
     * as-is.
     *
     * @param value
     * @return
     */
    public static String encode(String value)
    {
        try
        {
            return URLEncoder.encode(value, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            ModInfo.LOGGER.log(Level.ERROR, "Can't encode: " + value);
        }
        return value;
    }

    /**
     * Utility method to truncate a string to a length which will not exceed
     * maxBytes after it has been URL encoded.  The result can optionally be
     * returned as encoded since the work will have already been done.
     *
     * @param value        string to truncate
     * @param maxBytes     encoded byte length limit.  If null, no length change is made.
     * @param encodeResult true to return result as URL encoded
     * @return the (possibly) truncated string, plain or encoded
     */
    static String urlClamp(String value, Integer maxBytes, boolean encodeResult)
    {
        if (maxBytes == null)
        {
            return encodeResult ? encode(value) : value;
        }
        else
        {
            StringBuilder sb = new StringBuilder(value);
            String encoded = encode(sb.toString());
            int byteLength = encoded.getBytes().length;
            int offset = 0;
            while (byteLength > maxBytes)
            {
                offset = (int) Math.max(1, Math.floor((byteLength - maxBytes) / 11));
                sb.setLength(sb.length() - offset);
                encoded = encode(sb.toString());
                byteLength = encoded.getBytes().length;
            }

            return encodeResult ? encoded : sb.toString();
        }
    }

    public void put(Parameter param, String value)
    {
        params.put(param, value);
    }

    public Payload add(Map<Parameter, String> map)
    {
        params.putAll(map);
        return this;
    }

    String toUrlEncodedString()
    {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<Parameter, String>> iter = params.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<Parameter, String> entry = iter.next();
            Parameter param = entry.getKey();
            sb.append(param.pname).append("=");
            sb.append(urlClamp(entry.getValue(), param.maxBytes, true));
            if (iter.hasNext())
            {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    String toVerboseString()
    {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<Parameter, String>> iter = params.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<Parameter, String> entry = iter.next();
            Parameter param = entry.getKey();
            sb.append(param.pname).append("=");

            String value = entry.getValue();
            if (param == Parameter.TrackingId)
            {
                value = "UA-XXXXXXXX-1";
            }
            sb.append(urlClamp(value, param.maxBytes, false));
            if (iter.hasNext())
            {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    public enum Parameter
    {
        Version("v"),
        TrackingId("tid"),
        ClientId("cid"),
        HitType("t"),
        ApplicationName("an", 100),
        ApplicationVersion("av", 100),
        NonInteractionHit("ni"),
        ContentDescription("cd", 2048),
        ScreenResolution("sr", 20),
        UserLanguage("ul", 20),
        ExceptionDescription("exd", 150),
        ExceptionFatal("exf"),
        EventCategory("ec", 150),
        EventAction("ea", 500),
        EventLabel("el", 500),
        EventValue("ev"),
        CustomMetric1("cm1"); // Reserved for message count

        private String pname;
        private Integer maxBytes;

        private Parameter(String pname)
        {
            this.pname = pname;
            this.maxBytes = null;
        }

        private Parameter(String pname, int maxBytes)
        {
            this.pname = pname;
            this.maxBytes = maxBytes;
        }

        public String getParameterName()
        {
            return this.pname;
        }

        public int getMaxBytes()
        {
            return this.maxBytes;
        }
    }

    public enum Type
    {
        AppView("appview"),
        Event("event"),
        Exception("exception");

        private String hname;

        private Type(String hname)
        {
            this.hname = hname;
        }

        public String getHitName()
        {
            return this.hname;
        }
    }
}
