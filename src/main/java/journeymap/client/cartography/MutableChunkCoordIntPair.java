package journeymap.client.cartography;

import net.minecraft.world.ChunkCoordIntPair;

public class MutableChunkCoordIntPair extends ChunkCoordIntPair {

    public MutableChunkCoordIntPair(int p_i1947_1_, int p_i1947_2_) {
        super(p_i1947_1_, p_i1947_2_);

    }

    public MutableChunkCoordIntPair setChunkXPos(int chunkXPos){
        this.chunkXPos = chunkXPos;
        return this;
    }
    public MutableChunkCoordIntPair setChunkZPos(int chunkZPos){
        this.chunkZPos = chunkZPos;
        return this;
    }
}
