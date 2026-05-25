package journeymap.client.mixin.mixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinTestMinecraft
{

    @Inject(method = "startGame", at = @At("RETURN"))
    private void inej(CallbackInfo ci)
    {
        System.out.println("Hello World from journeymap mixins!!");
        System.out.println("Hello World from journeymap mixins!!");
        System.out.println("Hello World from journeymap mixins!!");
        System.out.println("Hello World from journeymap mixins!!");
        System.out.println("Hello World from journeymap mixins!!");
        System.out.println("Hello World from journeymap mixins!!");
    }
}
