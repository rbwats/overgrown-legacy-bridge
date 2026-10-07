package org.overgrowns.migration;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ModifyXpGainPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Legacy modify_xp_gain (class ModifyExperiencePower), with its attribute_modify_transfer class modify_experience. */
public final class LegacyXpGain {
    private LegacyXpGain() {}

    private static final ResourceLocation MODIFY_XP_GAIN = new ResourceLocation("apoli", "modify_xp_gain");

    public static int modify(Player player, int value) {
        List<AttributeModifier> modifiers = new ArrayList<>();
        PowerLookup.forEach(player, MODIFY_XP_GAIN, ModifyXpGainPower.Config.class,
            cfg -> modifiers.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers())));
        LegacyAttributeTransferPower.begin(player, "modify_experience");
        double result = modifiers.isEmpty() ? value : AttributeModifierHelper.apply((double) value, modifiers, player);
        result = LegacyAttributeTransferPower.end(result);
        return Double.isFinite(result) ? (int) result : value;
    }
}
