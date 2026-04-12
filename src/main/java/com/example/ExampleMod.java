package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.ItemStack;

public class ExampleMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Ağ paketi kuryesi kaydediliyor
        PayloadTypeRegistry.serverboundPlay().register(TrashPayload.TYPE, TrashPayload.CODEC);

        // Sunucu bu kuryeyi aldığında eşyayı siliyor
        ServerPlayNetworking.registerGlobalReceiver(TrashPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player() != null && context.player().containerMenu != null) {
                    context.player().containerMenu.setCarried(ItemStack.EMPTY);
                }
            });
        });
    }
}