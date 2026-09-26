package org.overgrowns.migration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.Optional;

/** Selects the highest-priority legacy HUD atlas or sprite mapping on the client. */
public final class LegacyStatusBarTexturePower extends PowerType<LegacyStatusBarTexturePower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "status_bar_texture");

    public record Config(Optional<ResourceLocation> texture,
                         Map<ResourceLocation, ResourceLocation> textureMap,
                         int priority) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(Config::texture),
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)
                .optionalFieldOf("texture_map", Map.of()).forGetter(Config::textureMap),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }

    public static Optional<Config> select(LivingEntity entity) {
        Config[] selected = {null};
        PowerLookup.forEach(entity, ID, Config.class, cfg -> {
            if (selected[0] == null || cfg.priority() > selected[0].priority()) selected[0] = cfg;
        });
        return Optional.ofNullable(selected[0]);
    }
}
