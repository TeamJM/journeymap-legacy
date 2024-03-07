package journeymap.client.service.webmap.kotlin.routes


import cpw.mods.fml.relauncher.ReflectionHelper
import journeymap.client.render.texture.TextureCache
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiPlayerInfo
import net.minecraft.client.network.NetHandlerPlayClient
import spark.kotlin.RouteHandler
import java.awt.image.BufferedImage
import javax.imageio.ImageIO


internal fun skinGet(handler: RouteHandler): Any
{
    val netHandlerGui = ReflectionHelper.getPrivateValue<Map<String, GuiPlayerInfo>, NetHandlerPlayClient>(
        NetHandlerPlayClient::class.java, Minecraft.getMinecraft().netHandler, "field_147310_i", "playerInfoMap"
    )
    val username = netHandlerGui[handler.params("uuid")]?.name
    val img: BufferedImage

    img = if (username == null)
    {
        BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB)
    }
    else
    {
        TextureCache.instance().getPlayerSkin(username).image

    }

    handler.response.raw().contentType = "image/png"
    ImageIO.write(img, "png", handler.response.raw().outputStream)
    handler.response.raw().outputStream.flush()

    return handler.response
}
