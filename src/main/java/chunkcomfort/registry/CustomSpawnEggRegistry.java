package chunkcomfort.registry;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class CustomSpawnEggRegistry {

    public static class Entry {
        public String itemId;
        public String nbtPath;
        public ResourceLocation entityId;
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    public static void reload(String[] config) {
        ENTRIES.clear();

        for (String line : config) {
            try {
                String[] split = line.split("=");
                ResourceLocation entity =
                        new ResourceLocation(split[0].trim());

                String[] right = split[1].split(",");
                String item = right[0].trim();
                String nbt = right[1].trim();

                Entry e = new Entry();
                e.entityId = entity;
                e.itemId = item;
                e.nbtPath = nbt;

                ENTRIES.add(e);
            } catch (Exception ignored) {}
        }
    }

    public static ResourceLocation resolve(ItemStack stack) {

        String itemId =
                stack.getItem().getRegistryName().toString();

        for (Entry entry : ENTRIES) {

            if (!entry.itemId.equals(itemId)) continue;

            if (!stack.hasTagCompound()) continue;

            String result =
                    readNBTPath(stack.getTagCompound(), entry.nbtPath);

            if (result != null &&
                    result.equals(entry.entityId.getPath())) {

                return entry.entityId;
            }
        }

        return null;
    }

    private static String readNBTPath(NBTTagCompound tag, String path) {

        String[] nodes = path.split("\\.");

        NBTTagCompound current = tag;

        for (int i = 0; i < nodes.length - 1; i++) {
            if (!current.hasKey(nodes[i])) return null;
            current = current.getCompoundTag(nodes[i]);
        }

        return current.getString(nodes[nodes.length - 1]);
    }
}