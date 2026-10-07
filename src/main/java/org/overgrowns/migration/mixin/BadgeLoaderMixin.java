package org.overgrowns.migration.mixin;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.overgrowns.migration.LegacyBadges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = dev.overgrown.origins.badge.BadgeLoader.class, remap = false)
public abstract class BadgeLoaderMixin {
    @Inject(method = "readBadge", at = @At("HEAD"), require = 0)
    private static void overgrownLegacyBridge$legacySprite(JsonElement element, Map<ResourceLocation, ?> standalone,
            ResourceLocation powerId, CallbackInfoReturnable<?> cir) {
        LegacyBadges.normalize(element);
    }
}
