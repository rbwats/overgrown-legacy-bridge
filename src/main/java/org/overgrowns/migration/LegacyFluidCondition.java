package org.overgrowns.migration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.FluidCtx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
public final class LegacyFluidCondition implements ConditionType<FluidCtx, ResourceLocation> {
    public MapCodec<ResourceLocation> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(IdCodecs.ID.fieldOf("fluid").forGetter(id -> id)).apply(i, id -> id));
    }
    public boolean test(ResourceLocation fluid, FluidCtx ctx) {
        return BuiltInRegistries.FLUID.getKey(ctx.state().getType()).equals(fluid);
    }
}
