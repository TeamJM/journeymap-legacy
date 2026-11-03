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
        if (!isMiniMapReady() || !UIManager.getInstance().isMiniMapEnabled())
        {
            return false;
        }
        return JourneymapClient.getInstance().isMapping() || !JourneymapClient.getCoreProperties().mappingEnabled.get();
    }

    /**
     * The minimap and its margins, which hold the compass and the labels, where JourneyMap's own settings put it.
     * This only works out the layout, so it does not reload the map or save settings.
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
        JourneymapClient client = JourneymapClient.getInstance();
        return client != null && client.isInitialized() && ForgeHelper.INSTANCE.getClient().thePlayer != null;
    }
}
