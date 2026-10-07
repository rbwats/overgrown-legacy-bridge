package org.overgrowns.migration.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.overgrowns.migration.LegacyEntityUse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Priority 0 results and negative-priority legacy entity-use powers apply after vanilla interaction. */
@Mixin(Player.class)
public abstract class PlayerInteractMixin {
    @Inject(method = "interact", at = @At("RETURN"), cancellable = true)
    private void overgrownLegacyBridge$afterVanilla(Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide() || self.isSpectator() || !LegacyEntityUse.hasLegacyPowers()) return;
        cir.setReturnValue(LegacyEntityUse.afterVanilla(self, target, hand, cir.getReturnValue()));
    }
}
