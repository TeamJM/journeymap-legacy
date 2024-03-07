package journeymap.common.kotlin.extensions

import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import java.io.InputStream

fun ResourceLocation.getResourceAsStream(): InputStream
{
    return Minecraft.getMinecraft().resourceManager.getResource(this).inputStream
}
