package chunkcomfort.network;

import chunkcomfort.api.ICanBeHidden;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncHiddenEffectHandler implements IMessageHandler<PacketSyncHiddenEffect, IMessage> {

    @Override
    public IMessage onMessage(PacketSyncHiddenEffect message, MessageContext ctx) {

        Minecraft.getMinecraft().addScheduledTask(() -> {

            Entity entity = Minecraft.getMinecraft().world.getEntityByID(message.entityId);

            if (entity instanceof EntityPlayer) {

                EntityPlayer player = (EntityPlayer) entity;

                Potion potion = Potion.getPotionById(message.potionId);

                PotionEffect effect = player.getActivePotionEffect(potion);

                if (effect != null) {
                    ((ICanBeHidden) effect).chunkcomfort$setHidden(message.hidden);
                }
            }
        });

        return null;
    }
}