package com.example;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TrashPayload() implements CustomPacketPayload {

    // Kuryenin kimliği
    public static final CustomPacketPayload.Type<TrashPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("akilli_cop", "esya_sil"));

    // Kuryenin paketlenme şekli (Boş bir tetikleyici paket olduğu için unit kullanıyoruz)
    public static final StreamCodec<ByteBuf, TrashPayload> CODEC = StreamCodec.unit(new TrashPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}