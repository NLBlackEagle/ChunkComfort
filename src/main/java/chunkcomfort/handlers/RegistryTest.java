package chunkcomfort.handlers;

import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;
import chunkcomfort.registry.ComfortEffectRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.List;

public class RegistryTest {

    public static void runTest() {
        // Ensure registries are loaded
        ForgeConfigHandler.initialize();

        System.out.println("=== BlockComfortRegistry Test ===");
        for (Block block : new Block[] { Blocks.CHEST, Blocks.CRAFTING_TABLE }) {
            if (BlockComfortRegistry.contains(block)) {
                System.out.println("Block: " + block.getRegistryName() +
                        " -> Value: " + BlockComfortRegistry.get(block).getValue() +
                        ", Limit: " + BlockComfortRegistry.get(block).getLimit() +
                        ", Group: " + BlockComfortRegistry.get(block).getGroup());
            } else {
                System.out.println("Block: " + block.getRegistryName() + " not in registry");
            }
        }

        System.out.println("\n=== FireBlockRegistry Test ===");
        for (Block block : new Block[] { Blocks.TORCH, Blocks.FIRE }) {
            System.out.println("Block: " + block.getRegistryName() +
                    " is fire block? " + FireBlockRegistry.contains(block));
        }

        System.out.println("\n=== ComfortEffectRegistry Test ===");
        int[] testComforts = new int[] { -10, 10, 20, 5 };
        for (int comfort : testComforts) {
            List<PotionEffect> effects = ComfortEffectRegistry.getEffects(comfort);
            System.out.print("Comfort: " + comfort + " -> Effects: ");
            if (effects.isEmpty()) {
                System.out.println("None");
            } else {
                for (PotionEffect effect : effects) {
                    Potion potion = effect.getPotion();
                    System.out.print(potion.getRegistryName() + "(" + effect.getAmplifier() + ") ");
                }
                System.out.println();
            }
        }
    }
}