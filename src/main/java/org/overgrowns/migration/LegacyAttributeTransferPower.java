package org.overgrowns.migration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Set;

/**
 * Legacy attribute_modify_transfer, limited to the class Overgrown exposes a matching value hook for
 * (modify_air_speed, used by origins:like_air). Other classes stay an explicit load error rather than a no-op.
 */
public final class LegacyAttributeTransferPower extends PowerType<LegacyAttributeTransferPower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "attribute_modify_transfer");
    private static final Set<String> AIR_SPEED = Set.of("modify_air_speed", "origins:modify_air_speed", "apoli:modify_air_speed");

    public record Config(String modifyClass, ResourceLocation attribute, double multiplier) {}

    private static final Codec<String> CLASS = Codec.STRING.flatXmap(name -> AIR_SPEED.contains(name)
        ? DataResult.success(name)
        : DataResult.error(() -> "attribute_modify_transfer class " + name + " is not supported on Overgrown; only modify_air_speed is"),
        DataResult::success);

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            CLASS.fieldOf("class").forGetter(Config::modifyClass),
            IdCodecs.ID.fieldOf("attribute").forGetter(Config::attribute),
            Codec.DOUBLE.optionalFieldOf("multiplier", 1.0).forGetter(Config::multiplier)
        ).apply(i, Config::new));
    }

    /** Applies the holder's current modifiers of each transferred attribute to the air speed value. */
    public static float modifyAirSpeed(LivingEntity entity, float value) {
        double[] result = {value};
        PowerLookup.forEach(entity, ID, Config.class, cfg -> {
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(cfg.attribute());
            AttributeInstance instance = attribute == null ? null : entity.getAttribute(attribute);
            if (instance != null) result[0] = transfer(result[0], instance, cfg.multiplier());
        });
        return Double.isFinite(result[0]) ? (float) result[0] : value;
    }

    /** Vanilla attribute order: additions, then base multipliers, then total multipliers. */
    private static double transfer(double base, AttributeInstance instance, double multiplier) {
        double value = base;
        for (AttributeModifier mod : instance.getModifiers())
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) value += mod.getAmount() * multiplier;
        double added = value;
        for (AttributeModifier mod : instance.getModifiers())
            if (mod.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) value += added * mod.getAmount() * multiplier;
        for (AttributeModifier mod : instance.getModifiers())
            if (mod.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) value *= 1.0 + mod.getAmount() * multiplier;
        return value;
    }
}
