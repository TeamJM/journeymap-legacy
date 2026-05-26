package journeymap.client.mixin.mixins;

import journeymap.client.Constants;
import journeymap.client.mixin.interfaces.GuiSelectWorldExt;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(GuiSelectWorld.class)
public abstract class MixinGuiSelectWorld extends GuiScreen implements GuiSelectWorldExt
{

    @Unique
    private boolean jm$deleteJmData = true;

    @Override
    public void jm$setShouldDeleteJmData(boolean value)
    {
        this.jm$deleteJmData = value;
    }

    @ModifyArg(
            method = "confirmClicked(ZI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/storage/ISaveFormat;deleteWorldDirectory(Ljava/lang/String;)Z"))
    private String cleanupOnDelete(String worldName)
    {
        if (this.jm$deleteJmData)
        {
            if (worldName != null && !worldName.isEmpty())
            {
                try
                {
                    Path jmWorldDir = Minecraft.getMinecraft().mcDataDir.toPath()
                            .resolve(Constants.SP_DATA_DIR)
                            .resolve(worldName);
                    if (Files.isDirectory(jmWorldDir))
                    {
                        FileUtils.deleteDirectory(jmWorldDir.toFile());
                        Journeymap.getLogger().info("Deleted JourneyMap data for world: {}", worldName);
                    }
                }
                catch (IOException e)
                {
                    Journeymap.getLogger().warn("Failed to delete JourneyMap data for world {}: {}", worldName, e);
                }
                catch (Throwable t)
                {
                    Journeymap.getLogger().warn("Unexpected error when trying to delete JourneyMap data for world {}: {}", worldName, t);
                }
            }
        }
        return worldName;
    }
}
