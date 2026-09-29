package org.overgrowns.migration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
/** Modifies only the insomnia value used by the phantom spawner, never the stored statistic. */
public final class LegacyInsomniaPower extends PowerType<LegacyInsomniaPower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "modify_insomnia_ticks");
    public record Config(Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers) {}
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }
    public static int modify(LivingEntity entity, int value) {
        var mods = new ArrayList<AttributeModifier>();
        PowerLookup.forEach(entity, ID, Config.class, cfg -> mods.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers())));
        double result = AttributeModifierHelper.apply(value, mods, entity, PowerContainer.of(entity));
        return Double.isFinite(result) ? (int) result : value;
    }
}
