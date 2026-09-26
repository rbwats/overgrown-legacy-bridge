package org.overgrowns.migration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.network.VelocityUpdater;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Applies the legacy bi-entity modify_velocity action to the target. */
public final class LegacyModifyVelocityAction
        implements ActionType<BiEntityCtx, LegacyModifyVelocityAction.Config> {
    public record Config(AttributeModifier modifier, List<String> axes) {}

    @Override
    public MapCodec<Config> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.fieldOf("modifier").forGetter(Config::modifier),
            Codec.STRING.listOf().optionalFieldOf("axes", List.of("x", "y", "z"))
                .forGetter(Config::axes)
        ).apply(i, Config::new));
    }

    @Override
    public void run(Config cfg, BiEntityCtx ctx) {
        Entity target = ctx.target();
        if (target == null) return;
        Vec3 before = target.getDeltaMovement();
        PowerContainer holder = PowerContainer.of(target);
        double x = cfg.axes().contains("x")
            ? cfg.modifier().applyToValue(before.x, target, holder) : before.x;
        double y = cfg.axes().contains("y")
            ? cfg.modifier().applyToValue(before.y, target, holder) : before.y;
        double z = cfg.axes().contains("z")
            ? cfg.modifier().applyToValue(before.z, target, holder) : before.z;
        VelocityUpdater.apply(target, new Vec3(x, y, z), true);
    }
}
