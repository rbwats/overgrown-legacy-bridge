package org.overgrowns.migration;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.*;
import dev.overgrown.apoli.data.*;
import net.minecraft.world.entity.Entity;
import java.util.*;
public final class LegacyDamageAction<C> implements ActionType<C, LegacyDamageAction.Config> {
    public record Config(Expression amount, LegacyDamageSource source, Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers) {}
    private final java.util.function.Function<C, Entity> target;
    private final java.util.function.Function<C, Entity> actor;
    public LegacyDamageAction(java.util.function.Function<C, Entity> target, java.util.function.Function<C, Entity> actor) { this.target = target; this.actor = actor; }
    public MapCodec<Config> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.FLOAT_OR_EXPR.fieldOf("amount").forGetter(Config::amount),
            LegacyDamageSource.CODEC.fieldOf("source").forGetter(Config::source),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }
    public void run(Config cfg, C ctx) {
        Entity entity = target.apply(ctx); if (entity == null) return;
        float amount = (float) AttributeModifierHelper.apply(cfg.amount().eval(entity), AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()), entity);
        entity.hurt(cfg.source().create(entity.level(), actor.apply(ctx)), amount);
    }
}
