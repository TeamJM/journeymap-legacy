/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.log;

import journeymap.client.Constants;
import journeymap.client.JourneymapClient;
import journeymap.common.Journeymap;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class LogFormatter
{
    public static final String LINEBREAK = System.getProperty("line.separator");

    private static int OutOfMemoryWarnings = 0;
    private static int LinkageErrorWarnings = 0;

    public LogFormatter()
    {
        super();
    }

    public static String toString(Throwable thrown)
    {
        checkErrors(thrown);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        thrown.printStackTrace(ps);
        ps.flush();
        return baos.toString();
    }

    private static void checkErrors(Throwable thrown)
    {
        int maxRecursion = 5;
        if (thrown != null && OutOfMemoryWarnings < 5 && LinkageErrorWarnings < 5)
        {
            while (thrown != null && maxRecursion > 0)
            {
                if (thrown instanceof StackOverflowError)
                {
                    return;
                }
                else if (thrown instanceof OutOfMemoryError)
                {
                    OutOfMemoryWarnings++;
                    ChatLog.announceI18N("jm.common.memory_warning", thrown.toString());
                    thrown.printStackTrace(System.err);
                    break;
                }
                else
                {
                    if (thrown instanceof LinkageError)
                    {
                        LinkageErrorWarnings++;
                        String error = Constants.getString("jm.error.compatability", JourneymapClient.MOD_NAME, Journeymap.FORGE_VERSION);
                        thrown.printStackTrace(System.err);
                        ChatLog.announceError(error);
                        thrown.printStackTrace(System.err);
                        break;
                    }
                    else
                    {
                        if (thrown instanceof Exception)
                        {
                            thrown = ((Exception) thrown).getCause();
                            maxRecursion--;
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a String of the stacktrace only up to the same method which calls this one.
     *
     * @param t
     * @return
     */
    public static String toPartialString(Throwable t)
    {
        StringBuilder sb = new StringBuilder(t.toString());
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        for (StackTraceElement ste : t.getStackTrace())
        {
            sb.append("\n\tat " + ste);
            if (ste.getClassName().equals(caller.getClassName()) && ste.getMethodName().equals(caller.getMethodName()))
            {
                break;
            }
        }
        return sb.toString();
    }
}
