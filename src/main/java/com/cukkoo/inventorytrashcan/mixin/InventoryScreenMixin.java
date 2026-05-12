package com.cukkoo.inventorytrashcan.mixin;

import com.cukkoo.inventorytrashcan.InventoryTrashCanMod;
import com.cukkoo.inventorytrashcan.network.TrashActionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends Screen {

    @Unique
    private static final WidgetSprites TRASH_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath("inventory_trash_can", "trash_can"),
            Identifier.fromNamespaceAndPath("inventory_trash_can", "trash_can_hovered")
    );

    @Unique
    private ImageButton trashButton;

    protected InventoryScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        int x = this.width / 2 + 90 + InventoryTrashCanMod.CONFIG.slotX;
        int y = this.height / 2 + 55 + InventoryTrashCanMod.CONFIG.slotY;

        trashButton = new ImageButton(x, y, 16, 16, TRASH_SPRITES, (btn) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.containerMenu == null) return;

            boolean shiftDown = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);

            ItemStack carried = mc.player.containerMenu.getCarried();

            if (shiftDown) {
                ItemStack trashRef = !carried.isEmpty() ? carried : InventoryTrashCanMod.lastTrashedItem;
                if (trashRef.isEmpty()) return;

                int rawId = BuiltInRegistries.ITEM.getId(trashRef.getItem());
                ClientPlayNetworking.send(new TrashActionPayload(1, rawId, ItemStack.EMPTY));

                InventoryScreen screen = (InventoryScreen) (Object) this;
                int totalCount = carried.getCount();
                for (Slot slot : screen.getMenu().slots) {
                    if (slot.container instanceof Inventory && slot.hasItem()
                            && slot.getItem().getItem() == trashRef.getItem()) {
                        totalCount += slot.getItem().getCount();
                        slot.set(ItemStack.EMPTY);
                    }
                }
                mc.player.containerMenu.setCarried(ItemStack.EMPTY);
                ItemStack bulkTrashed = trashRef.copy();
                bulkTrashed.setCount(totalCount);
                InventoryTrashCanMod.lastTrashedItem = bulkTrashed;
            } else if (!carried.isEmpty()) {
                InventoryTrashCanMod.lastTrashedItem = carried.copy();
                mc.player.containerMenu.setCarried(ItemStack.EMPTY);
                ClientPlayNetworking.send(new TrashActionPayload(0, 0, ItemStack.EMPTY));
            } else if (!InventoryTrashCanMod.lastTrashedItem.isEmpty()) {
                ItemStack restored = InventoryTrashCanMod.lastTrashedItem.copy();
                mc.player.containerMenu.setCarried(restored);
                ClientPlayNetworking.send(new TrashActionPayload(2, 0, restored));
                InventoryTrashCanMod.lastTrashedItem = ItemStack.EMPTY;
            }

            btn.setFocused(false);
        });
        this.addRenderableWidget(trashButton);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("RETURN"))
    private void afterRender(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                             float delta, CallbackInfo ci) {
        if (trashButton == null) return;

        if (!InventoryTrashCanMod.lastTrashedItem.isEmpty()) {
            ItemStack trashItem = InventoryTrashCanMod.lastTrashedItem;
            extractor.item(trashItem, trashButton.getX(), trashButton.getY());
            if (trashItem.getCount() > 1) {
                String count = String.valueOf(trashItem.getCount());
                Font font = Minecraft.getInstance().font;
                extractor.text(font, count,
                        trashButton.getX() + 17 - font.width(count),
                        trashButton.getY() + 9, 0xFFFFFFFF);
            }
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ItemStack carried = client.player.containerMenu.getCarried();
        ItemStack highlightRef = !carried.isEmpty() ? carried : InventoryTrashCanMod.lastTrashedItem;
        if (highlightRef.isEmpty()) return;

        boolean hovering = mouseX >= trashButton.getX() && mouseX < trashButton.getX() + 16
                && mouseY >= trashButton.getY() && mouseY < trashButton.getY() + 16;
        if (!hovering) return;

        boolean shiftDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (!shiftDown) return;

        InventoryScreen screen = (InventoryScreen) (Object) this;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory && slot.hasItem()
                    && slot.getItem().getItem() == highlightRef.getItem()) {
                AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
                int sx = acc.getLeftPos() + slot.x;
                int sy = acc.getTopPos() + slot.y;
                extractor.fill(sx, sy, sx + 16, sy + 16, 0x80FF0000);
            }
        }
    }
}
