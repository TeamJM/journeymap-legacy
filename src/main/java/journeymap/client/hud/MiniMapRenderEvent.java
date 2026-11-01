package journeymap.client.hud;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Posted on the MinecraftForge event bus before the minimap draws on the HUD, not for the preview in JourneyMap's
 * options. Cancel it to hide the minimap for this frame. Client only.
 */
@SideOnly(Side.CLIENT)
@Cancelable
public class MiniMapRenderEvent extends Event
{
}
