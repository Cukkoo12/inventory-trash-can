package com.example.mixin;

import com.example.TrashPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class TrashSlotMixin extends Screen {

    // Resimlerin (Sprites) sisteme tanıtılması
    private static final WidgetSprites COP_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath("inventory_trash_can", "trash_can"),
            Identifier.fromNamespaceAndPath("inventory_trash_can", "trash_can_hovered")  // Kapalı Kapak (Fare Üstünde)
    );

    protected TrashSlotMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    public void copButonuEkle(CallbackInfo ci) {
        // Envanterdeki konumu
        int x = this.width / 2 + 90;
        int y = this.height / 2 + 55;

        ImageButton copButonu = new ImageButton(
                x, y,
                16, 16,
                COP_SPRITES,
                (button) -> {
                    Minecraft mc = Minecraft.getInstance();

                    if (mc.player != null && mc.player.containerMenu != null) {
                        ItemStack faredekiEsya = mc.player.containerMenu.getCarried();

                        // Eğer farenin ucunda gerçekten bir eşya varsa
                        if (!faredekiEsya.isEmpty()) {

                            // 1. İstemci tarafında (Görsel olarak) sil
                            mc.player.containerMenu.setCarried(ItemStack.EMPTY);

                            // 2. Sunucu tarafına (Server'a) paketi yolla
                            ClientPlayNetworking.send(new TrashPayload());
                        }
                    }

                    // Kapağın takılı kalmaması için butondan odağı kaldırıyoruz
                    button.setFocused(false);
                }
        );

        this.addRenderableWidget(copButonu);
    }
}