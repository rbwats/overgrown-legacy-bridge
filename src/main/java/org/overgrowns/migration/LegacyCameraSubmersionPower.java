package org.overgrowns.migration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.*;
import dev.overgrown.apoli.power.builtin.ModifyCameraSubmersionPower.Submersion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import java.util.Optional;
public final class LegacyCameraSubmersionPower extends PowerType<LegacyCameraSubmersionPower.Config> {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation(LegacyBridge.MOD_ID, "modify_camera_submersion");
    public record Config(Optional<Submersion> from, Submersion to) {}
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Submersion.CODEC.optionalFieldOf("from").forGetter(Config::from),
            Submersion.CODEC.fieldOf("to").forGetter(Config::to)
        ).apply(i, Config::new));
    }
    public static FogType remap(Entity entity, FogType original) {
        FogType[] result = {original};
        PowerLookup.forEach(entity, ID, Config.class, cfg -> {
            if (cfg.from().isEmpty() || cfg.from().get().fog() == result[0]) result[0] = cfg.to().fog();
        });
        return result[0];
    }
}
