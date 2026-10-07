package org.overgrowns.migration.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.overgrowns.migration.LegacyXpGain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Legacy modify_xp_gain changed an orb's value as the player picked it up; Overgrown registers the power but never applies it. */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Shadow private int value;

    @Inject(method = "playerTouch", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;takeXpDelay:I", opcode = Opcodes.PUTFIELD))
    private void overgrownLegacyBridge$xpGain(Player player, CallbackInfo ci) {
        value = LegacyXpGain.modify(player, value);
    }
}
