package org.overgrowns.migration.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import org.overgrowns.migration.LegacyGrindstoneMenu;
import org.overgrowns.migration.LegacyGrindstonePower;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/** Legacy modify_grindstone: rewrite the result after vanilla computes it. */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin extends AbstractContainerMenu implements LegacyGrindstoneMenu {
    @Shadow @Final Container repairSlots;
    @Shadow @Final private Container resultSlots;

    @Unique private Player overgrownLegacyBridge$player;
    @Unique private Optional<BlockPos> overgrownLegacyBridge$pos = Optional.empty();
    @Unique private List<LegacyGrindstonePower.Config> overgrownLegacyBridge$applied = List.of();

    protected GrindstoneMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    private void overgrownLegacyBridge$cache(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        overgrownLegacyBridge$player = inventory.player;
        overgrownLegacyBridge$pos = access.evaluate((level, pos) -> pos);
        BY_CONTAINER.put(repairSlots, this);
        BY_CONTAINER.put(resultSlots, this);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void overgrownLegacyBridge$modifyResult(CallbackInfo ci) {
        overgrownLegacyBridge$applied = LegacyGrindstonePower.modifyResult(overgrownLegacyBridge$player, overgrownLegacyBridge$pos,
            repairSlots.getItem(0), repairSlots.getItem(1), resultSlots.getItem(0), result -> {
                resultSlots.setItem(0, result);
                broadcastChanges();
                return result;
            });
    }

    @Override public Player overgrownLegacyBridge$player() { return overgrownLegacyBridge$player; }
    @Override public Optional<BlockPos> overgrownLegacyBridge$pos() { return overgrownLegacyBridge$pos; }
    @Override public List<LegacyGrindstonePower.Config> overgrownLegacyBridge$applied() { return overgrownLegacyBridge$applied; }
}
