/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.log;

import com.google.common.util.concurrent.AtomicDouble;
import journeymap.common.Journeymap;
import net.minecraft.util.EnumChatFormatting;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for timing whatever needs to be timed.
 */
public class StatTimer
{
    public static final double NS = 1000000D;
    private static final int WARMUP_COUNT_DEFAULT = 10;
    private static final int MAX_COUNT = 1000000;
    private static final int MAX_ELAPSED_LIMIT_WARNINGS = 10;
    private static final int ELAPSED_LIMIT_DEFAULT = 1000;
    private static final Logger logger = Journeymap.getLogger();
    private static Map<String, StatTimer> timers = Collections.synchronizedMap(new HashMap<String, StatTimer>());
    private final int warmupCount;
    private final int elapsedLimit;
    private final AtomicLong counter = new AtomicLong();
    private final AtomicLong cancelCounter = new AtomicLong();
    private final AtomicDouble totalTime = new AtomicDouble();
    private final String name;
    private final boolean disposable;
    private final boolean doWarmup;
    private int elapsedLimitWarnings = MAX_ELAPSED_LIMIT_WARNINGS;
    private boolean warmup = true;
    private boolean maxed = false;
    private boolean ranTooLong = true;
    private int ranTooLongCount;
    private Long started;
    private double max = 0;
    private double min = Double.MAX_VALUE;

    /**
     * Private constructor.
     *
     * @param name
     * @param warmupCount
     */
    private StatTimer(String name, int warmupCount, int elapsedLimit, boolean disposable)
    {
        this.name = name;
        this.warmupCount = warmupCount;
        this.elapsedLimit = elapsedLimit;
        this.disposable = disposable;
        this.doWarmup = warmupCount > 0;
        this.warmup = warmupCount > 0;
    }

    /**
     * Get a timer by name.  If it hasn't been created, it will have WARMUP_COUNT_DEFAULT.
     *
     * @param name
     * @return
     */
    public synchronized static StatTimer get(String name)
    {
        return get(name, WARMUP_COUNT_DEFAULT);
    }

    /**
     * Get a timer by name.  If it hasn't been created, it will have the warmupCount value provided.
     *
     * @param name
     * @param warmupCount
     * @return
     */
    public synchronized static StatTimer get(String name, int warmupCount)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("StatTimer name required");
        }
        StatTimer timer = timers.get(name);
        if (timer == null)
        {
            timer = new StatTimer(name, warmupCount, ELAPSED_LIMIT_DEFAULT, false);
            timers.put(name, timer);
        }
        return timer;
    }

    /**
     * Get a timer by name.  If it hasn't been created, it will have the warmupCount and elapsedLimit value provided.
     *
     * @param name
     * @param warmupCount
     * @param warmupCount
     * @return
     */
    public synchronized static StatTimer get(String name, int warmupCount, int elapsedLimit)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("StatTimer name required");
        }
        StatTimer timer = timers.get(name);
        if (timer == null)
        {
            timer = new StatTimer(name, warmupCount, elapsedLimit, false);
            timers.put(name, timer);
        }
        return timer;
    }

    /**
     * Create a disposable timer with a warmupCount of 0.
     *
     * @param name
     * @return
     */
    public static StatTimer getDisposable(String name)
    {
        return new StatTimer(name, 0, ELAPSED_LIMIT_DEFAULT, true);
    }

    /**
     * Create a disposable timer with a warmupCount of 0.
     *
     * @param name
     * @return
     */
    public static StatTimer getDisposable(String name, int elapsedLimit)
    {
        return new StatTimer(name, 0, elapsedLimit, true);
    }

    /**
     * Reset all timers.
     */
    public synchronized static void resetAll()
    {
        for (StatTimer timer : timers.values())
        {
            timer.reset();
        }
    }

    /**
     * Report all timers via log file.
     */
    public synchronized static String getReport()
    {
        List<StatTimer> list = new ArrayList<StatTimer>(timers.values());
        Collections.sort(list, new Comparator<StatTimer>()
        {
            @Override
            public int compare(StatTimer o1, StatTimer o2)
            {
                return o1.name.compareTo(o2.name);
            }
        });
        StringBuffer sb = new StringBuffer();
        for (StatTimer timer : list)
        {
            if (timer.counter.get() > 0)
            {
                sb.append(LogFormatter.LINEBREAK).append(timer.getReportString());
            }
        }
        return sb.toString();
    }

    /**
     * Report all timers sorted by most time consumed
     */
    public synchronized static List<String> getReportByTotalTime(String prefix, String suffix)
    {
        List<StatTimer> list = new ArrayList<StatTimer>(timers.values());
        Collections.sort(list, new Comparator<StatTimer>()
        {
            @Override
            public int compare(StatTimer o1, StatTimer o2)
            {
                return Double.compare(o2.totalTime.get(), o1.totalTime.get());
            }
        });
        ArrayList<String> strings = new ArrayList<String>();
        for (StatTimer timer : list)
        {
            if (timer.counter.get() > 0)
            {
                strings.add(prefix + timer.getSimpleReportString() + suffix);
            }

            if (strings.size() >= 30)
            {
                break;
            }
        }
        return strings;
    }

    /**
     * Pad string s with up to n spaces.
     *
     * @param s
     * @param n
     * @return
     */
    private static String pad(Object s, int n)
    {
        return String.format("%1$-" + n + "s", s);
    }

    /**
     * Start the timer.
     *
     * @return
     */
    public StatTimer start()
    {
        synchronized (counter)
        {
            if (maxed)
            {
                return this;
            }

            if (started != null)
            {
                logger.warn(name + " is already running, cancelling first");
                this.cancel();
            }

            ranTooLong = false;

            if (counter.get() == MAX_COUNT)
            {
                maxed = true;
                logger.info(name + " hit max count, " + MAX_COUNT);
                return this;
            }

            if (warmup && counter.get() > warmupCount)
            {
                warmup = false;
                max = 0;
                min = Double.MAX_VALUE;
                counter.set(0);
                cancelCounter.set(0);
                totalTime.set(0);
                if (logger.isTraceEnabled())
                {
                    logger.debug(name + " warmup done, " + warmupCount);
                }
            }

            started = System.nanoTime();
            return this;
        }
    }

    /**
     * Stop the timer, returns elapsed time in milliseconds.
     */
    public double stop()
    {
        synchronized (counter)
        {
            if (maxed)
            {
                return 0;
            }

            if (started == null)
            {
                // If counter == 0, timer was reset while running.
                // Otherwise it's being used improperly.
                if (counter.get() > 0)
                {
                    logger.warn(name + " is not running.");
                }
                return 0;
            }

            try
            {
                final double elapsedMs = (System.nanoTime() - started) / NS;
                totalTime.getAndAdd(elapsedMs);
                counter.getAndIncrement();
                if (elapsedMs < min)
                {
                    min = elapsedMs;
                }
                if (elapsedMs > max)
                {
                    max = elapsedMs;
                }
                started = null;

                if (!warmup && elapsedMs >= elapsedLimit)
                {
                    ranTooLong = true;
                    ranTooLongCount++;
                    if (elapsedLimitWarnings > 0)
                    {
                        String msg = this.getName() + " was slow: " + elapsedMs;
                        if (--elapsedLimitWarnings == 0)
                        {
                            msg += " (Warning limit reached)";
                            logger.warn(msg);
                            // TODO: when report strings fixed to be JSON, this mess won't be needed
                            logger.warn(getReportString().replaceAll("<b>", "").replaceAll("</b>", "").trim());
                        }
                        else
                        {
                            logger.debug(msg);
                        }
                    }
                }
                return elapsedMs;
            }
            catch (Throwable t)
            {
                logger.error("Timer error: " + LogFormatter.toString(t));
                reset();
                return 0;
            }
        }
    }

    /**
     * Returns elapsed time in milliseconds.
     */
    public double elapsed()
    {
        synchronized (counter)
        {
            if (maxed || started == null)
            {
                return 0;
            }

            return (System.nanoTime() - started) / NS;
        }
    }

    /**
     * Only useful after stop();
     *
     * @return
     */
    public boolean hasReachedElapsedLimit()
    {
        return ranTooLong;
    }

    public int getElapsedLimitReachedCount()
    {
        return ranTooLongCount;
    }

    public int getElapsedLimitWarningsRemaining()
    {
        return elapsedLimitWarnings;
    }

    /**
     * Stop the timer, return simple report of results.
     *
     * @return
     */
    public String stopAndReport()
    {
        stop();
        return getSimpleReportString();
    }

    /**
     * Cancel a started timer.
     */
    public void cancel()
    {
        synchronized (counter)
        {
            started = null;
            cancelCounter.incrementAndGet();
        }
    }

    /**
     * Reset the timer.
     */
    public void reset()
    {
        synchronized (counter)
        {
            warmup = doWarmup;
            maxed = false;
            started = null;
            counter.set(0);
            cancelCounter.set(0);
            totalTime.set(0);
            elapsedLimitWarnings = MAX_ELAPSED_LIMIT_WARNINGS;
            ranTooLong = false;
            ranTooLongCount = 0;
        }
    }

    /**
     * Log the timer's stats.
     */
    public void report()
    {
        logger.info(getReportString());
    }

    /**
     * Get the timer's stats as a HTML string.
     * TODO: Yes, this is horrible, it should be in JSON
     *
     * @return
     */
    public String getReportString()
    {
        final DecimalFormat df = new DecimalFormat("###.##");
        synchronized (counter)
        {
            final long count = counter.get();
            final double total = totalTime.get();
            final double avg = total / count;
            final long cancels = cancelCounter.get();

            String report = String.format("<b>%40s:</b> Avg: %8sms, Min: %8sms, Max: %10sms, Total: %10s sec, Count: %8s, Canceled: %8s, Slow: %8s",
                    name, df.format(avg), df.format(min), df.format(max), TimeUnit.MILLISECONDS.toSeconds((long) total), count, cancels, ranTooLongCount);

            if (warmup)
            {
                report += String.format("* Warmup of %s not met", warmupCount);
            }
            if (maxed)
            {
                report += "(MAXED)";
            }

            return report;
        }
    }

    public String getLogReportString()
    {
        return EnumChatFormatting.getTextWithoutFormattingCodes(getSimpleReportString());
    }

    /**
     * Gets a simplified report of the timer stats with color formatting for Shift-F3 display.
     *
     * @return
     */
    public String getSimpleReportString()
    {
        try
        {
            final DecimalFormat df = new DecimalFormat("###.##");
            synchronized (counter)
            {
                final long count = counter.get();
                final double total = totalTime.get();
                final double avg = total / count;

                final StringBuilder sb = new StringBuilder(name);
                sb.append(EnumChatFormatting.DARK_GRAY);
                sb.append(" count ").append(EnumChatFormatting.RESET);
                sb.append(count);
                sb.append(EnumChatFormatting.DARK_GRAY);
                sb.append(" avg ").append(EnumChatFormatting.RESET);
                if (ranTooLongCount > 0)
                {
                    sb.append(EnumChatFormatting.RESET);
                }
                sb.append(df.format(avg));
                sb.append(EnumChatFormatting.DARK_GRAY);
                sb.append("ms");
                sb.append(EnumChatFormatting.RESET);
                if (maxed)
                {
                    sb.append("(MAXED)");
                }
                return sb.toString();
            }
        }
        catch (Throwable t)
        {
            return String.format("StatTimer '%s' encountered an error getting its simple report: %s", name, t);
        }
    }

    public String getName()
    {
        return name;
    }
}