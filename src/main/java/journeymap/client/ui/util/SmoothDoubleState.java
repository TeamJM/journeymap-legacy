package journeymap.client.ui.util;

public class SmoothDoubleState
{
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

    public void updateTowards(double target, double response, double maxDeltaSeconds, double snapThreshold, double jumpThreshold)
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
