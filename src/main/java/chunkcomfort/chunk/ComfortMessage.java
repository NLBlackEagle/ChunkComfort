package chunkcomfort.chunk;

import chunkcomfort.config.ForgeConfigHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;

public class ComfortMessage {

    public static void send(EntityPlayer player, int comfort) {

        List<String> validMessages = new ArrayList<>();
        List<String> fallbackMessages = new ArrayList<>();

        for (String entry : ForgeConfigHandler.server.stagedMessages) {

            try {
                String[] parts = entry.split(",", 3);

                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                String message = parts[2].trim();

                // Remove surrounding quotes
                if (message.startsWith("'") && message.endsWith("'")) {
                    message = message.substring(1, message.length() - 1);
                }

                if (min == 0 && max == 0) {
                    fallbackMessages.add(message);
                    continue;
                }

                if (comfort >= min && comfort <= max) {
                    validMessages.add(message);
                }

            } catch (Exception e) {
                System.out.println("Invalid comfort message: " + entry);
            }
        }

        String selected = null;

        if (!validMessages.isEmpty()) {
            selected = validMessages.get(
                    player.world.rand.nextInt(validMessages.size())
            );
        } else if (!fallbackMessages.isEmpty()) {
            selected = fallbackMessages.get(
                    player.world.rand.nextInt(fallbackMessages.size())
            );
        }

        if (selected != null) {
            player.sendMessage(new TextComponentTranslation(selected));
        }
    }
}