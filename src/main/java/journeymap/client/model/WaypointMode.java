package journeymap.client.model;

public enum WaypointMode
{
    OPEN("jm.waypoint.mode_open"),
    CLOSED("jm.waypoint.mode_closed"),
    TEMPORARY("jm.waypoint.mode_temporary"),
    DESTINATION("jm.waypoint.mode_destination");

    private final String translationKey;

    WaypointMode(String translationKey)
    {
        this.translationKey = translationKey;
    }

    public static WaypointMode from(Waypoint waypoint)
    {
        if (waypoint.isTemporary())
        {
            return TEMPORARY;
        }
        if (waypoint.isDestination())
        {
            return DESTINATION;
        }
        if (waypoint.getVisibility() == WaypointVisibility.DISABLED)
        {
            return CLOSED;
        }
        return OPEN;
    }

    public String getTranslationKey()
    {
        return translationKey;
    }

    public WaypointMode next()
    {
        switch (this)
        {
            case OPEN:
                return CLOSED;
            case CLOSED:
                return TEMPORARY;
            case TEMPORARY:
                return DESTINATION;
            default:
                return OPEN;
        }
    }

    public WaypointMode previous()
    {
        switch (this)
        {
            case OPEN:
                return DESTINATION;
            case CLOSED:
                return OPEN;
            case TEMPORARY:
                return CLOSED;
            default:
                return TEMPORARY;
        }
    }

    public void applyTo(Waypoint waypoint)
    {
        switch (this)
        {
            case OPEN:
                waypoint.setVisibility(WaypointVisibility.ENABLED);
                waypoint.setLifecycle(WaypointLifecycle.PERSISTENT);
                break;
            case CLOSED:
                waypoint.setVisibility(WaypointVisibility.DISABLED);
                waypoint.setLifecycle(WaypointLifecycle.PERSISTENT);
                break;
            case TEMPORARY:
                waypoint.setVisibility(WaypointVisibility.ENABLED);
                waypoint.setLifecycle(WaypointLifecycle.TEMPORARY);
                break;
            case DESTINATION:
                waypoint.setVisibility(WaypointVisibility.ENABLED);
                waypoint.setLifecycle(WaypointLifecycle.DESTINATION);
                break;
            default:
                break;
        }
    }
}
