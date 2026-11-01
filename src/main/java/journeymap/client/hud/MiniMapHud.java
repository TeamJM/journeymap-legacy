package journeymap.client.hud;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.JourneymapClient;
import journeymap.client.ui.UIManager;
import net.minecraft.client.Minecraft;

/**
 * What another mod can ask about the minimap, safe to call at any time. To change how it draws, see
 * {@link MiniMapRenderEvent}. Client only.
 */
@SideOnly(Side.CLIENT)
public final class MiniMapHud
{
    private MiniMapHud()
    {
    }

    /**
     * Whether the player has the minimap turned on in JourneyMap. False until there is a minimap to ask, like in the
     * main menu.
     */
    public static boolean isEnabled()
    {
        return isMiniMapReady() && UIManager.getInstance().isMiniMapEnabled();
    }

    /**
     * The minimap and its margins, which hold the compass and the labels, where JourneyMap's own settings put it.
     *
     * @return left, top, right and bottom, in screen pixels and not in GUI scaled pixels. All 0 until there is a minimap
     * to ask, like in the main menu.
     */
    public static int[] getBounds()
    {
        return isMiniMapReady() ? UIManager.getInstance().getMiniMap().getBounds() : new int[4];
    }

    /**
     * Using UIManager before this is true would break it for the whole session.
     */
    private static boolean isMiniMapReady()
    {
        return Minecraft.getMinecraft().thePlayer != null && JourneymapClient.getInstance().isInitialized();
    }
}
