package org.overgrowns.migration;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;

/** Applies legacy gravity modifiers to vanilla's gravity value during travel. */
public final class LegacyGravityPower extends PowerType<LegacyGravityPower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "modify_gravity");

    public record Config(AttributeModifier modifier) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.fieldOf("modifier").forGetter(Config::modifier)
        ).apply(i, Config::new));
    }

    public static double modify(LivingEntity entity, double baseGravity) {
        var modifiers = new ArrayList<AttributeModifier>();
        PowerLookup.forEach(entity, ID, Config.class, cfg -> modifiers.add(cfg.modifier()));
        if (modifiers.isEmpty()) return baseGravity;
        PowerContainer holder = PowerContainer.of(entity);
        double gravity = AttributeModifierHelper.apply(baseGravity, modifiers, entity, holder);
        return Double.isFinite(gravity) ? gravity : baseGravity;
    }
}
