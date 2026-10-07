package org.overgrowns.migration.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.overgrowns.migration.LegacyInsomniaPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
/** Wrapping rather than redirecting lets other phantom mods hook the same calls. */
@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {
    @Unique private ServerPlayer overgrownLegacyBridge$player;
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getStats()Lnet/minecraft/stats/ServerStatsCounter;"))
    private ServerStatsCounter overgrownLegacyBridge$capture(ServerPlayer player, Operation<ServerStatsCounter> original) {
        overgrownLegacyBridge$player = player;
        return original.call(player);
    }
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
    private int overgrownLegacyBridge$insomnia(int original) {
        // The result is passed to nextInt, which rejects non-positive bounds.
        if (overgrownLegacyBridge$player == null) return original;
        org.overgrowns.migration.LegacyAttributeTransferPower.begin(overgrownLegacyBridge$player, "modify_insomnia_ticks");
        int modified = LegacyInsomniaPower.modify(overgrownLegacyBridge$player, original);
        modified = (int) org.overgrowns.migration.LegacyAttributeTransferPower.end((double) modified);
        return Math.max(1, modified);
    }
}
