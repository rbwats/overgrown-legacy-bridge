package org.overgrowns.migration;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.power.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
/** State is persisted and synchronized through the native power container's auxiliary values. */
public final class LegacyToggleNightVisionPower extends PowerType<LegacyToggleNightVisionPower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "toggle_night_vision");
    public record Config(boolean activeByDefault, float strength, Key key) {}
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("active_by_default", false).forGetter(Config::activeByDefault),
            Codec.FLOAT.optionalFieldOf("strength", 1f).forGetter(Config::strength),
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Config::key)
        ).apply(i, Config::new));
    }
    public void onAdded(ResourceLocation id, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && holder.getAuxInt(id).isEmpty()) impl.setAuxInt(id, cfg.activeByDefault() ? 1 : 0);
    }
    public void onRemoved(ResourceLocation id, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && !holder.hasPower(id)) impl.removeAux(id);
    }
    public static boolean enabled(PowerContainer holder, ResourceLocation id, Config cfg) {
        return holder != null && holder.getAuxIntOr(id, cfg.activeByDefault() ? 1 : 0) != 0;
    }
    public static boolean toggle(PowerContainer holder, ResourceLocation id, Config cfg) {
        if (!(holder instanceof PowerContainerImpl impl) || !holder.hasPower(id)) return false;
        impl.setAuxInt(id, enabled(holder, id, cfg) ? 0 : 1);
        return true;
    }
    public static float strengthFor(LivingEntity entity) {
        float[] result = {0};
        PowerLookup.forEachEntry(entity, ID, Config.class, (id, cfg) -> {
            if (enabled(PowerContainer.of(entity), id, cfg)) result[0] = Math.max(result[0], cfg.strength());
        });
        return result[0];
    }
}
