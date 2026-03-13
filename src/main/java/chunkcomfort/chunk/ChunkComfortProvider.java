package chunkcomfort.chunk;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

public class ChunkComfortProvider implements ICapabilitySerializable<NBTTagCompound> {

    private final ChunkComfortData instance;

    public ChunkComfortProvider(@Nullable ChunkComfortData data) {
        this.instance = (data != null) ? data : new ChunkComfortData();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == ChunkComfortCapability.CHUNK_COMFORT_CAP;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == ChunkComfortCapability.CHUNK_COMFORT_CAP) {
            return ChunkComfortCapability.CHUNK_COMFORT_CAP.cast(instance);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return (NBTTagCompound) ChunkComfortCapability.CHUNK_COMFORT_CAP
                .getStorage()
                .writeNBT(ChunkComfortCapability.CHUNK_COMFORT_CAP, instance, null);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        ChunkComfortCapability.CHUNK_COMFORT_CAP.getStorage().readNBT(
                ChunkComfortCapability.CHUNK_COMFORT_CAP, instance, null, nbt
        );
    }

    public ChunkComfortData getData() {
        return instance;
    }
}