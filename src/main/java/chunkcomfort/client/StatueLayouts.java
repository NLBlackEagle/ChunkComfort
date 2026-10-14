package chunkcomfort.client;

import chunkcomfort.ChunkComfort;
import chunkcomfort.statue.StatueType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Candle positions per statue, loaded from assets/chunkcomfort/statues/<statue>.json:
 *   { "slots": [[x, y, z, height], ...] }
 * in model pixels (0-16) for a statue facing north; y is where the candle stands and the
 * optional height is the candle's wax height (default StatueType.CANDLE_HEIGHT).
 * Lives in client resources so resource packs can move candles or add more for extra slots.
 */
public class StatueLayouts implements IResourceManagerReloadListener {

    /** One candle spot: position (model pixels, statue facing north) and wax height in pixels. */
    public static class Slot {
        public final Vec3d pos;
        public final double height;

        Slot(Vec3d pos, double height) {
            this.pos = pos;
            this.height = height;
        }
    }

    private static final Map<StatueType, List<Slot>> LAYOUTS = new EnumMap<>(StatueType.class);

    public static List<Slot> get(StatueType type) {
        return LAYOUTS.getOrDefault(type, Collections.emptyList());
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        LAYOUTS.clear();

        for (StatueType type : StatueType.values()) {
            ResourceLocation location = new ResourceLocation(ChunkComfort.MODID, "statues/" + type.name + ".json");

            try (IResource resource = resourceManager.getResource(location);
                 Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {

                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                List<Slot> slots = new ArrayList<>();

                for (JsonElement element : json.getAsJsonArray("slots")) {
                    JsonArray pos = element.getAsJsonArray();
                    double height = pos.size() > 3 ? pos.get(3).getAsDouble() : StatueType.CANDLE_HEIGHT;
                    slots.add(new Slot(new Vec3d(pos.get(0).getAsDouble(), pos.get(1).getAsDouble(), pos.get(2).getAsDouble()), height));
                    if (slots.size() >= StatueType.MAX_SLOTS) break;
                }

                LAYOUTS.put(type, slots);

            } catch (Exception e) {
                ChunkComfort.LOGGER.warn("Could not load statue candle layout {}", location, e);
            }
        }
    }
}
