package com.cukkoo.inventorytrashcan;

import com.cukkoo.inventorytrashcan.config.ModConfig;
import com.cukkoo.inventorytrashcan.network.TrashMessage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lwjgl.glfw.GLFW;

@Mod(InventoryTrashCanMod.MOD_ID)
public class InventoryTrashCanMod {

    public static final String MOD_ID = "inventory_trash_can";
    public static ModConfig CONFIG;
    public static ItemStack lastTrashedItem = ItemStack.EMPTY;

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "trash"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean deleteWasDown = false;

    public InventoryTrashCanMod() {
        CONFIG = ModConfig.load(FMLPaths.CONFIGDIR.get());

        CHANNEL.registerMessage(0, TrashMessage.class,
                TrashMessage::encode,
                TrashMessage::decode,
                TrashMessage::handle
        );
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            long window = client.getWindow().getWindow();
            boolean deleteDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DELETE);
            boolean justPressed = deleteDown && !deleteWasDown;
            deleteWasDown = deleteDown;

            if (!justPressed) return;
            if (!(client.screen instanceof InventoryScreen)) return;

            boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

            if (shiftDown) {
                ItemStack carried = client.player.containerMenu.getCarried();
                if (carried.isEmpty()) return;

                int rawId = BuiltInRegistries.ITEM.getId(carried.getItem());
                CHANNEL.sendToServer(new TrashMessage(1, rawId, ItemStack.EMPTY));

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
                    CHANNEL.sendToServer(new TrashMessage(0, 0, ItemStack.EMPTY));
                } else if (!lastTrashedItem.isEmpty()) {
                    ItemStack restored = lastTrashedItem.copy();
                    client.player.containerMenu.setCarried(restored);
                    CHANNEL.sendToServer(new TrashMessage(2, 0, restored));
                    lastTrashedItem = ItemStack.EMPTY;
                }
            }
        }
    }
}
