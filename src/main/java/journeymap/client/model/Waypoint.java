/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Since;
import journeymap.client.Constants;
import journeymap.client.cartography.RGB;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.render.texture.TextureCache;
import journeymap.client.render.texture.TextureImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;

import java.awt.*;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.*;

/**
 * Generic waypoint data holder
 */
public class Waypoint implements Serializable
{
    public static final int VERSION = 1;
    public static final Gson GSON = new GsonBuilder().setVersion(VERSION).create();

    protected static final String ICON_NORMAL = "waypoint-normal.png";
    protected static final String ICON_DEATH = "waypoint-death.png";

//    @Expose(serialize = false, deserialize = false)
//    protected int version = 1;

    @Since(1)
    protected String id;

    @Since(1)
    protected String name;

    @Since(1)
    protected String icon;

    @Since(1)
    protected int x;

    @Since(1)
    protected int y;

    @Since(1)
    protected int z;

    @Since(1)
    protected int r;

    @Since(1)
    protected int g;

    @Since(1)
    protected int b;

    @Since(1)
    protected boolean enable;

    @Since(1)
    protected Type type;

    @Since(1)
    protected Origin origin;

    @Since(1)
    protected TreeSet<Integer> dimensions;

    protected transient boolean readOnly;
    protected transient boolean dirty;
    protected transient Minecraft mc = ForgeHelper.INSTANCE.getClient();

    /**
     * Default constructor Required by GSON
     */
    private Waypoint()
    {
    }

    public Waypoint(Waypoint original)
    {
        this(original.name, original.x, original.y, original.z, original.enable, original.r, original.g, original.b, original.type, original.origin, original.dimensions.first(), original.dimensions);
        this.x = original.x;
        this.y = original.y;
        this.z = original.z;
    }

    public Waypoint(String name, int posX, int posY, int posZ, Color color, Type type, Integer currentDimension)
    {
        this(name, posX, posY, posZ, true, color.getRed(), color.getGreen(), color.getBlue(), type, Origin.JourneyMap, currentDimension, Arrays.asList(currentDimension));
    }

    /**
     * Main constructor.
     */
    public Waypoint(String name, int x, int y, int z, boolean enable, int red, int green, int blue, Type type, Origin origin, Integer currentDimension, Collection<Integer> dimensions)
    {
        if (name == null)
        {
            name = createName(x, z);
        }
        if (dimensions == null || dimensions.size() == 0)
        {
            dimensions = new TreeSet<Integer>();
            dimensions.add(ForgeHelper.INSTANCE.getPlayerDimension());
        }
        this.dimensions = new TreeSet<Integer>(dimensions);
        this.dimensions.add(currentDimension);

        this.name = name;
        setLocation(x, y, z, currentDimension);

        this.r = red;
        this.g = green;
        this.b = blue;
        this.enable = enable;
        this.type = type;
        this.origin = origin;

        switch (type)
        {
            case Normal:
            {
                icon = ICON_NORMAL;
                break;
            }
            case Death:
            {
                icon = ICON_DEATH;
                break;
            }
        }
    }

    public static Waypoint of(EntityPlayer player)
    {
        return at(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ), Type.Normal, ForgeHelper.INSTANCE.getPlayerDimension());
    }

    public static Waypoint at(int posX, int posY, int posZ, Type type, int dimension)
    {
        String name;
        if (type == Type.Death)
        {
            Date now = new Date();
            name = String.format("%s %s %s", Constants.getString("jm.waypoint.deathpoint"),
                    DateFormat.getTimeInstance().format(now),
                    DateFormat.getDateInstance(DateFormat.SHORT).format(now));
        }
        else
        {
            name = createName(posX, posZ);
        }
        Waypoint waypoint = new Waypoint(name, posX, posY, posZ, Color.white, type, dimension);
        waypoint.setRandomColor();
        return waypoint;
    }

    private static String createName(int x, int z)
    {
        return String.format("%s, %s", x, z);
    }

    public static Waypoint fromString(String json)
    {
        return GSON.fromJson(json, Waypoint.class);
    }

    public void setLocation(int x, int y, int z, int currentDimension)
    {
        this.x = (currentDimension == -1) ? x * 8 : x;
        this.y = y;
        this.z = (currentDimension == -1) ? z * 8 : z;
        updateId();
    }

    public String updateId()
    {
        String oldId = this.id;
        this.id = String.format("%s_%s,%s,%s", this.name, this.x, this.y, this.z);
        return oldId;
    }

    public boolean isDeathPoint()
    {
        return this.type == Type.Death;
    }

    public TextureImpl getTexture()
    {
        return isDeathPoint() ? TextureCache.instance().getDeathpoint() : TextureCache.instance().getWaypoint();
    }

    public ChunkCoordIntPair getChunkCoordIntPair()
    {
        return new ChunkCoordIntPair(x >> 4, z >> 4);
    }

    public void setRandomColor()
    {
        Random random = new Random();
        int r = random.nextInt(255);
        int g = random.nextInt(255);
        int b = random.nextInt(255);

        int min = 100;
        int max = Math.max(r, Math.max(g, b));
        if (max < min)
        {
            if (r == max)
            {
                r = min;
            }
            else if (g == max)
            {
                g = min;
            }
            else
            {
                b = min;
            }
        }

        setColor(RGB.toInteger(r, g, b));
    }

    public Integer getColor()
    {
        return RGB.toInteger(r, g, b);
    }

    public void setColor(Integer color)
    {
        int c[] = RGB.ints(color);
        this.r = c[0];
        this.g = c[1];
        this.b = c[2];
    }

    public Integer getSafeColor()
    {
        if (r + g + b >= 100)
        {
            return getColor();
        }
        return RGB.DARK_GRAY_RGB;
    }

    public Collection<Integer> getDimensions()
    {
        return this.dimensions;
    }

    public void setDimensions(Collection<Integer> dims)
    {
        this.dimensions = new TreeSet<Integer>(dims);
    }

    public boolean isTeleportReady()
    {
        return y >= 0 && this.isInPlayerDimension();
    }

    public boolean isInPlayerDimension()
    {
        return dimensions.contains(ForgeHelper.INSTANCE.getPlayerDimension());
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public int getX()
    {
        return (mc.thePlayer.dimension == -1) ? x / 8 : x;
    }

    public double getBlockCenteredX()
    {
        return getX() + .5d;
    }

    public int getY()
    {
        return y;
    }

    public double getBlockCenteredY()
    {
        return getY() + .5d;
    }

    public int getZ()
    {
        return (mc.thePlayer.dimension == -1) ? z / 8 : z;
    }

    public double getBlockCenteredZ()
    {
        return getZ() + .5d;
    }

    public Vec3 getPosition()
    {
        return ForgeHelper.INSTANCE.newVec3(getBlockCenteredX(), getBlockCenteredY(), getBlockCenteredZ());
    }

    public int getR()
    {
        return r;
    }

    public void setR(int r)
    {
        this.r = r;
    }

    public int getG()
    {
        return g;
    }

    public void setG(int g)
    {
        this.g = g;
    }

    public int getB()
    {
        return b;
    }

    public void setB(int b)
    {
        this.b = b;
    }

    public boolean isEnable()
    {
        return enable;
    }

    public void setEnable(boolean enable)
    {
        if (enable != this.enable)
        {
            this.enable = enable;
            this.dirty = true;
        }
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public Origin getOrigin()
    {
        return origin;
    }

    public void setOrigin(Origin origin)
    {
        this.origin = origin;
    }

    public String getFileName()
    {
        return id.replaceAll("[\\\\/:\"*?<>|]", "_").concat(".json");
    }

    public boolean isDirty()
    {
        return dirty;
    }

    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;
    }

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    @Override
    public String toString()
    {
        return GSON.toJson(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        Waypoint waypoint = (Waypoint) o;

        if (b != waypoint.b)
        {
            return false;
        }
        if (enable != waypoint.enable)
        {
            return false;
        }
        if (g != waypoint.g)
        {
            return false;
        }
        if (r != waypoint.r)
        {
            return false;
        }
        if (x != waypoint.x)
        {
            return false;
        }
        if (y != waypoint.y)
        {
            return false;
        }
        if (z != waypoint.z)
        {
            return false;
        }
        if (!dimensions.equals(waypoint.dimensions))
        {
            return false;
        }
        if (!icon.equals(waypoint.icon))
        {
            return false;
        }
        if (!id.equals(waypoint.id))
        {
            return false;
        }
        if (!name.equals(waypoint.name))
        {
            return false;
        }
        if (origin != waypoint.origin)
        {
            return false;
        }
        if (type != waypoint.type)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    public enum Origin
    {
        JourneyMap,
        ReiMinimap,
        VoxelMap
    }

    public enum Type
    {
        Normal,
        Death
    }
}
