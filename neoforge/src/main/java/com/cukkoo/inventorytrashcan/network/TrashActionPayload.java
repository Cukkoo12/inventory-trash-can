package com.cukkoo.inventorytrashcan.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record TrashActionPayload(int action, int rawItemId, ItemStack restoreStack) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TrashActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("inventory_trash_can", "trash_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrashActionPayload> CODEC = new StreamCodec<>() {
        @Override
        public TrashActionPayload decode(RegistryFriendlyByteBuf buf) {
            int action = buf.readVarInt();
            int rawItemId = buf.readVarInt();
            ItemStack stack = action == 2 ? ItemStack.STREAM_CODEC.decode(buf) : ItemStack.EMPTY;
            return new TrashActionPayload(action, rawItemId, stack);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TrashActionPayload payload) {
            buf.writeVarInt(payload.action());
            buf.writeVarInt(payload.rawItemId());
            if (payload.action() == 2) {
                ItemStack.STREAM_CODEC.encode(buf, payload.restoreStack());
            }
        }
    };

    @Override
    public CustomPacketPayload.Type<TrashActionPayload> type() {
        return TYPE;
    }
}
