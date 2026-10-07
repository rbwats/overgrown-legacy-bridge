package org.overgrowns.migration;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Origins 1.10.0 keyed some Java behavior to power IDs (PowerTypeReference.isActive) rather than power types. */
public final class LegacyIdPowers {
    private LegacyIdPowers() {}

    public static boolean active(Entity entity, ResourceLocation id) {
        if (entity == null) return false;
        PowerContainer holder = PowerContainer.of(entity);
        if (holder == null || !holder.hasPower(id) || holder.isSuppressed(id)) return false;
        Power power = ApoliPowers.get(id);
        return power != null && (power.condition().isEmpty() || power.condition().get().test(EntityCtx.of(entity, entity.level())));
    }
}
