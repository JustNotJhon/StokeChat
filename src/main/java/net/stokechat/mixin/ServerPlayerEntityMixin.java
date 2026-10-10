package net.stokechat.mixin;

import net.minecraft.advancement.Achievement;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.stokechat.DiscordWebhook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "incrementStat(Lnet/minecraft/stat/Stat;I)V", at = @At("HEAD"))
    private void stokechat$announceStat(Stat stat, int amount, CallbackInfo ci) {
        String player = ((ServerPlayerEntity) (Object) this).getUsername();
        if (stat == Stats.DEATHS) {
            DiscordWebhook.announcePlayerDeath(player);
        } else if (stat instanceof Achievement) {
            DiscordWebhook.announceAchievement(player);
        }
    }
}