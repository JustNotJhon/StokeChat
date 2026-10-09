package net.stokechat.mixin;

import net.stokechat.DiscordWebhook;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.server.ServerPacketListener;
import net.minecraft.text.ChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPacketListener.class)
public abstract class StokeChatServerPacketListenerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void stokechat$onChat(ChatMessageS2CPacket packet, CallbackInfo ci) {
        String msg = packet.message;
        String username = this.player.getUsername();
        if (msg != null && DiscordWebhook.handleWebhookCommand(this.player, msg)) {
            ci.cancel();
            return;
        }

        if (msg == null) {
            System.out.println("[StokeChat] Chat intercepted: player=" + username
                + ", message is null; ignored.");
            return;
        }

        msg = msg.trim();
        System.out.println("[StokeChat] Chat intercepted: player=" + username
            + ", characters=" + msg.length() + ".");
        if (msg.isEmpty()) {
            System.out.println("[StokeChat] Empty message; ignored.");
            return;
        }
        if (msg.startsWith("/")) {
            System.out.println("[StokeChat] Command ignored by the chat forwarder.");
            return;
        }

        DiscordWebhook.send(username, msg);
    }
}