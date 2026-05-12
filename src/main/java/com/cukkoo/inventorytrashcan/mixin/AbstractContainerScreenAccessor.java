package com.cukkoo.inventorytrashcan.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("x")
    int getLeftPos();

    @Accessor("y")
    int getTopPos();
}
