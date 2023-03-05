/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.nbt;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Created by Mysticdrew on 10/27/2014.
 */
public class WorldNbtIDSaveHandler
{
    private NBTWorldSaveDataHandler data;
    private World world;

    public WorldNbtIDSaveHandler()
    {
        world = MinecraftServer.getServer().getEntityWorld();
        data = (NBTWorldSaveDataHandler) world.perWorldStorage.loadData(NBTWorldSaveDataHandler.class, "JourneyMapWorldID");
    }


    public String getWorldID()
    {
        return getNBTWorldID();
    }


    public void setWorldID(String worldID)
    {
        saveWorldID(worldID);
    }

    private String getNBTWorldID()
    {
        if (data == null)
        {
            return createNewWorldID();
        }
        else if (data.getData().hasKey("JourneyMapWorldID"))
        {
            //LogHelper.info("World ID: " + data.getData().getString("JourneyMapWorldID"));
            return data.getData().getString("JourneyMapWorldID");
        }
        return "noWorldIDFound";
    }

    private String createNewWorldID()
    {
        String worldID = UUID.randomUUID().toString();
        data = new NBTWorldSaveDataHandler("JourneyMapWorldID");
        world.perWorldStorage.setData("JourneyMapWorldID", data);
        saveWorldID(worldID);
        //LogHelper.info("Created New World ID: " + worldID);
        return worldID;
    }

    private void saveWorldID(String worldID)
    {
        data.getData().setString("JourneyMapWorldID", worldID);
        data.markDirty();
    }
}
