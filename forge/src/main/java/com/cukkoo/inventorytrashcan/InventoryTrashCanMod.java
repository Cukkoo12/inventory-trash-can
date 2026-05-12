package com.cukkoo.inventorytrashcan;

import com.cukkoo.inventorytrashcan.config.ModConfig;
import com.cukkoo.inventorytrashcan.network.TrashActionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(InventoryTrashCanMod.MOD_ID)
public class InventoryTrashCanMod {

    public static final String MOD_ID = "inventory_trash_can";
    public static ModConfig CONFIG;
    public static ItemStack lastTrashedItem = ItemStack.EMPTY;

    public InventoryTrashCanMod() {
        CONFIG = ModConfig.load();

        ChannelBuilder.named(Identifier.fromNamespaceAndPath(MOD_ID, "trash"))
                .networkProtocolVersion(1)
                .payloadChannel()
                .play()
                .serverbound(flow -> flow.add(
                        TrashActionPayload.TYPE,
                        TrashActionPayload.CODEC,
                        (TrashActionPayload payload, CustomPayloadEvent.Context ctx) -> {
                            ctx.enqueueWork(() -> {
                                var player = ctx.getSender();
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
                            ctx.setPacketHandled(true);
                        }
                ));
    }

    public static void sendPacket(TrashActionPayload payload) {
        PacketDistributor.SERVER.noArg().send(new ServerboundCustomPayloadPacket(payload));
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientEvents {
        private static boolean deleteWasDown = false;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
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
                sendPacket(new TrashActionPayload(1, rawId, ItemStack.EMPTY));

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
                    sendPacket(new TrashActionPayload(0, 0, ItemStack.EMPTY));
                } else if (!lastTrashedItem.isEmpty()) {
                    ItemStack restored = lastTrashedItem.copy();
                    client.player.containerMenu.setCarried(restored);
                    sendPacket(new TrashActionPayload(2, 0, restored));
                    lastTrashedItem = ItemStack.EMPTY;
                }
            }
        }
    }
}
