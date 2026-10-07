package org.overgrowns.migration.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import org.overgrowns.migration.LegacyStatusBarTexturePower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Replaces vanilla status icons with the texture selected by a legacy power. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    private static final ResourceLocation ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");

    // Legacy Apoli replaced the status bars, hearts, experience bar, crosshair, mount jump bar and mount health.
    @ModifyArg(method = {"renderPlayerHealth", "renderHeart", "renderExperienceBar", "renderCrosshair", "renderJumpMeter", "renderVehicleHealth"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"),
        index = 0, require = 0)
    private ResourceLocation overgrownLegacyBridge$statusBarTexture(ResourceLocation vanilla) {
        var player = Minecraft.getInstance().player;
        if (player == null) return vanilla;
        return LegacyStatusBarTexturePower.select(player)
            .map(cfg -> cfg.textureMap().getOrDefault(vanilla,
                vanilla.equals(ICONS) ? cfg.texture().orElse(vanilla) : vanilla))
            .orElse(vanilla);
    }
}
