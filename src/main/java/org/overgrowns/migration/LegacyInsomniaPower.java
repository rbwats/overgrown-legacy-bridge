package org.overgrowns.migration;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

/** Applies the old insomnia modifier to vanilla's time-since-rest statistic. */
public final class LegacyInsomniaPower extends PowerType<LegacyInsomniaPower.Config> {
    public record Config(AttributeModifier modifier) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.fieldOf("modifier").forGetter(Config::modifier)
        ).apply(i, Config::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (!(holder.rawOwner() instanceof ServerPlayer player)) return;
        var stat = Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
        int before = player.getStats().getValue(stat);
        int after = Math.max(0, (int) cfg.modifier().applyToValue(before, player, holder));
        if (before != after) player.getStats().setValue(player, stat, after);
    }
}
