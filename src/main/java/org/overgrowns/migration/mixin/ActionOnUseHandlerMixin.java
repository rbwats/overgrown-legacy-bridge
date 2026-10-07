package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ActionOnUseHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.overgrowns.migration.LegacyEntityUse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy entity-use priorities; untouched while no legacy entity-use power is loaded. */
@Mixin(value = ActionOnUseHandler.class, remap = false)
public abstract class ActionOnUseHandlerMixin {
    @Inject(method = "fire", at = @At("HEAD"), cancellable = true)
    private static void overgrownLegacyBridge$legacyPriorities(Player actor, Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (LegacyEntityUse.hasLegacyPowers()) cir.setReturnValue(LegacyEntityUse.beforeVanilla(actor, target, hand));
    }
}
