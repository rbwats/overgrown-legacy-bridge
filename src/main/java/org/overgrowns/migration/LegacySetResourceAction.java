package org.overgrowns.migration;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.resources.ResourceLocation;

/** Implements the legacy {"type":"origins:set_resource","resource":"...","value":N} shape. */
public final class LegacySetResourceAction implements ActionType<EntityCtx, LegacySetResourceAction.Config> {
    public record Config(ResourceLocation resource, Expression value) {}

    private static final MapCodec<Config> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        IdCodecs.ID.fieldOf("resource").forGetter(Config::resource),
        Expression.INT_OR_EXPR.fieldOf("value").forGetter(Config::value)
    ).apply(instance, Config::new));

    @Override
    public MapCodec<Config> codec() {
        return CODEC;
    }

    @Override
    public void run(Config config, EntityCtx context) {
        PowerContainer container = PowerContainer.of(context.entity());
        if (container == null) return;
        int value = config.value().evalIntWith(context.entity(), container, 0);
        PowerResources.write(container, config.resource(), value);
    }
}
