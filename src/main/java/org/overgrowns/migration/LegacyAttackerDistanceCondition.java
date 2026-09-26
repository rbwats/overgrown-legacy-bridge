package org.overgrowns.migration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.entity.Entity;

/** Compares distance between an incoming damage attacker and its target. */
public final class LegacyAttackerDistanceCondition
        implements ConditionType<DamageCtx, LegacyAttackerDistanceCondition.Config> {
    public record Config(Comparison comparison, double compareTo, boolean invertedDistance) {}

    @Override
    public MapCodec<Config> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Config::comparison),
            Codec.DOUBLE.fieldOf("compare_to").forGetter(Config::compareTo),
            Codec.BOOL.optionalFieldOf("inverted_distance", false).forGetter(Config::invertedDistance)
        ).apply(i, Config::new));
    }

    @Override
    public boolean test(Config cfg, DamageCtx ctx) {
        Entity attacker = ctx.source().getEntity();
        if (attacker == null || ctx.target() == null) return false;
        boolean result = cfg.comparison().compare(attacker.distanceTo(ctx.target()), cfg.compareTo());
        return cfg.invertedDistance() != result;
    }
}
