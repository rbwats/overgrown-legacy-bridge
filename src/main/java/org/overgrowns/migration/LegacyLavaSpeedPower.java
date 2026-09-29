package org.overgrowns.migration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.*;
import dev.overgrown.apoli.power.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
/** Legacy lava friction attribute, applied without requiring AdditionalEntityAttributes. */
public final class LegacyLavaSpeedPower extends PowerType<LegacyLavaSpeedPower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "modify_lava_speed");
    public record Config(Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers) {}
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }
    public static double modify(LivingEntity entity, double original) {
        var modifiers = new ArrayList<AttributeModifier>();
        PowerLookup.forEach(entity, ID, Config.class, cfg -> modifiers.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers())));
        if (modifiers.isEmpty()) return original;
        double result = AttributeModifierHelper.apply(original, modifiers, entity, PowerContainer.of(entity));
        return Double.isFinite(result) ? Math.max(0, Math.min(1, result)) : original;
    }
}
