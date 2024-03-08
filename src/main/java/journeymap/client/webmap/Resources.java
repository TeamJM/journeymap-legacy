package journeymap.client.webmap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public class Resources
{
    public static InputStream getResourceAsStream(ResourceLocation resourceLocation) throws IOException
    {
        return Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation).getInputStream();
    }
}
