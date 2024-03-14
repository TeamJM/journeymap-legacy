/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model.mod;

import journeymap.client.log.LogFormatter;
import journeymap.client.model.BlockMD;
import journeymap.client.model.ChunkMD;
import journeymap.client.model.mod.vanilla.VanillaBlockHandler;
import journeymap.common.Journeymap;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Delegates the handling of some mods' blocks to a special handler.  The handling may or may not be related
 * to how the blocks are colored on the map.  For example, a certain block might trigger creation of a waypoint.
 */
public class ModBlockDelegate
{
    private static Logger logger = Journeymap.getLogger();
    private final List<IModBlockHandler> handlers;

    /**
     * Register special block handlers when this class is initialized.
     */
    public ModBlockDelegate()
    {
        handlers = Arrays.asList(
                new VanillaBlockHandler(),
                new CarpentersBlocks.CommonHandler(),
                new TerraFirmaCraft.TfcBlockHandler(),
                new Miscellaneous.CommonHandler(),
                new BiomesOPlenty());
    }

    /**
     * Provide special handling of a block in-situ when encountered during a mapping task.
     * The block returned will be used to color that spot on the map.
     */
    public static BlockMD handleBlock(ChunkMD chunkMD, final BlockMD blockMD, int localX, int y, int localZ)
    {
        BlockMD delegatedBlockMD = null;
        try
        {
            IModBlockHandler handler = blockMD.getModBlockHandler();
            if (handler != null)
            {
                delegatedBlockMD = handler.handleBlock(chunkMD, blockMD, localX, y, localZ);
            }
            else
            {
                blockMD.setModBlockHandler(null);
            }
        }
        catch (Throwable t)
        {
            String message = String.format("Error handling block '%s': %s", blockMD, LogFormatter.toString(t));
            logger.error(message);
        }
        if (delegatedBlockMD == null)
        {
            delegatedBlockMD = blockMD;
        }
        return delegatedBlockMD;
    }

    /**
     * Call handlers to initialize their blocks' flags with the cache.
     */
    public void initialize(BlockMD blockMD)
    {
        for (IModBlockHandler handler : handlers)
        {
            initialize(handler, blockMD);
        }
    }

    /**
     * Initialize a IModBlockHandler and register blocks to be handled by it.
     */
    private void initialize(IModBlockHandler handler, BlockMD blockMD)
    {
        try
        {
            boolean specialHandling = handler.initialize(blockMD);
            if (specialHandling)
            {
                logger.info(String.format("Registered IModBlockHandler %s for: '%s'.",
                        handler.getClass().getName(),
                        blockMD));
            }
        }
        catch (Throwable t)
        {
            String message = String.format("Couldn't initialize IModBlockHandler '%s': %s",
                    handler.getClass(),
                    LogFormatter.toString(t));
            logger.error(message);
            return;
        }


    }

    /**
     * Interface for a class that initializes block flags for a specific mod, and/or
     * does special block handling during mapping.
     */
    public interface IModBlockHandler
    {
        /**
         * Provide Block UIDs that will be registered with as needing a special handler.
         * If return is true, it should be registered for handleBlock
         */
        boolean initialize(BlockMD blockMD);

        /**
         * Provide special handling of a block in-situ when encountered during a mapping task.
         * The block returned will be used to color that spot on the map.
         */
        BlockMD handleBlock(ChunkMD chunkMD, BlockMD blockMD, int localX, int y, int localZ);
    }

    /**
     * Interface for a class that determines how block colors are derived during mapping.
     */
    public interface IModBlockColorHandler
    {
        Integer getBlockColor(ChunkMD chunkMD, BlockMD blockMD, int globalX, int y, int globalZ);

        Integer getTextureColor(BlockMD blockMD);
    }
}
