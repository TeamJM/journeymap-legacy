package journeymap.client.io;

import journeymap.client.Constants;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorldDataCleanup
{
    private WorldDataCleanup() {}

    public static void deleteJMWorldData(String worldFolderName)
    {
        if (worldFolderName == null || worldFolderName.isEmpty())
        {
            return;
        }
        try
        {
            Path jmWorldDir = Minecraft.getMinecraft().mcDataDir.toPath()
                    .resolve(Constants.SP_DATA_DIR)
                    .resolve(worldFolderName);
            if (Files.isDirectory(jmWorldDir))
            {
                FileUtils.deleteDirectory(jmWorldDir.toFile());
                Journeymap.getLogger().info("Deleted JourneyMap data for world: {}", worldFolderName);
            }
        }
        catch (IOException e)
        {
            Journeymap.getLogger().warn("Failed to delete JourneyMap data for world {}: {}", worldFolderName, e.toString());
        }
        catch (Throwable t)
        {
            Journeymap.getLogger().warn("Unexpected error deleting JourneyMap data for world {}: {}", worldFolderName, t.toString());
        }
    }
}
