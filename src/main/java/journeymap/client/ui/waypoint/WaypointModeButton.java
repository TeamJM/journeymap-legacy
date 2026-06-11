package journeymap.client.ui.waypoint;

import journeymap.client.Constants;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.model.Waypoint;
import journeymap.client.model.WaypointMode;
import journeymap.client.ui.component.Button;

public class WaypointModeButton extends Button
{
    private WaypointMode mode;

    public WaypointModeButton(Waypoint waypoint)
    {
        super("");
        this.mode = WaypointMode.from(waypoint);
        refresh();
    }

    public WaypointMode getMode()
    {
        return mode;
    }

    public void cycle()
    {
        mode = mode.next();
        refresh();
    }

    public void cyclePrevious()
    {
        mode = mode.previous();
        refresh();
    }

    public void applyTo(Waypoint waypoint)
    {
        mode.applyTo(waypoint);
    }

    @Override
    public void refresh()
    {
        displayString = Constants.getString(mode.getTranslationKey());
        fitWidth(ForgeHelper.INSTANCE.getFontRenderer());
    }
}
