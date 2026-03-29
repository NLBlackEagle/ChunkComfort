package chunkcomfort.registry;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class FireSourceItemRegistry {

    private static final Set<Item> FIRE_SOURCE_ITEMS = new HashSet<>();

    public static void reload(String[] fireSourceItems) {

        FIRE_SOURCE_ITEMS.clear();

        if (fireSourceItems == null) return;

        for (String name : fireSourceItems) {

            if (name == null || name.trim().isEmpty()) continue;

            try {
                ResourceLocation id = new ResourceLocation(name.trim());

                // Check if the item actually exists
                if (!Item.REGISTRY.containsKey(id)) continue;

                Item item = Item.REGISTRY.getObject(id);

                // Reject invalid or air items
                if (item == null || item == Items.AIR) continue;

                FIRE_SOURCE_ITEMS.add(item);

            } catch (Exception ignored) {
                // optionally log invalid item
            }
        }
    }

    public static boolean isFireSourceItem(Item item) {
        return item != null && FIRE_SOURCE_ITEMS.contains(item);
    }

    public static Set<Item> getAll() {
        return new HashSet<>(FIRE_SOURCE_ITEMS);
    }
}