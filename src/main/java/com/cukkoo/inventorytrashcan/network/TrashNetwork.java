package com.cukkoo.inventorytrashcan.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class TrashNetwork {

    public static final Identifier CHANNEL = new Identifier("inventory_trash_can", "trash_action");

    public static void sendTrash() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        ClientPlayNetworking.send(CHANNEL, buf);
    }

    public static void sendBulkTrash(int rawItemId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(1);
        buf.writeVarInt(rawItemId);
        ClientPlayNetworking.send(CHANNEL, buf);
    }

    public static void sendRestore(ItemStack stack) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(2);
        buf.writeVarInt(0);
        buf.writeItemStack(stack);
        ClientPlayNetworking.send(CHANNEL, buf);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, (server, player, handler, buf, responseSender) -> {
            int action = buf.readVarInt();
            int rawItemId = buf.readVarInt();
            ItemStack stack = action == 2 ? buf.readItemStack() : ItemStack.EMPTY;
            server.execute(() -> {
                if (player.currentScreenHandler == null) return;
                switch (action) {
                    case 0 -> player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                    case 1 -> {
                        player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                        Item target = Registries.ITEM.get(rawItemId);
                        for (var slot : player.currentScreenHandler.slots) {
                            if (slot.inventory instanceof PlayerInventory
                                    && slot.getStack().getItem() == target) {
                                slot.setStack(ItemStack.EMPTY);
                            }
                        }
                    }
                    case 2 -> player.currentScreenHandler.setCursorStack(stack);
                }
            });
        });
    }
}
