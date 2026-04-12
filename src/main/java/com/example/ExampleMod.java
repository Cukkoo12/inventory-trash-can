package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.ItemStack;

public class ExampleMod implements ModInitializer {

	@Override
	public void onInitialize() {
		// İŞTE YENİ KOMUT: playC2S() yerine serverboundPlay() kullanıyoruz!
		PayloadTypeRegistry.serverboundPlay().register(TrashPayload.TYPE, TrashPayload.CODEC);

		// Sunucu bu kuryeyi aldığında ne yapacak?
		ServerPlayNetworking.registerGlobalReceiver(TrashPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				if (context.player() != null && context.player().containerMenu != null) {
					context.player().containerMenu.setCarried(ItemStack.EMPTY);
				}
			});
		});
	}
}