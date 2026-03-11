package chunkcomfort.chunk;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

public class ChunkComfortProvider implements ICapabilitySerializable<NBTTagCompound> {

    private ChunkComfortData instance;

    public ChunkComfortProvider(@Nullable ChunkComfortData data) {

        this.instance = (data != null) ? data : new ChunkComfortData();

        for (ChunkComfortData.GroupData gd : instance.groups.values()) {
            gd.counts.clear();
            gd.currentScore = 0;
            gd.limit = 0;
        }

        instance.comfortScore = 0;
        instance.fireCount = 0;
        instance.initialized = true;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == ChunkComfortCapability.CHUNK_COMFORT_CAP;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {

        if (capability == null) {
            System.out.println("[ChunkComfort] getCapability: capability is null");
        }

        if (ChunkComfortCapability.CHUNK_COMFORT_CAP == null) {
            System.out.println("[ChunkComfort] getCapability: CHUNK_COMFORT_CAP is null");
        }

        if (instance == null) {
            System.out.println("[ChunkComfort] getCapability: instance is null");
        }

        if (capability == ChunkComfortCapability.CHUNK_COMFORT_CAP) {
            return ChunkComfortCapability.CHUNK_COMFORT_CAP.cast(instance);
        }

        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {

        if (instance == null) {
            System.out.println("[ChunkComfort] serializeNBT: instance is null!");
            instance = new ChunkComfortData();
        }

        if (ChunkComfortCapability.CHUNK_COMFORT_CAP == null) {
            System.out.println("[ChunkComfort] serializeNBT: CHUNK_COMFORT_CAP is null!");
            return new NBTTagCompound();
        }

        if (ChunkComfortCapability.CHUNK_COMFORT_CAP.getStorage() == null) {
            System.out.println("[ChunkComfort] serializeNBT: capability storage is null!");
            return new NBTTagCompound();
        }

        try {
            return (NBTTagCompound) ChunkComfortCapability.CHUNK_COMFORT_CAP
                    .getStorage()
                    .writeNBT(ChunkComfortCapability.CHUNK_COMFORT_CAP, instance, null);
        } catch (Exception e) {

            System.out.println("[ChunkComfort] serializeNBT: Exception while writing NBT!");
            e.printStackTrace();
            return new NBTTagCompound();
        }
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {

        if (instance == null) {
            instance = new ChunkComfortData();
        }

        ChunkComfortCapability.CHUNK_COMFORT_CAP
                .getStorage()
                .readNBT(ChunkComfortCapability.CHUNK_COMFORT_CAP, instance, null, nbt);
    }
}