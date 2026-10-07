package org.overgrowns.migration.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overgrowns.migration.LegacyBuiltinPowers;
import org.overgrowns.migration.LegacyIdPowers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** origins:no_cobweb_slowdown and origins:master_of_webs_no_slowdown cancelled every stuck-in-block slowdown for players. */
@Mixin(Entity.class)
public abstract class LegacyCobwebMixin {
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void overgrownLegacyBridge$noSlowdown(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player && (LegacyIdPowers.active(self, LegacyBuiltinPowers.NO_COBWEB_SLOWDOWN)
                || LegacyIdPowers.active(self, LegacyBuiltinPowers.MASTER_OF_WEBS_NO_SLOWDOWN))) ci.cancel();
    }
}
