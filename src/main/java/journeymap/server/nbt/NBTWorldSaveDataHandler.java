/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.nbt;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

/**
 * Created by Mysticdrew on 10/22/2014.
 */
public class NBTWorldSaveDataHandler extends WorldSavedData
{

    private NBTTagCompound data = new NBTTagCompound();
    private String tagName;

    public NBTWorldSaveDataHandler(String tagName)
    {
        super(tagName);
        this.tagName = tagName;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        data = compound.getCompoundTag(tagName);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        compound.setTag(tagName, data);
    }

    public NBTTagCompound getData()
    {
        return data;
    }
}
