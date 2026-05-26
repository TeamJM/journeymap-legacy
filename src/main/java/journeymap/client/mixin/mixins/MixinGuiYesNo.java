package journeymap.client.mixin.mixins;

import cpw.mods.fml.client.config.GuiCheckBox;
import journeymap.client.Constants;
import journeymap.client.mixin.api.IGuiSelectWorldDeleteFlag;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiYesNo.class)
public abstract class MixinGuiYesNo extends GuiScreen
{
    @Shadow
    protected GuiYesNoCallback parentScreen;

    @Unique
    private GuiCheckBox journeymap$checkbox;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void journeymap$addJmCheckbox(CallbackInfo ci)
    {
        if (!(this.parentScreen instanceof GuiSelectWorld))
        {
            return;
        }
        boolean initial = ((IGuiSelectWorldDeleteFlag) this.parentScreen).journeymap$shouldDeleteJmData();
        this.journeymap$checkbox = new GuiCheckBox(
                0, // id unused: we match by reference in actionPerformed
                this.width / 2 - 80,
                110,
                " " + Constants.getString("jm.common.deleteworld_text"),
                initial
        );
        this.buttonList.add(this.journeymap$checkbox);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void journeymap$onJmCheckboxClick(GuiButton button, CallbackInfo ci)
    {
        if (this.journeymap$checkbox != null && button == this.journeymap$checkbox)
        {
            ((IGuiSelectWorldDeleteFlag) this.parentScreen)
                    .journeymap$setShouldDeleteJmData(this.journeymap$checkbox.isChecked());
            ci.cancel();
        }
    }
}
