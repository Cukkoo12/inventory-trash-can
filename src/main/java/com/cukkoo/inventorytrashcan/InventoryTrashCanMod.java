package com.cukkoo.inventorytrashcan;

import com.cukkoo.inventorytrashcan.config.ModConfig;
import com.cukkoo.inventorytrashcan.network.TrashActionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class InventoryTrashCanMod implements ClientModInitializer {

    public static final String MOD_ID = "inventory_trash_can";
    public static ModConfig CONFIG;

    public static ItemStack lastTrashedItem = ItemStack.EMPTY;

    private static boolean deleteWasDown = false;

    @Override
    public void onInitializeClient() {
        CONFIG = ModConfig.load();

        PayloadTypeRegistry.serverboundPlay().register(TrashActionPayload.TYPE, TrashActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TrashActionPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                if (player == null || player.containerMenu == null) return;

                switch (payload.action()) {
                    case 0 -> player.containerMenu.setCarried(ItemStack.EMPTY);
                    case 1 -> {
                        player.containerMenu.setCarried(ItemStack.EMPTY);
                        Item target = BuiltInRegistries.ITEM.byId(payload.rawItemId());
                        for (Slot slot : player.containerMenu.slots) {
                            if (slot.container instanceof Inventory
                                    && slot.getItem().getItem() == target) {
                                slot.set(ItemStack.EMPTY);
                            }
                        }
                    }
                    case 2 -> player.containerMenu.setCarried(payload.restoreStack());
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean deleteDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_DELETE);
            boolean justPressed = deleteDown && !deleteWasDown;
            deleteWasDown = deleteDown;

            if (!justPressed) return;
            if (!(client.screen instanceof InventoryScreen)) return;

            boolean shiftDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);

            if (shiftDown) {
                ItemStack carried = client.player.containerMenu.getCarried();
                if (carried.isEmpty()) return;

                int rawId = BuiltInRegistries.ITEM.getId(carried.getItem());
                ClientPlayNetworking.send(new TrashActionPayload(1, rawId, ItemStack.EMPTY));

                InventoryScreen screen = (InventoryScreen) client.screen;
                int totalCount = carried.getCount();
                for (Slot slot : screen.getMenu().slots) {
                    if (slot.container instanceof Inventory && slot.hasItem()
                            && slot.getItem().getItem() == carried.getItem()) {
                        totalCount += slot.getItem().getCount();
                        slot.set(ItemStack.EMPTY);
                    }
                }
                ItemStack bulkTrashed = carried.copy();
                bulkTrashed.setCount(totalCount);
                lastTrashedItem = bulkTrashed;
                client.player.containerMenu.setCarried(ItemStack.EMPTY);
            } else {
                ItemStack carried = client.player.containerMenu.getCarried();
                if (!carried.isEmpty()) {
                    lastTrashedItem = carried.copy();
                    client.player.containerMenu.setCarried(ItemStack.EMPTY);
                    ClientPlayNetworking.send(new TrashActionPayload(0, 0, ItemStack.EMPTY));
                } else if (!lastTrashedItem.isEmpty()) {
                    ItemStack restored = lastTrashedItem.copy();
                    client.player.containerMenu.setCarried(restored);
                    ClientPlayNetworking.send(new TrashActionPayload(2, 0, restored));
                    lastTrashedItem = ItemStack.EMPTY;
                }
            }
        });
    }
}
