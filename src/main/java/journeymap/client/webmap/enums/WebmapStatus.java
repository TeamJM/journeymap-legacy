package journeymap.client.webmap.enums;

public enum WebmapStatus
{
    READY("ready"),
    DISABLED("disabled"),

    NO_WORLD("no_world"),
    STARTING("starting");

    private final String status;

    WebmapStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }
}
