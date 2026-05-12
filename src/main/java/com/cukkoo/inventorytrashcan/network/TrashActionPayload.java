package com.cukkoo.inventorytrashcan.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TrashActionPayload(int action, int rawItemId, ItemStack restoreStack) implements CustomPayload {

    public static final CustomPayload.Id<TrashActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of("inventory_trash_can", "trash_action"));

    public static final PacketCodec<RegistryByteBuf, TrashActionPayload> CODEC = new PacketCodec<>() {
        @Override
        public TrashActionPayload decode(RegistryByteBuf buf) {
            int action = buf.readVarInt();
            int rawItemId = buf.readVarInt();
            ItemStack stack = action == 2 ? ItemStack.PACKET_CODEC.decode(buf) : ItemStack.EMPTY;
            return new TrashActionPayload(action, rawItemId, stack);
        }

        @Override
        public void encode(RegistryByteBuf buf, TrashActionPayload payload) {
            buf.writeVarInt(payload.action());
            buf.writeVarInt(payload.rawItemId());
            if (payload.action() == 2) {
                ItemStack.PACKET_CODEC.encode(buf, payload.restoreStack());
            }
        }
    };

    @Override
    public CustomPayload.Id<TrashActionPayload> getId() {
        return ID;
    }
}
