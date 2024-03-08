package journeymap.client.webmap.routes;

import cpw.mods.fml.relauncher.ReflectionHelper;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import journeymap.client.render.texture.TextureCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Map;

public class Skin
{
    public static void get(Context ctx)
    {

        Map<String, GuiPlayerInfo> netHandlerGui = ReflectionHelper.getPrivateValue(NetHandlerPlayClient.class, Minecraft.getMinecraft().getNetHandler(), "field_147310_i", "playerInfoMap");
        String username = netHandlerGui.get(ctx.queryParam("uuid")).name;
        BufferedImage img;
        if (username == null)
        {
            img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        }
        else
        {
            img = TextureCache.instance().getPlayerSkin(username).getImage();
        }
        try
        {
            ctx.contentType(ContentType.IMAGE_PNG);
            ImageIO.write(img, "png", ctx.res.getOutputStream());
            ctx.res.getOutputStream().flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
