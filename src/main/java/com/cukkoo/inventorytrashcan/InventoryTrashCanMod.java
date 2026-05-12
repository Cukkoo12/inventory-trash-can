package com.cukkoo.inventorytrashcan;

import com.cukkoo.inventorytrashcan.config.ModConfig;
import com.cukkoo.inventorytrashcan.network.TrashNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
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
        CONFIG = ModConfig.load(FabricLoader.getInstance().getConfigDir());
        TrashNetwork.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            long window = client.getWindow().getHandle();
            boolean deleteDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DELETE) == GLFW.GLFW_PRESS;
            boolean justPressed = deleteDown && !deleteWasDown;
            deleteWasDown = deleteDown;

            if (!justPressed) return;
            if (!(client.currentScreen instanceof InventoryScreen)) return;

            boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

            if (shiftDown) {
                ItemStack carried = client.player.currentScreenHandler.getCursorStack();
                if (carried.isEmpty()) return;

                int rawId = Registries.ITEM.getRawId(carried.getItem());
                TrashNetwork.sendBulkTrash(rawId);

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
                    TrashNetwork.sendTrash();
                } else if (!lastTrashedItem.isEmpty()) {
                    ItemStack restored = lastTrashedItem.copy();
                    client.player.currentScreenHandler.setCursorStack(restored);
                    TrashNetwork.sendRestore(restored);
                    lastTrashedItem = ItemStack.EMPTY;
                }
            }
        });
    }
}
