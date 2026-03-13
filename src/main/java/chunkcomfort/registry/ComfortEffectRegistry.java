package chunkcomfort.registry;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public class ComfortEffectRegistry {

    private static final TreeMap<Integer, List<PotionEffect>> EFFECTS = new TreeMap<>();

    public static void register(int comfort, List<PotionEffect> effects) {
        EFFECTS.put(comfort, effects);
    }

    public static List<PotionEffect> getEffects(int comfort) {
        Map.Entry<Integer, List<PotionEffect>> entry = EFFECTS.floorEntry(comfort);
        return entry != null ? entry.getValue() : Collections.emptyList();
    }

    public static void clear() { EFFECTS.clear(); }

    public static void reload(String[] configLines) {
        clear();
        if (configLines == null) return;

        for (String line : configLines) {
            try {
                String[] parts = line.split(",", 2);
                if (parts.length != 2) continue;

                int comfort = Integer.parseInt(parts[0].trim());
                List<PotionEffect> effects = parsePotionList(parts[1].trim());

                register(comfort, effects);
            } catch (Exception ignored) {}
        }
    }

    private static List<PotionEffect> parsePotionList(String str) {
        List<PotionEffect> list = new ArrayList<>();
        if (!str.startsWith("[[") || !str.endsWith("]]")) return list;

        str = str.substring(2, str.length() - 2);
        String[] entries = str.split("\\],\\[");

        for (String entry : entries) {
            String[] parts = entry.split(",");
            if (parts.length != 2) continue;

            try {
                String potionId = parts[0].trim();
                int amplifier = Integer.parseInt(parts[1].trim());

                Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(potionId));
                if (potion != null) list.add(new PotionEffect(potion, 200, amplifier, false, false));
            } catch (Exception ignored) {}
        }
        return list;
    }
}