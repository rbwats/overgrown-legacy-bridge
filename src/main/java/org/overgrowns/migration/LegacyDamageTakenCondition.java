package org.overgrowns.migration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;

/** Allows entity actions inside a damage modifier to compare the incoming hit. */
public final class LegacyDamageTakenCondition
        implements ConditionType<EntityCtx, LegacyDamageTakenCondition.Config> {
    public record Config(Comparison comparison, double compareTo) {}

    @Override
    public MapCodec<Config> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Config::comparison),
            Codec.DOUBLE.fieldOf("compare_to").forGetter(Config::compareTo)
        ).apply(i, Config::new));
    }

    @Override
    public boolean test(Config cfg, EntityCtx ctx) {
        LegacyDamageContext.Hit hit = LegacyDamageContext.current();
        return hit != null && hit.target() == ctx.entity()
            && cfg.comparison().compare(hit.incomingAmount(), cfg.compareTo());
    }
}
