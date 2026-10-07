package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.Power;
import dev.overgrown.origins.badge.Badge;
import dev.overgrown.origins.badge.BadgeManager;
import dev.overgrown.origins.badge.KeybindBadge;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.overgrowns.migration.LegacyToggleNightVisionPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Legacy gave toggle night vision the toggle badge; Overgrown would give the bridge's power the active badge. */
@Mixin(value = BadgeManager.class, remap = false)
public abstract class ToggleNightVisionBadgeMixin {
    @Unique private static final ResourceLocation TOGGLE_SPRITE = new ResourceLocation("origins", "textures/gui/badge/isaacfanta/toggle.png");

    @Inject(method = "autoBadges", at = @At("HEAD"), cancellable = true)
    private static void overgrownLegacyBridge$toggleBadge(ResourceLocation id, Power power, RecipeManager recipes, RegistryAccess registries,
                                                         CallbackInfoReturnable<List<Badge>> cir) {
        if (power.config() instanceof LegacyToggleNightVisionPower.Config cfg)
            cir.setReturnValue(List.of(new KeybindBadge(TOGGLE_SPRITE, "origins.gui.badge.toggle", cfg.key().key())));
    }
}
