package journeymap.client.mixin.mixins;

import cpw.mods.fml.client.config.GuiCheckBox;
import journeymap.client.Constants;
import journeymap.client.mixin.interfaces.GuiSelectWorldExt;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.util.EnumChatFormatting;
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
    private GuiCheckBox jm$checkbox;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void addJmCheckbox(CallbackInfo ci)
    {
        if (this.parentScreen instanceof GuiSelectWorldExt)
        {
            boolean initial = ((GuiSelectWorldExt) this.parentScreen).jm$shouldDeleteJmData();
            this.jm$checkbox = new GuiCheckBox(
                    -164816186,
                    this.width / 2 - 80,
                    110,
                    " " + EnumChatFormatting.RED + Constants.getString("jm.common.deleteworld_text"),
                    initial
            );
            this.buttonList.add(this.jm$checkbox);
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void onJmCheckboxClick(GuiButton button, CallbackInfo ci)
    {
        if (this.jm$checkbox != null && button == this.jm$checkbox)
        {
            ((GuiSelectWorldExt) this.parentScreen).jm$setShouldDeleteJmData(this.jm$checkbox.isChecked());
            ci.cancel();
        }
    }
}
