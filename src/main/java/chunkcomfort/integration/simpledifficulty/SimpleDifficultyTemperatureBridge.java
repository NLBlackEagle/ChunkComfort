package chunkcomfort.integration.simpledifficulty;

import com.charles445.simpledifficulty.api.SDCapabilities;
import com.charles445.simpledifficulty.api.temperature.ITemperatureCapability;
import net.minecraft.entity.player.EntityPlayer;

public class SimpleDifficultyTemperatureBridge {

    public static double getTemperatureLevel(EntityPlayer player) {

        if (!SimpleDifficultyIntegration.LOADED) {
            return 0.0;
        }

        ITemperatureCapability cap = SDCapabilities.getTemperatureData(player);
        if (cap == null) {
            return 0.0;
        }

        // Map SD integer temperature to decimal
        double temp = (cap.getTemperatureLevel() - 12) * 0.1;
        return temp;
    }
}