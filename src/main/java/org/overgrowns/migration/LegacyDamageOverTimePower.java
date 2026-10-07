package org.overgrowns.migration;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.*;
public final class LegacyDamageOverTimePower extends PowerType<LegacyDamageOverTimePower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "damage_over_time");
    public record Config(int interval, int onset, float damage, float easyDamage, Optional<LegacyDamageSource> source,
            ResourceLocation damageType, Optional<ResourceLocation> protection, float effectiveness) {}
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("interval", 20).forGetter(Config::interval),
            Codec.INT.optionalFieldOf("onset_delay").forGetter(c -> Optional.of(c.onset())),
            Codec.FLOAT.fieldOf("damage").forGetter(Config::damage),
            Codec.FLOAT.optionalFieldOf("damage_easy").forGetter(c -> Optional.of(c.easyDamage())),
            LegacyDamageSource.CODEC.optionalFieldOf("damage_source").forGetter(Config::source),
            IdCodecs.ID.optionalFieldOf("damage_type", new ResourceLocation("apoli", "damage_over_time")).forGetter(Config::damageType),
            IdCodecs.ID.optionalFieldOf("protection_enchantment").forGetter(Config::protection),
            Codec.FLOAT.optionalFieldOf("protection_effectiveness", 1f).forGetter(Config::effectiveness)
        ).apply(i, (interval, onset, damage, easy, source, type, protection, effectiveness) -> new Config(interval, onset.orElse(interval), damage, easy.orElse(damage), source, type, protection, effectiveness)));
    }
    public static int onset(Config cfg, LivingEntity entity) {
        int protection = 0;
        if (cfg.protection().isPresent()) {
            var enchantment = BuiltInRegistries.ENCHANTMENT.get(cfg.protection().get());
            if (enchantment != null) for (var stack : enchantment.getSlotItems(entity).values()) {
                int level = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
                if (level > 0) protection += level + 1;
            }
        }
        return cfg.onset() + (int)(Math.pow(protection * 2, 1.3) * cfg.effectiveness()) * 20;
    }
    public void tick(ResourceLocation id, Config cfg, PowerContainer holder) {
        if (!(holder instanceof PowerContainerImpl impl) || holder.owner() == null) return;
        LivingEntity owner = holder.owner();
        Power power = ApoliPowers.get(id); if (power == null) return;
        // Counters change every tick: keep them in unsynchronized scratch state and persist them at most once a
        // second, or when damage or a reset happens, instead of broadcasting the aux state every tick.
        int[] state = impl.scratchInts(id, 3);
        if (state[2] == 0) {
            int[] saved = impl.getAuxInts(id);
            if (saved != null && saved.length >= 2) { state[0] = saved[0]; state[1] = saved[1]; }
            state[2] = 1;
        }
        boolean persist = owner.tickCount % 20 == 0;
        boolean active = power.condition().isEmpty() || power.condition().get().test(EntityCtx.of(owner, owner.level()));
        if (!active) {
            if (state[1] >= 20) { persist |= state[0] != 0; state[0] = 0; } else state[1]++;
        } else {
            state[1] = 0;
            int elapsed = state[0] - onset(cfg, owner);
            if (elapsed >= 0 && elapsed % cfg.interval() == 0) {
                DamageSource source = cfg.source().map(s -> s.create(owner.level(), null)).orElseGet(() -> {
                    var type = owner.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, cfg.damageType())).orElse(null);
                    if (type == null) {
                        if (MISSING_TYPES.add(cfg.damageType())) LegacyBridge.LOGGER.error("Damage over time power {} uses unknown damage type {}", id, cfg.damageType());
                        return null;
                    }
                    return new DamageSource(type);
                });
                if (source != null) owner.hurt(source, owner.level().getDifficulty() == Difficulty.EASY ? cfg.easyDamage() : cfg.damage());
                persist = true;
            }
            state[0]++;
        }
        if (persist) impl.setAuxInts(id, new int[] {state[0], state[1]});
    }
    /** Sets the timer from a migrated legacy save; the next tick reloads it. */
    public static void restore(PowerContainerImpl impl, ResourceLocation id, int inDamageTicks, int outOfDamageTicks) {
        impl.setAuxInts(id, new int[] {inDamageTicks, outOfDamageTicks});
        java.util.Arrays.fill(impl.scratchInts(id, 3), 0);
    }
    /** Clears the timer on respawn, as legacy {@code onRespawn} did. */
    public static void reset(PowerContainerImpl impl, ResourceLocation id) {
        impl.setAuxInts(id, new int[2]);
        java.util.Arrays.fill(impl.scratchInts(id, 3), 0);
    }
    private static final Set<ResourceLocation> MISSING_TYPES = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public void onRemoved(ResourceLocation id, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && !holder.hasPower(id)) impl.removeAux(id);
    }
}
