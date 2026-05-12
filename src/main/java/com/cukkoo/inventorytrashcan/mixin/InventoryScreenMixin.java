package com.cukkoo.inventorytrashcan.mixin;

import com.cukkoo.inventorytrashcan.InventoryTrashCanMod;
import com.cukkoo.inventorytrashcan.network.TrashNetwork;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends Screen {

    @Unique
    private static final Identifier TRASH = new Identifier("inventory_trash_can", "textures/gui/sprites/trash_can.png");
    @Unique
    private static final Identifier TRASH_HOVERED = new Identifier("inventory_trash_can", "textures/gui/sprites/trash_can_hovered.png");

    @Unique
    private PressableWidget trashButton;

    protected InventoryScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        int x = this.width / 2 + 90 + InventoryTrashCanMod.CONFIG.slotX;
        int y = this.height / 2 + 55 + InventoryTrashCanMod.CONFIG.slotY;

        trashButton = new PressableWidget(x, y, 16, 16, Text.empty()) {
            @Override
            public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                Identifier texture = this.isHovered() ? TRASH_HOVERED : TRASH;
                context.drawTexture(texture, this.getX(), this.getY(), 0, 0, 16, 16, 16, 16);
            }

            @Override
            public void onPress() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.player.currentScreenHandler == null) return;

                long window = mc.getWindow().getHandle();
                boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

                ItemStack carried = mc.player.currentScreenHandler.getCursorStack();

                if (shiftDown) {
                    ItemStack trashRef = !carried.isEmpty() ? carried : InventoryTrashCanMod.lastTrashedItem;
                    if (trashRef.isEmpty()) return;

                    int rawId = Registries.ITEM.getRawId(trashRef.getItem());
                    TrashNetwork.sendBulkTrash(rawId);

                    InventoryScreen screen = (InventoryScreen) (Object) this;
                    int totalCount = carried.getCount();
                    for (Slot slot : screen.getScreenHandler().slots) {
                        if (slot.inventory instanceof PlayerInventory && slot.hasStack()
                                && slot.getStack().getItem() == trashRef.getItem()) {
                            totalCount += slot.getStack().getCount();
                            slot.setStack(ItemStack.EMPTY);
                        }
                    }
                    mc.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                    ItemStack bulkTrashed = trashRef.copy();
                    bulkTrashed.setCount(totalCount);
                    InventoryTrashCanMod.lastTrashedItem = bulkTrashed;
                } else if (!carried.isEmpty()) {
                    InventoryTrashCanMod.lastTrashedItem = carried.copy();
                    mc.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                    TrashNetwork.sendTrash();
                } else if (!InventoryTrashCanMod.lastTrashedItem.isEmpty()) {
                    ItemStack restored = InventoryTrashCanMod.lastTrashedItem.copy();
                    mc.player.currentScreenHandler.setCursorStack(restored);
                    TrashNetwork.sendRestore(restored);
                    InventoryTrashCanMod.lastTrashedItem = ItemStack.EMPTY;
                }

                this.setFocused(false);
            }

            @Override
            public void appendClickableNarrations(NarrationMessageBuilder builder) {
            }
        };
        this.addDrawableChild(trashButton);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At("RETURN"))
    private void afterRender(DrawContext context, int mouseX, int mouseY,
                             float delta, CallbackInfo ci) {
        if (trashButton == null) return;

        if (!InventoryTrashCanMod.lastTrashedItem.isEmpty()) {
            ItemStack trashItem = InventoryTrashCanMod.lastTrashedItem;
            context.drawItem(trashItem, trashButton.getX(), trashButton.getY());
            if (trashItem.getCount() > 1) {
                String count = String.valueOf(trashItem.getCount());
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                context.drawText(textRenderer, count,
                        trashButton.getX() + 17 - textRenderer.getWidth(count),
                        trashButton.getY() + 9, 0xFFFFFFFF, true);
            }
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack carried = client.player.currentScreenHandler.getCursorStack();
        ItemStack highlightRef = !carried.isEmpty() ? carried : InventoryTrashCanMod.lastTrashedItem;
        if (highlightRef.isEmpty()) return;

        boolean hovering = mouseX >= trashButton.getX() && mouseX < trashButton.getX() + 16
                && mouseY >= trashButton.getY() && mouseY < trashButton.getY() + 16;
        if (!hovering) return;

        long window = client.getWindow().getHandle();
        boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (!shiftDown) return;

        InventoryScreen screen = (InventoryScreen) (Object) this;
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.inventory instanceof PlayerInventory && slot.hasStack()
                    && slot.getStack().getItem() == highlightRef.getItem()) {
                AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
                int sx = acc.getLeftPos() + slot.x;
                int sy = acc.getTopPos() + slot.y;
                context.fill(sx, sy, sx + 16, sy + 16, 0x80FF0000);
            }
        }
    }
}
