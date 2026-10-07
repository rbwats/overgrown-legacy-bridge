package org.overgrowns.migration.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.overgrowns.migration.LegacyOriginUpgrades;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Origins 1.10.0 checked origin upgrades as an advancement was completed. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow private ServerPlayer player;

    @Shadow public abstract AdvancementProgress getOrStartProgress(Advancement advancement);

    @Inject(method = "award", at = @At("HEAD"))
    private void overgrownLegacyBridge$wasDone(Advancement advancement, String criterion, CallbackInfoReturnable<Boolean> cir,
                                               @Share("wasDone") LocalBooleanRef wasDone) {
        wasDone.set(getOrStartProgress(advancement).isDone());
    }

    @Inject(method = "award", at = @At("RETURN"))
    private void overgrownLegacyBridge$upgrade(Advancement advancement, String criterion, CallbackInfoReturnable<Boolean> cir,
                                               @Share("wasDone") LocalBooleanRef wasDone) {
        if (!wasDone.get() && getOrStartProgress(advancement).isDone())
            LegacyOriginUpgrades.onCompleted(player, advancement.getId());
    }
}
