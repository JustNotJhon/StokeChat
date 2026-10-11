package net.stokechat.mixin;

import net.minecraft.server.MinecraftServer;
import net.stokechat.DiscordGateway;
import net.stokechat.DiscordWebhook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void stokechat$announceShutdown(CallbackInfo ci) {
        DiscordGateway.stop();
        DiscordWebhook.announceServerStopped();
    }

    @Inject(method = "method_2036", at = @At("HEAD"), cancellable = true)
    private void stokechat$blockBotConfigOverRcon(String input, CallbackInfoReturnable<String> cir) {
        String command = input.trim();
        if (command.startsWith("/")) command = command.substring(1);
        String[] parts = command.split("\\s+", 2);
        if (parts.length > 0 && "stokechat".equalsIgnoreCase(parts[0])) {
            cir.setReturnValue("StokeChat configuration commands are only available from the local server console.");
        }
    }
}
