package org.overgrowns.migration;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;

/** Compatibility for the old passive mob group used by moon's forcefield. */
public final class LegacyPassiveMobCondition
        implements ConditionType<EntityCtx, LegacyPassiveMobCondition.Config> {
    public record Config() {}

    @Override
    public MapCodec<Config> codec() {
        return MapCodec.unit(new Config());
    }

    @Override
    public boolean test(Config cfg, EntityCtx ctx) {
        return ctx.entity() instanceof PathfinderMob && !(ctx.entity() instanceof Enemy);
    }
}
