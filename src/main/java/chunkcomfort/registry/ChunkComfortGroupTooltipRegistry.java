package chunkcomfort.registry;

import net.minecraft.client.resources.I18n;

import java.util.HashSet;
import java.util.Set;

public class ChunkComfortGroupTooltipRegistry {

    private static final Set<String> VALID_GROUPS = new HashSet<>();

    public static void reload(Set<String> allGroups) {
        VALID_GROUPS.clear();

        for (String group : allGroups) {
            String key = "tooltip.chunkcomfort.hidden." + group;

            String translated = I18n.format(key);

            if (!translated.equals(key)) {
                VALID_GROUPS.add(group);
            }
        }
    }

    public static boolean hasTooltip(String group) {
        return VALID_GROUPS.contains(group);
    }
}
