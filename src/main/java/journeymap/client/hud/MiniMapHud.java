package journeymap.client.hud;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.ui.UIManager;

import java.util.function.BooleanSupplier;

/**
 * Lets another mod move, resize or hide the minimap. Without hooks, the minimap draws as usual. Client only.
 */
@SideOnly(Side.CLIENT)
public final class MiniMapHud
{
    private static BooleanSupplier layoutScreenOpen = () -> false;
    private static BooleanSupplier shouldDraw = () -> true;
    private static Runnable moveToPosition = () -> {
    };

    private MiniMapHud()
    {
    }

    /**
     * @param layoutScreenOpen True while a screen is open where the minimap should still draw.
     * @param shouldDraw       False skips drawing the minimap.
     * @param moveToPosition   Called while the minimap draws, before anything is drawn, in screen pixels and not in GUI
     *                         scaled pixels. Apply a move or a resize here. JourneyMap restores it after drawing.
     */
    public static void setHooks(BooleanSupplier layoutScreenOpen, BooleanSupplier shouldDraw, Runnable moveToPosition)
    {
        MiniMapHud.layoutScreenOpen = layoutScreenOpen;
        MiniMapHud.shouldDraw = shouldDraw;
        MiniMapHud.moveToPosition = moveToPosition;
    }

    /**
     * Whether the player has the minimap turned on in JourneyMap.
     */
    public static boolean isEnabled()
    {
        return UIManager.getInstance().isMiniMapEnabled();
    }

    /**
     * The minimap and its margins, which hold the compass and the labels, where JourneyMap's own settings put it.
     *
     * @return left, top, right and bottom, in screen pixels and not in GUI scaled pixels
     */
    public static int[] getBounds()
    {
        return UIManager.getInstance().getMiniMap().getBounds();
    }

    public static boolean isLayoutScreenOpen()
    {
        return layoutScreenOpen.getAsBoolean();
    }

    public static boolean shouldDraw()
    {
        return shouldDraw.getAsBoolean();
    }

    public static void moveToPosition()
    {
        moveToPosition.run();
    }
}
