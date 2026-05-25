package journeymap.client.model;

import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.forge.helper.IColorHelper;
import journeymap.client.log.StatTimer;
import journeymap.common.Journeymap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class BlockSpriteMD
{

    private static final Map<BlockSpriteMD, BlockSpriteMD> cache = new HashMap<>(256);
    private static final BlockSpriteMD NULL_SPRITE = new BlockSpriteMD(0, 0, 0, 0);
    private static final IColorHelper colorHelper = ForgeHelper.INSTANCE.getColorHelper();

    static
    {
        NULL_SPRITE.hasColor = true;
    }

    public final int x;
    public final int y;
    public final int width;
    public final int height;
    private boolean hasColor;
    private int color;

    private BlockSpriteMD(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static BlockSpriteMD get(TextureAtlasSprite sprite)
    {
        if (sprite == null)
        {
            return NULL_SPRITE;
        }
        final BlockSpriteMD spriteMD = new BlockSpriteMD(sprite.getOriginX(), sprite.getOriginY(), sprite.getIconWidth(), sprite.getIconHeight());
        return cache.computeIfAbsent(spriteMD, Function.identity());
    }

    public static void reset()
    {
        StatTimer timer = StatTimer.get("BlockSpriteMD.reset", 0, 2000);
        timer.start();

        cache.clear();
        final Set<BlockSpriteMD> all = ForgeHelper.INSTANCE.getAllRegisteredBlockSprites();

        timer.stop();
        Journeymap.getLogger().info("Built BlockSpriteMD cache ({}) : {}", all.size(), timer.getLogReportString());
    }

    public static void loadColorsFrom(Collection<BlockMD> allBlocks)
    {
        for (BlockMD blockMD : allBlocks)
        {
            if (blockMD.hasColor())
            {
                final BlockSpriteMD sprite = colorHelper.getSprite(blockMD);
                if (sprite != NULL_SPRITE) sprite.setColor(blockMD.getColor());
            }
        }
    }

    public static Collection<BlockSpriteMD> getCached()
    {
        return new ArrayList<>(cache.values());
    }

    public boolean ensureColor()
    {
        if (!this.hasColor)
        {
            this.setColor(colorHelper.getSpriteColor(this));
            return this.hasColor;
        }
        return false;
    }

    public void setColor(int color)
    {
        this.hasColor = true;
        this.color = color;
    }

    public void setColor(Integer color)
    {
        if (color == null)
        {
            this.hasColor = false;
        }
        else
        {
            this.hasColor = true;
            this.color = color;
        }
    }

    public boolean hasColor()
    {
        return hasColor;
    }

    public int getColor()
    {
        return color;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass()) return false;
        BlockSpriteMD spriteMD = (BlockSpriteMD) o;
        return x == spriteMD.x && y == spriteMD.y && width == spriteMD.width && height == spriteMD.height;
    }

    @Override
    public int hashCode()
    {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString()
    {
        return String.format("BlockSpriteMD [x:%s y:%s width:%s height:%s]", this.x, this.y, this.width, this.height);
    }
}
