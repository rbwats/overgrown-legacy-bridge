package org.overgrowns.migration.mixin;
import net.minecraft.world.damagesource.DamageSource;
import org.overgrowns.migration.LegacyDamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(DamageSource.class)
public abstract class LegacyDamageNameMixin {
    @ModifyVariable(method = "getLocalizedDeathMessage", at = @At(value = "FIELD", target = "Lnet/minecraft/world/damagesource/DamageSource;causingEntity:Lnet/minecraft/world/entity/Entity;", ordinal = 0))
    private String overgrownLegacyBridge$name(String value) {
        return (Object)this instanceof LegacyDamageSource.Named source ? "death.attack." + source.legacyName : value;
    }
}
