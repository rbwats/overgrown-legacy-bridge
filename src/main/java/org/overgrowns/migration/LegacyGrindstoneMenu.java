package org.overgrowns.migration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Grindstone state the anonymous slot classes reach through their container rather than the synthetic outer field. */
public interface LegacyGrindstoneMenu {
    Map<Container, LegacyGrindstoneMenu> BY_CONTAINER = Collections.synchronizedMap(new WeakHashMap<>());

    Player overgrownLegacyBridge$player();
    Optional<BlockPos> overgrownLegacyBridge$pos();
    List<LegacyGrindstonePower.Config> overgrownLegacyBridge$applied();

    static LegacyGrindstoneMenu of(Container container) {
        return container == null ? null : BY_CONTAINER.get(container);
    }
}
