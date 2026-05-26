package journeymap.client.mixin.mixins;

import journeymap.client.io.WorldDataCleanup;
import journeymap.client.mixin.api.IGuiSelectWorldDeleteFlag;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiSelectWorld.class)
public abstract class MixinGuiSelectWorld extends GuiScreen implements IGuiSelectWorldDeleteFlag
{
    @Unique
    private boolean journeymap$deleteJmData = true;

    @Override
    public boolean journeymap$shouldDeleteJmData()
    {
        return this.journeymap$deleteJmData;
    }

    @Override
    public void journeymap$setShouldDeleteJmData(boolean value)
    {
        this.journeymap$deleteJmData = value;
    }

    @ModifyArg(
            method = "confirmClicked(ZI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/storage/ISaveFormat;deleteWorldDirectory(Ljava/lang/String;)Z"
            )
    )
    private String journeymap$cleanupOnDelete(String worldFolderName)
    {
        if (this.journeymap$deleteJmData)
        {
            WorldDataCleanup.deleteJMWorldData(worldFolderName);
        }
        return worldFolderName;
    }
}
