package net.stokechat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;

public class StokeChat implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) return;

        System.out.println("[StokeChat] onInitialize started.");
        try {
            if (DiscordWebhook.load()) {
                System.out.println("[StokeChat] Initialization completed with a webhook configured.");
            } else {
                System.out.println("[StokeChat] Initialization completed without a valid webhook.");
            }
        } catch (IOException e) {
            System.err.println("[StokeChat] Failed to read configuration ("
                + e.getClass().getSimpleName() + ").");
        }
    }
}