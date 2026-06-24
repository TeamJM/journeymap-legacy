package journeymap.client.ui.util;

/**
 * Smooths a numeric value using exponential response over real frame time.
 */
public class SmoothDoubleState
{
    private static final double RESPONSE = 18D;
    private static final double MAX_DELTA_SECONDS = 0.05D;
    private static final double SNAP_THRESHOLD = 0.001D;
    private static final double JUMP_THRESHOLD = 6D;

    private double value;
    private long lastUpdateNanos;

    public double getValue()
    {
        return value;
    }

    public boolean isInitialized()
    {
        return lastUpdateNanos != 0L;
    }

    public void clear()
    {
        value = 0D;
        lastUpdateNanos = 0L;
    }

    public void snapTo(double target)
    {
        value = target;
        lastUpdateNanos = System.nanoTime();
    }

    public void updateTowards(double target)
    {
        updateTowards(target, RESPONSE, MAX_DELTA_SECONDS, SNAP_THRESHOLD, JUMP_THRESHOLD);
    }

    public double getScale(double target, boolean enabled)
    {
        if (!enabled)
        {
            snapTo(target);
            return 1D;
        }
        updateTowards(target);
        return Math.pow(2D, value - target);
    }

    private void updateTowards(double target, double response, double maxDeltaSeconds, double snapThreshold, double jumpThreshold)
    {
        long now = System.nanoTime();
        if (lastUpdateNanos == 0L)
        {
            value = target;
            lastUpdateNanos = now;
            return;
        }

        double deltaSeconds = Math.min((now - lastUpdateNanos) / 1_000_000_000D, maxDeltaSeconds);
        lastUpdateNanos = now;
        if (Math.abs(value - target) > jumpThreshold)
        {
            // Large jumps are intentional state changes, not animation frames to blend through.
            value = target;
        }
        else if (deltaSeconds > 0D)
        {
            double blend = 1D - Math.exp(-response * deltaSeconds);
            value += (target - value) * blend;
        }

        if (Math.abs(value - target) < snapThreshold)
        {
            value = target;
        }
    }
}
