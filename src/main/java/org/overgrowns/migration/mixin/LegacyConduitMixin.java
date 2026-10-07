package org.overgrowns.migration.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.overgrowns.migration.LegacyBuiltinPowers;
import org.overgrowns.migration.LegacyIdPowers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** origins:conduit_power_on_land let a conduit's effect reach players out of water. */
@Mixin(ConduitBlockEntity.class)
public abstract class LegacyConduitMixin {
    @WrapOperation(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private static boolean overgrownLegacyBridge$onLand(Player player, Operation<Boolean> original) {
        return original.call(player) || LegacyIdPowers.active(player, LegacyBuiltinPowers.CONDUIT_POWER_ON_LAND);
    }
}
