package net.stokechat.mixin;

import net.stokechat.DiscordWebhook;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandRegistry.class)
public abstract class CommandRegistryMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void stokechat$setWebhook(CommandSource source, String input, CallbackInfoReturnable<Integer> cir) {
        if (DiscordWebhook.handleWebhookCommand(source, input)) {
            cir.setReturnValue(1);
        }
    }
}
