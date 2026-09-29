package org.overgrowns.migration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.*;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;
public final class LegacyFluidRenderPower extends PowerType<LegacyFluidRenderPower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "modify_fluid_render");
    public record Config(Optional<BlockCondition> blockCondition, Optional<FluidCondition> fluidCondition, ResourceLocation fluid) {}
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            FluidCondition.CODEC.optionalFieldOf("fluid_condition").forGetter(Config::fluidCondition),
            IdCodecs.ID.fieldOf("fluid").forGetter(Config::fluid)
        ).apply(i, Config::new));
    }
}
