package journeymap.client.ui.minimap;

import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.ui.UIManager;

/**
 * Lets players move, resize and hide the minimap in Dashboard's HUD editor. The only class using Dashboard, so only
 * call it while Dashboard is loaded.
 */
@SideOnly(Side.CLIENT)
public final class DashboardHud
{
    private static final HudElement MINIMAP = new HudElement("journeymap:minimap", DashboardHud::getDefaultBounds)
            .setActiveCheck(() -> UIManager.getInstance().isMiniMapEnabled());

    private DashboardHud()
    {
    }

    public static void register()
    {
        HudRegistry.register(MINIMAP);
    }

    /**
     * The minimap and its margins, which hold the compass and the labels. JourneyMap measures in screen pixels, the
     * HUD in GUI scaled pixels.
     */
    private static HudBounds getDefaultBounds(int screenWidth, int screenHeight)
    {
        DisplayVars dv = UIManager.getInstance().getMiniMap().getDisplayVars();
        int scale = HudRegistry.getResolution().getScaleFactor();
        int left = (dv.textureX - dv.marginX) / scale;
        int top = (dv.textureY - dv.marginY) / scale;
        int right = (dv.textureX + dv.minimapWidth + dv.marginX + scale - 1) / scale;
        int bottom = (dv.textureY + dv.minimapHeight + dv.marginY + scale - 1) / scale;
        return new HudBounds(left, top, right - left, bottom - top);
    }
}
