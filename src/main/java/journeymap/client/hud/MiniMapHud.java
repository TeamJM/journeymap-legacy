package journeymap.client.hud;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.JourneymapClient;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.ui.UIManager;

/**
 * What another mod can ask about the minimap, safe to call from the start of the game on. Only call it on the client
 * thread. To change how it draws, see {@link MiniMapRenderEvent}.
 */
@SideOnly(Side.CLIENT)
public final class MiniMapHud
{
    private MiniMapHud()
    {
    }

    /**
     * Whether JourneyMap draws the minimap on the HUD: the player has it turned on, and mapping runs or is turned off
     * in the settings. False until there is a minimap to ask, like in the main menu. Open screens and a cancelled
     * {@link MiniMapRenderEvent} are not part of it, since those can change every frame.
     */
    public static boolean isEnabled()
    {
        return isMiniMapReady() && UIManager.getInstance().isMiniMapEnabled();
    }

    /**
     * The minimap and its margins, which hold the compass and the labels, where JourneyMap's own settings put it.
     * This only works out the layout, so it does not reload the map or save settings.
     *
     * @return left, top, right and bottom, in screen pixels and not in GUI scaled pixels. All 0 until there is a minimap
     * to ask, like in the main menu or while joining a world.
     */
    public static int[] getBounds()
    {
        return isMiniMapReady() ? UIManager.getInstance().getMiniMap().getBounds() : new int[4];
    }

    /**
     * The same point from which JourneyMap itself draws the minimap. Using UIManager earlier would set it up for the
     * wrong world.
     */
    private static boolean isMiniMapReady()
    {
        JourneymapClient client = JourneymapClient.getInstance();
        return client != null && client.isInitialized() && ForgeHelper.INSTANCE.getClient().thePlayer != null
                && client.shouldDrawMiniMap();
    }
}
