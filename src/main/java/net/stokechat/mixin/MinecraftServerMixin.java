package net.stokechat.mixin;

import net.minecraft.server.MinecraftServer;
import net.stokechat.DiscordGateway;
import net.stokechat.DiscordWebhook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void stokechat$announceShutdown(CallbackInfo ci) {
        DiscordGateway.stop();
        DiscordWebhook.announceServerStopped();
    }

}
