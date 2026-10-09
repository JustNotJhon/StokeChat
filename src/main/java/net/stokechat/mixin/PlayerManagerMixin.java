package net.stokechat.mixin;

import net.stokechat.DiscordWebhook;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.Connection;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void stokechat$announceJoin(Connection connection, ServerPlayerEntity player, CallbackInfo ci) {
        DiscordWebhook.announcePlayerJoined(player.getUsername());
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void stokechat$announceLeave(ServerPlayerEntity player, CallbackInfo ci) {
        DiscordWebhook.announcePlayerLeft(player.getUsername());
    }
}
