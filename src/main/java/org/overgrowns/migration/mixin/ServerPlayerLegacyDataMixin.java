package org.overgrowns.migration.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.overgrowns.migration.LegacySaveMigration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps a player's Origins 1.10.0 components from load until they are migrated on join. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerLegacyDataMixin implements LegacySaveMigration.Holder {
    @Unique private CompoundTag overgrownLegacyBridge$legacy;

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void overgrownLegacyBridge$capture(CompoundTag tag, CallbackInfo ci) {
        CompoundTag legacy = LegacySaveMigration.capture(tag);
        if (legacy != null) overgrownLegacyBridge$legacy = legacy;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void overgrownLegacyBridge$keep(CompoundTag tag, CallbackInfo ci) {
        if (overgrownLegacyBridge$legacy != null) LegacySaveMigration.writeBack(tag, overgrownLegacyBridge$legacy);
    }

    @Override public CompoundTag overgrownLegacyBridge$legacyComponents() { return overgrownLegacyBridge$legacy; }
    @Override public void overgrownLegacyBridge$setLegacyComponents(CompoundTag components) { overgrownLegacyBridge$legacy = components; }
}
