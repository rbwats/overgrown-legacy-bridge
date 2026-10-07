package org.overgrowns.migration.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.overgrown.apoli.command.ApoliPowerCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import org.overgrowns.migration.LegacyPowerCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy defaults for /power grant and /power revoke without an explicit source. */
@Mixin(value = ApoliPowerCommand.class, remap = false)
public abstract class ApoliPowerCommandMixin {
    @ModifyVariable(method = "grant", at = @At("HEAD"), argsOnly = true, require = 0)
    private static ResourceLocation overgrownLegacyBridge$grantSource(ResourceLocation explicitSource) {
        return explicitSource != null ? explicitSource : LegacyPowerCommands.COMMAND_SOURCE;
    }

    @Inject(method = "revoke", at = @At("HEAD"), cancellable = true, require = 0)
    private static void overgrownLegacyBridge$revoke(CommandContext<CommandSourceStack> context,
            CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        // "remove" shares this handler and already removes every source, as legacy remove did.
        boolean remove = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("remove"));
        if (!remove) cir.setReturnValue(LegacyPowerCommands.revokeDefault(context));
    }
}
