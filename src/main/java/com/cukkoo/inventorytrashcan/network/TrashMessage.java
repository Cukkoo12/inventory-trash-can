package com.cukkoo.inventorytrashcan.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TrashMessage {

    private final int action;
    private final int rawItemId;
    private final ItemStack restoreStack;

    public TrashMessage(int action, int rawItemId, ItemStack restoreStack) {
        this.action = action;
        this.rawItemId = rawItemId;
        this.restoreStack = restoreStack;
    }

    public static void encode(TrashMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeVarInt(msg.rawItemId);
        if (msg.action == 2) buf.writeItem(msg.restoreStack);
    }

    public static TrashMessage decode(FriendlyByteBuf buf) {
        int action = buf.readVarInt();
        int rawItemId = buf.readVarInt();
        ItemStack stack = action == 2 ? buf.readItem() : ItemStack.EMPTY;
        return new TrashMessage(action, rawItemId, stack);
    }

    public static void handle(TrashMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.containerMenu == null) return;
            switch (msg.action) {
                case 0 -> player.containerMenu.setCarried(ItemStack.EMPTY);
                case 1 -> {
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    Item target = BuiltInRegistries.ITEM.byId(msg.rawItemId);
                    for (Slot slot : player.containerMenu.slots) {
                        if (slot.container instanceof Inventory && slot.getItem().getItem() == target) {
                            slot.set(ItemStack.EMPTY);
                        }
                    }
                }
                case 2 -> player.containerMenu.setCarried(msg.restoreStack);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
