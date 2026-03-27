package chunkcomfort.integration.simpledifficulty;

import com.charles445.simpledifficulty.api.SDCapabilities;
import com.charles445.simpledifficulty.api.temperature.ITemperatureCapability;
import net.minecraft.entity.player.EntityPlayer;

public class SimpleDifficultyTemperatureBridge {

    public static double getTemperatureLevel(EntityPlayer player) {
        // ✅ Step 1: debug SD detection
        if (!SimpleDifficultyIntegration.LOADED) {
            System.out.println("[DEBUG] SimpleDifficulty not loaded. Skipping temperature checks.");
            return 0.0;
        }

        ITemperatureCapability cap = SDCapabilities.getTemperatureData(player);
        if (cap == null) {
            System.out.println("[DEBUG] Player " + player.getName() + " has no temperature capability.");
            return 0.0;
        }

        // Map SD integer temperature to decimal
        double temp = (cap.getTemperatureLevel() - 12) * 0.1;
        System.out.println("[DEBUG] Player " + player.getName() + " temperature: " + temp);

        return temp;
    }
}