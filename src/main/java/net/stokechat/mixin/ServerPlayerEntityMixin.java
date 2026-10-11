package net.stokechat.mixin;

import net.minecraft.advancement.Achievement;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.stokechat.DiscordWebhook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "onKilled", at = @At("HEAD"))
    private void stokechat$announceDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        String deathMessage = player.getDamageTracker().method_6076().toString();
        DiscordWebhook.announcePlayerDeath(player.getUsername(), deathMessage);
    }

    @Inject(method = "incrementStat(Lnet/minecraft/stat/Stat;I)V", at = @At("HEAD"))
    private void stokechat$announceStat(Stat stat, int amount, CallbackInfo ci) {
        if (stat instanceof Achievement) {
            String player = ((ServerPlayerEntity) (Object) this).getUsername();
            DiscordWebhook.announceAchievement(player, stat.toString());
        }
    }
}