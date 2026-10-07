package org.overgrowns.migration;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import java.util.*;
/** Preserves the eight legacy tag comparisons and registry tie order from Apoli 2.9.0. */
public record LegacyDamageSource(String name, boolean bypassesArmor, boolean fire, boolean unblockable,
        boolean magic, boolean outOfWorld, boolean projectile, boolean explosive) {
    public static final Codec<LegacyDamageSource> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("name").forGetter(LegacyDamageSource::name),
        Codec.BOOL.optionalFieldOf("bypasses_armor", false).forGetter(LegacyDamageSource::bypassesArmor),
        Codec.BOOL.optionalFieldOf("fire", false).forGetter(LegacyDamageSource::fire),
        Codec.BOOL.optionalFieldOf("unblockable", false).forGetter(LegacyDamageSource::unblockable),
        Codec.BOOL.optionalFieldOf("magic", false).forGetter(LegacyDamageSource::magic),
        Codec.BOOL.optionalFieldOf("out_of_world", false).forGetter(LegacyDamageSource::outOfWorld),
        Codec.BOOL.optionalFieldOf("projectile", false).forGetter(LegacyDamageSource::projectile),
        Codec.BOOL.optionalFieldOf("explosive", false).forGetter(LegacyDamageSource::explosive)
    ).apply(i, LegacyDamageSource::new));
    private int score(Holder<DamageType> holder) {
        int count = 0;
        if (holder.is(DamageTypeTags.BYPASSES_ARMOR) == bypassesArmor) count++;
        if (holder.is(DamageTypeTags.IS_FIRE) == fire) count++;
        if (holder.is(DamageTypeTags.BYPASSES_SHIELD) == unblockable) count++;
        if (holder.is(DamageTypeTags.WITCH_RESISTANT_TO) == magic) count++;
        if (holder.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS) == magic) count++;
        if (holder.is(DamageTypeTags.BYPASSES_INVULNERABILITY) == outOfWorld) count++;
        if (holder.is(DamageTypeTags.IS_PROJECTILE) == projectile) count++;
        if (holder.is(DamageTypeTags.IS_EXPLOSION) == explosive) count++;
        return count;
    }
    public DamageSource create(Level level, Entity attacker) {
        var best = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).holders()
            .max(Comparator.comparingInt(this::score)).orElseThrow();
        if (score(best) < 8 && WARNED.add(this)) LegacyBridge.LOGGER.warn("Legacy damage source {} has only {}/8 matching tags in this registry ({}).", name, score(best), best.key().location());
        return new Named(best, attacker, name);
    }
    private static final Set<LegacyDamageSource> WARNED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static class Named extends DamageSource {
        public final String legacyName;
        public Named(Holder<DamageType> holder, Entity attacker, String name) { super(holder, attacker); legacyName = name; }
        /** Legacy Apoli renamed the source itself, so origins:name damage conditions match the legacy name. */
        @Override public String getMsgId() { return legacyName; }
    }
}
