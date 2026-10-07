package org.overgrowns.migration.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.overgrowns.migration.LegacyGrindstoneMenu;
import org.overgrowns.migration.LegacyGrindstonePower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** GrindstoneMenu$4 is the result slot: take actions after the XP drop, and the XP modifier. */
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneResultSlotMixin {
    @Inject(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V", ordinal = 0))
    private void overgrownLegacyBridge$onTake(Player player, ItemStack stack, CallbackInfo ci) {
        LegacyGrindstoneMenu menu = LegacyGrindstoneMenu.of(((Slot) (Object) this).container);
        if (menu != null) LegacyGrindstonePower.onTake(menu.overgrownLegacyBridge$player(), menu.overgrownLegacyBridge$pos(), menu.overgrownLegacyBridge$applied(), stack);
    }

    @Inject(method = "getExperienceAmount", at = @At("RETURN"), cancellable = true)
    private void overgrownLegacyBridge$experience(Level level, CallbackInfoReturnable<Integer> cir) {
        LegacyGrindstoneMenu menu = LegacyGrindstoneMenu.of(((Slot) (Object) this).container);
        if (menu != null && !menu.overgrownLegacyBridge$applied().isEmpty())
            cir.setReturnValue(LegacyGrindstonePower.modifyExperience(menu.overgrownLegacyBridge$player(), menu.overgrownLegacyBridge$applied(), cir.getReturnValue()));
    }
}
