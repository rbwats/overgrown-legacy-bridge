package org.overgrowns.migration.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyLavaSpeedPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LivingEntity.class)
public abstract class LavaSpeedMixin {
    @ModifyExpressionValue(method = "travel", at = {
        @At(value = "CONSTANT", args = "doubleValue=0.5", ordinal = 0),
        @At(value = "CONSTANT", args = "doubleValue=0.5", ordinal = 1),
        @At(value = "CONSTANT", args = "doubleValue=0.5", ordinal = 2)
    })
    private double overgrownLegacyBridge$lavaSpeed(double value) {
        return LegacyLavaSpeedPower.modify((LivingEntity)(Object)this, value);
    }
}
