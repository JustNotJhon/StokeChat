package net.stokechat.mixin;

import net.stokechat.DiscordWebhook;
import net.stokechat.DiscordGateway;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftDedicatedServer.class)
public abstract class DedicatedServerMixin {
    @Inject(method = "setupServer", at = @At("RETURN"))
    private void stokechat$announceStartup(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            DiscordWebhook.announceServerStarted();
            DiscordGateway.start((MinecraftServer)(Object)this);
        }
    }
}
