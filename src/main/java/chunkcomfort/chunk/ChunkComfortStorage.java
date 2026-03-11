package chunkcomfort.chunk;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class ChunkComfortStorage implements IStorage<ChunkComfortData> {

    @Override
    public NBTTagCompound writeNBT(Capability<ChunkComfortData> capability, ChunkComfortData instance, net.minecraft.util.EnumFacing side) {

        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("comfortScore", instance.comfortScore);
        tag.setInteger("fireCount", instance.fireCount);
        tag.setBoolean("initialized", instance.initialized);

        NBTTagCompound groupsTag = new NBTTagCompound();

        for (String groupName : instance.groups.keySet()) {

            ChunkComfortData.GroupData gd = instance.groups.get(groupName);

            NBTTagCompound gdTag = new NBTTagCompound();

            gdTag.setInteger("limit", gd.limit);
            gdTag.setInteger("currentScore", gd.currentScore);

            NBTTagCompound countsTag = new NBTTagCompound();

            for (Integer value : gd.counts.keySet()) {
                countsTag.setInteger(String.valueOf(value), gd.counts.get(value));
            }

            gdTag.setTag("counts", countsTag);

            groupsTag.setTag(groupName, gdTag);
        }

        tag.setTag("groups", groupsTag);

        return tag;
    }

    @Override
    public void readNBT(Capability<ChunkComfortData> capability, ChunkComfortData instance, net.minecraft.util.EnumFacing side, net.minecraft.nbt.NBTBase nbt) {

        if (!(nbt instanceof NBTTagCompound)) return;

        NBTTagCompound tag = (NBTTagCompound) nbt;

        instance.comfortScore = tag.getInteger("comfortScore");
        instance.fireCount = tag.getInteger("fireCount");
        instance.initialized = tag.getBoolean("initialized");

        instance.groups.clear();

        NBTTagCompound groupsTag = tag.getCompoundTag("groups");

        for (String groupName : groupsTag.getKeySet()) {

            NBTTagCompound gdTag = groupsTag.getCompoundTag(groupName);

            ChunkComfortData.GroupData gd = new ChunkComfortData.GroupData();

            gd.limit = gdTag.getInteger("limit");
            gd.currentScore = gdTag.getInteger("currentScore");

            NBTTagCompound countsTag = gdTag.getCompoundTag("counts");

            for (String valueKey : countsTag.getKeySet()) {
                gd.counts.put(Integer.parseInt(valueKey), countsTag.getInteger(valueKey));
            }

            instance.groups.put(groupName, gd);
        }
    }
}