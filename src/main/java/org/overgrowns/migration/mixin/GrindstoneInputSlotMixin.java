package org.overgrowns.migration.mixin;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.overgrowns.migration.LegacyGrindstoneMenu;
import org.overgrowns.migration.LegacyGrindstonePower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** GrindstoneMenu$2 and $3 are the top and bottom input slots. */
@Mixin(targets = {"net.minecraft.world.inventory.GrindstoneMenu$2", "net.minecraft.world.inventory.GrindstoneMenu$3"})
public abstract class GrindstoneInputSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void overgrownLegacyBridge$allow(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;
        LegacyGrindstoneMenu menu = LegacyGrindstoneMenu.of(slot.container);
        if (menu != null && LegacyGrindstonePower.allowsInSlot(menu.overgrownLegacyBridge$player(), stack, slot.getContainerSlot() == 0))
            cir.setReturnValue(true);
    }
}
