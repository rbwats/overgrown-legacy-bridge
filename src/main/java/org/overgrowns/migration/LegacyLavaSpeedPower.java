package org.overgrowns.migration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.*;
import dev.overgrown.apoli.power.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
/**
 * Legacy lava friction attribute, applied without requiring AdditionalEntityAttributes. Like the original
 * conditioned attribute power, the condition is re-checked every 10 ticks on the server; the result is kept in
 * synchronized auxiliary state so the client's movement uses the same on/off state.
 */
public final class LegacyLavaSpeedPower extends PowerType<LegacyLavaSpeedPower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "modify_lava_speed");
    static final int TICK_RATE = 10;
    public record Config(Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers) {}
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }
    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (owner == null || owner.tickCount % TICK_RATE != 0 || !(holder instanceof PowerContainerImpl impl)) return;
        Power power = ApoliPowers.get(powerId);
        boolean active = power == null || power.condition().isEmpty() || power.condition().get().test(EntityCtx.of(owner, owner.level()));
        int state = active ? 1 : 0;
        if (impl.getAuxIntOr(powerId, 0) != state) impl.setAuxInt(powerId, state);
    }
    public static double modify(LivingEntity entity, double original) {
        if (!(PowerContainer.of(entity) instanceof PowerContainerImpl impl)) return original;
        var modifiers = new ArrayList<AttributeModifier>();
        for (ResourceLocation powerId : impl.powersOfType(ID)) {
            // 0 until the first check, as legacy added nothing before its first 10-tick update.
            if (impl.isSuppressed(powerId) || impl.getAuxIntOr(powerId, 0) == 0) continue;
            Power power = ApoliPowers.get(powerId);
            if (power != null && power.config() instanceof Config cfg)
                modifiers.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()));
        }
        if (modifiers.isEmpty()) return original;
        double result = AttributeModifierHelper.apply(original, modifiers, entity, impl);
        return Double.isFinite(result) ? Math.max(0, Math.min(1, result)) : original;
    }
}
