package journeymap.client.hud;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;

/**
 * Posted on the MinecraftForge event bus before the minimap draws on the HUD, not for the preview in JourneyMap's
 * options or while one of JourneyMap's own screens is open. Cancel it to hide the minimap for this frame. Client only.
 */
@SideOnly(Side.CLIENT)
@Cancelable
public class MiniMapRenderEvent extends Event
{
    /**
     * The open screen, or null without one.
     */
    public final GuiScreen screen;

    /**
     * The minimap normally only draws with no screen or the chat open. Set to true to also draw it under this screen.
     */
    public boolean drawWhileScreenOpen;

    /**
     * How far to move the minimap, in screen pixels and not in GUI scaled pixels.
     */
    public float offsetX, offsetY;

    /**
     * How much to resize the minimap, around the top left corner of {@link MiniMapHud#getBounds()}. 1 is normal size.
     */
    public float scale = 1;

    /**
     * For Forge's event bus, which creates one to register listeners.
     */
    public MiniMapRenderEvent()
    {
        this(null);
    }

    public MiniMapRenderEvent(GuiScreen screen)
    {
        this.screen = screen;
    }
}
