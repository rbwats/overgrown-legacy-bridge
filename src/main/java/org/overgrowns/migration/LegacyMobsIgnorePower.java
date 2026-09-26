package org.overgrowns.migration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Optional;

/** Keeps selected mobs from targeting a holder of an old Sync or Apugli power. */
public final class LegacyMobsIgnorePower extends PowerType<LegacyMobsIgnorePower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "mobs_ignore");

    public record Config(Optional<EntityCondition> mobCondition, boolean provokable) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityCondition.CODEC.optionalFieldOf("mob_condition").forGetter(Config::mobCondition),
            Codec.BOOL.optionalFieldOf("provokable", false).forGetter(Config::provokable)
        ).apply(i, Config::new));
    }

    public static boolean blocks(Mob mob, LivingEntity target) {
        boolean[] ignored = {false};
        PowerLookup.forEach(target, ID, Config.class, cfg -> {
            if (cfg.provokable() && mob.getLastHurtByMob() == target) return;
            if (cfg.mobCondition().isPresent()
                && !cfg.mobCondition().get().test(EntityCtx.of(mob, mob.level()))) return;
            ignored[0] = true;
        });
        return ignored[0];
    }
}
