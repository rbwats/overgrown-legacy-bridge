package org.overgrowns.migration.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Legacy getAttributeValue ran PowerHolderComponent.modify with class ModifyAttributePower, so a transfer naming
 * modify_attribute applied to every attribute read. Power conditions may read attributes, hence the guard.
 */
@Mixin(LivingEntity.class)
public abstract class AttributeValueTransferMixin {
    @Unique private static final ThreadLocal<Boolean> overgrownLegacyBridge$applying = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D", at = @At("RETURN"), cancellable = true)
    private void overgrownLegacyBridge$transfer(Attribute attribute, CallbackInfoReturnable<Double> cir) {
        if (!LegacyAttributeTransferPower.isUsed("modify_attribute") || overgrownLegacyBridge$applying.get()) return;
        overgrownLegacyBridge$applying.set(true);
        try {
            double value = LegacyAttributeTransferPower.apply((LivingEntity) (Object) this, "modify_attribute", cir.getReturnValueD());
            if (value != cir.getReturnValueD()) cir.setReturnValue(value);
        } finally {
            overgrownLegacyBridge$applying.set(false);
        }
    }
}
