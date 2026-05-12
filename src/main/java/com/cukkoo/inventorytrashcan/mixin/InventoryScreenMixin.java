package com.cukkoo.inventorytrashcan.mixin;

import com.cukkoo.inventorytrashcan.InventoryTrashCanMod;
import com.cukkoo.inventorytrashcan.network.TrashMessage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation TRASH = new ResourceLocation("inventory_trash_can", "textures/gui/sprites/trash_can.png");
    @Unique
    private static final ResourceLocation TRASH_HOVERED = new ResourceLocation("inventory_trash_can", "textures/gui/sprites/trash_can_hovered.png");

    @Unique
    private AbstractWidget trashButton;

    protected InventoryScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        int x = this.width / 2 + 90 + InventoryTrashCanMod.CONFIG.slotX;
        int y = this.height / 2 + 55 + InventoryTrashCanMod.CONFIG.slotY;

        trashButton = new AbstractWidget(x, y, 16, 16, Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation texture = this.isHovered() ? TRASH_HOVERED : TRASH;
                guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, 16, 16, 16, 16);
            }

            @Override
            public void onClick(double mouseX, double mouseY) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.player.containerMenu == null) return;

                long window = mc.getWindow().getWindow();
                boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                        || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

                ItemStack carried = mc.player.containerMenu.getCarried();

                if (shiftDown) {
                    ItemStack trashRef = !carried.isEmpty() ? carried : InventoryTrashCanMod.lastTrashedItem;
                    if (trashRef.isEmpty()) return;

                    int rawId = BuiltInRegistries.ITEM.getId(trashRef.getItem());
                    InventoryTrashCanMod.CHANNEL.sendToServer(new TrashMessage(1, rawId, ItemStack.EMPTY));

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
                    InventoryTrashCanMod.CHANNEL.sendToServer(new TrashMessage(0, 0, ItemStack.EMPTY));
                } else if (!InventoryTrashCanMod.lastTrashedItem.isEmpty()) {
                    ItemStack restored = InventoryTrashCanMod.lastTrashedItem.copy();
                    mc.player.containerMenu.setCarried(restored);
                    InventoryTrashCanMod.CHANNEL.sendToServer(new TrashMessage(2, 0, restored));
                    InventoryTrashCanMod.lastTrashedItem = ItemStack.EMPTY;
                }

                this.setFocused(false);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput output) {
            }
        };
        this.addRenderableWidget(trashButton);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At("RETURN"))
    private void afterRender(GuiGraphics guiGraphics, int mouseX, int mouseY,
                             float delta, CallbackInfo ci) {
        if (trashButton == null) return;

        if (!InventoryTrashCanMod.lastTrashedItem.isEmpty()) {
            ItemStack trashItem = InventoryTrashCanMod.lastTrashedItem;
            guiGraphics.renderItem(trashItem, trashButton.getX(), trashButton.getY());
            if (trashItem.getCount() > 1) {
                String count = String.valueOf(trashItem.getCount());
                Font font = Minecraft.getInstance().font;
                guiGraphics.drawString(font, count,
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

        long window = client.getWindow().getWindow();
        boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (!shiftDown) return;

        InventoryScreen screen = (InventoryScreen) (Object) this;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory && slot.hasItem()
                    && slot.getItem().getItem() == highlightRef.getItem()) {
                AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
                int sx = acc.getLeftPos() + slot.x;
                int sy = acc.getTopPos() + slot.y;
                guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0x80FF0000);
            }
        }
    }
}
