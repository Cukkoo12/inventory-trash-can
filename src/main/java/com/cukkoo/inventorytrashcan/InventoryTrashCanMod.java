package com.cukkoo.inventorytrashcan;

import com.cukkoo.inventorytrashcan.config.ModConfig;
import com.cukkoo.inventorytrashcan.network.TrashActionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

public class InventoryTrashCanMod implements ClientModInitializer {

    public static final String MOD_ID = "inventory_trash_can";
    public static ModConfig CONFIG;
    public static ItemStack lastTrashedItem = ItemStack.EMPTY;

    private static boolean deleteWasDown = false;

    @Override
    public void onInitializeClient() {
        CONFIG = ModConfig.load();

        PayloadTypeRegistry.playC2S().register(TrashActionPayload.ID, TrashActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TrashActionPayload.ID, (payload, context) -> {
            var player = context.player();
            if (player == null || player.currentScreenHandler == null) return;

            switch (payload.action()) {
                case 0 -> player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                case 1 -> {
                    player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                    Item target = Registries.ITEM.get(payload.rawItemId());
                    for (Slot slot : player.currentScreenHandler.slots) {
                        if (slot.inventory instanceof PlayerInventory
                                && slot.getStack().getItem() == target) {
                            slot.setStack(ItemStack.EMPTY);
                        }
                    }
                }
                case 2 -> player.currentScreenHandler.setCursorStack(payload.restoreStack());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            long window = client.getWindow().getHandle();
            boolean deleteDown = org.lwjgl.glfw.GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DELETE) == GLFW.GLFW_PRESS;
            boolean justPressed = deleteDown && !deleteWasDown;
            deleteWasDown = deleteDown;

            if (!justPressed) return;
            if (!(client.currentScreen instanceof InventoryScreen)) return;

            boolean shiftDown = org.lwjgl.glfw.GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                    || org.lwjgl.glfw.GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

            if (shiftDown) {
                ItemStack carried = client.player.currentScreenHandler.getCursorStack();
                if (carried.isEmpty()) return;

                int rawId = Registries.ITEM.getRawId(carried.getItem());
                ClientPlayNetworking.send(new TrashActionPayload(1, rawId, ItemStack.EMPTY));

                InventoryScreen screen = (InventoryScreen) client.currentScreen;
                int totalCount = carried.getCount();
                for (Slot slot : screen.getScreenHandler().slots) {
                    if (slot.inventory instanceof PlayerInventory && slot.hasStack()
                            && slot.getStack().getItem() == carried.getItem()) {
                        totalCount += slot.getStack().getCount();
                        slot.setStack(ItemStack.EMPTY);
                    }
                }
                ItemStack bulkTrashed = carried.copy();
                bulkTrashed.setCount(totalCount);
                lastTrashedItem = bulkTrashed;
                client.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
            } else {
                ItemStack carried = client.player.currentScreenHandler.getCursorStack();
                if (!carried.isEmpty()) {
                    lastTrashedItem = carried.copy();
                    client.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                    ClientPlayNetworking.send(new TrashActionPayload(0, 0, ItemStack.EMPTY));
                } else if (!lastTrashedItem.isEmpty()) {
                    ItemStack restored = lastTrashedItem.copy();
                    client.player.currentScreenHandler.setCursorStack(restored);
                    ClientPlayNetworking.send(new TrashActionPayload(2, 0, restored));
                    lastTrashedItem = ItemStack.EMPTY;
                }
            }
        });
    }
}
