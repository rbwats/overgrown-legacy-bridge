package org.overgrowns.migration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.network.VelocityUpdater;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Restores old bi-entity velocity spaces based on either participant's velocity. */
public final class LegacyRelativeVelocityAction
        implements ActionType<BiEntityCtx, LegacyRelativeVelocityAction.Config> {
    public record Config(Expression x, Expression y, Expression z, String relativeTo, boolean set) {}

    private static final Expression ZERO = Expression.constant(0.0);
    private static final MapCodec<Config> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Expression.DOUBLE_OR_EXPR.optionalFieldOf("x", ZERO).forGetter(Config::x),
        Expression.DOUBLE_OR_EXPR.optionalFieldOf("y", ZERO).forGetter(Config::y),
        Expression.DOUBLE_OR_EXPR.optionalFieldOf("z", ZERO).forGetter(Config::z),
        Codec.STRING.fieldOf("relative_to").forGetter(Config::relativeTo),
        Codec.BOOL.optionalFieldOf("set", false).forGetter(Config::set)
    ).apply(i, Config::new));

    @Override
    public MapCodec<Config> codec() {
        return CODEC;
    }

    @Override
    public void run(Config cfg, BiEntityCtx context) {
        Entity actor = context.actor();
        Entity target = context.target();
        if (actor == null || target == null) return;
        Entity reference = "target".equals(cfg.relativeTo()) ? target : actor;
        Vec3 amount = new Vec3(cfg.x().eval(actor), cfg.y().eval(actor), cfg.z().eval(actor));
        Vec3 delta = Space.transformVectorToBase(
            reference.getDeltaMovement(), amount, reference.getYRot(), true);
        if (delta.lengthSqr() > 0) VelocityUpdater.apply(target, delta, cfg.set());
    }
}
