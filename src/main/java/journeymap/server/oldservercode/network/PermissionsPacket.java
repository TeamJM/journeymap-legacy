/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.network;

/**
 * Created by Mysticdrew on 10/8/2014.
 */
public class PermissionsPacket /*implements IMessage*/
{

   /* private int radarAnimals;
    private int radarPlayers;
    private int radarMobs;
    private int radarVillagers;
    private int mapCaves;

    public PermissionsPacket(int radarAnimals, int radarPlayers, int radarMobs, int radarVillagers, int mapCaves) {
        this.radarAnimals = radarAnimals;
        this.radarPlayers = radarPlayers;
        this.radarMobs = radarMobs;
        this.radarVillagers = radarVillagers;
        this.mapCaves = mapCaves;
    }

    public int getRadarAnimals() {
        return radarAnimals;
    }

    public int getRadarPlayers() {
        return radarPlayers;
    }

    public int getRadarMobs() {
        return radarMobs;
    }

    public int getRadarVillagers() {
        return radarVillagers;
    }

    public int getMapCaves() {
        return mapCaves;
    }

    @Override
    public void fromBytes(ByteBuf buf){
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            ByteBufUtils.writeVarShort(buf, radarAnimals);
            ByteBufUtils.writeVarShort(buf, radarPlayers);
            ByteBufUtils.writeVarShort(buf, radarMobs);
            ByteBufUtils.writeVarShort(buf, radarVillagers);
            ByteBufUtils.writeVarShort(buf, mapCaves);
        }
        catch(Throwable t) {
            LogHelper.error("[toBytes]Failed to read message: " + t);
        }
    }

    public static class PermissionsListener implements IMessageHandler<PermissionsPacket, IMessage> {
        @Override
        public IMessage onMessage(PermissionsPacket message, MessageContext ctx) {

            String worldName = ctx.getServerHandler().playerEntity.getEntityWorld().getWorldInfo().getWorldName();
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            //ForgePacketHandler.sendPlayerWorldID(worldName, player);
            return null;
        }
    }*/
}
