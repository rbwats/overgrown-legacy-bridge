package org.overgrowns.migration.mixin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.overgrowns.migration.LegacyInsomniaPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {
    @Unique private ServerPlayer overgrownLegacyBridge$player;
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getStats()Lnet/minecraft/stats/ServerStatsCounter;"))
    private ServerStatsCounter overgrownLegacyBridge$capture(ServerPlayer player) {
        overgrownLegacyBridge$player = player;
        return player.getStats();
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
    private int overgrownLegacyBridge$insomnia(int value, int min, int max) {
        int original = Mth.clamp(value, min, max);
        return overgrownLegacyBridge$player == null ? original : LegacyInsomniaPower.modify(overgrownLegacyBridge$player, original);
    }
}
