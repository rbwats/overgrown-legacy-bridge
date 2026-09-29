package org.overgrowns.migration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.*;
import dev.overgrown.apoli.power.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
public final class LegacyModifyResourceAction implements ActionType<EntityCtx, LegacyModifyResourceAction.Config> {
    public record Config(ResourceLocation resource, Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers) {}
    public MapCodec<Config> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("resource").forGetter(Config::resource),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }
    public void run(Config cfg, EntityCtx ctx) {
        PowerContainer holder = PowerContainer.of(ctx.entity());
        if (holder == null) return;
        var current = PowerResources.read(holder, cfg.resource());
        if (current.isEmpty()) return;
        double value = AttributeModifierHelper.apply(current.getAsInt(), AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()), ctx.entity(), holder);
        PowerResources.write(holder, cfg.resource(), (int) value);
    }
}
