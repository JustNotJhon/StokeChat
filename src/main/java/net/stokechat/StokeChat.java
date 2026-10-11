package net.stokechat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class StokeChat implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) return;

        System.out.println("[StokeChat] Server integration initialized.");
    }
}