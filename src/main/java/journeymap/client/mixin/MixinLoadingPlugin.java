package journeymap.client.mixin;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@IFMLLoadingPlugin.Name("Journeymap loading plugin")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class MixinLoadingPlugin implements IFMLLoadingPlugin
{

    @Override
    public String[] getASMTransformerClass()
    {
        return null;
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
        try
        {
            Class.forName("org.spongepowered.asm.mixin.Mixins")
                    .getDeclaredMethod("addConfiguration", String.class)
                    .invoke(null, "mixins.journeymap.json");
        }
        catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e)
        {
            LogManager.getLogger("journeymap").info("Journeymap mixins won't be loaded");
        }
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
}
