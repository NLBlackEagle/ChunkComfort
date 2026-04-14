package chunkcomfort.mixin.vanilla;

import chunkcomfort.chunk.ChunkBossState;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketUpdateBossInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class BossBarMixin {

    @Inject(method = "handleUpdateBossInfo", at = @At("HEAD"))
    private void onBossInfo(SPacketUpdateBossInfo packet, CallbackInfo ci) {

        switch (packet.getOperation()) {

            case ADD:
                ChunkBossState.clientBossBarActive = true;
                break;

            case REMOVE:
                ChunkBossState.clientBossBarActive = false;
                break;

            default:
                break;
        }
    }
}