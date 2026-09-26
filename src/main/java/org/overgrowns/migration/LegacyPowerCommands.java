package org.overgrowns.migration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Accepts the old `/power revoke targets power source` spelling without changing pack files. */
public final class LegacyPowerCommands {
    private LegacyPowerCommands() {}

    public static void attach(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("apoli:power");
        if (root == null) {
            LegacyBridge.LOGGER.error("Could not attach legacy power command: apoli:power is unavailable");
            return;
        }
        for (String verb : new String[] {"revoke", "remove"}) {
            CommandNode<CommandSourceStack> command = root.getChild(verb);
            if (command == null || command.getChild("targets") == null ||
                command.getChild("targets").getChild("power") == null) {
                LegacyBridge.LOGGER.error("Could not attach legacy power command: {} tree changed", verb);
                continue;
            }
            CommandNode<CommandSourceStack> powerNode = command.getChild("targets").getChild("power");
            if (powerNode.getChild("legacy_source") == null) {
                powerNode.addChild(Commands.argument("legacy_source", ResourceLocationArgument.id())
                    .executes(LegacyPowerCommands::revokeFromSource).build());
            }
        }
        LegacyBridge.LOGGER.info("Attached legacy /power revoke and /power remove syntax");
    }

    private static int revokeFromSource(CommandContext<CommandSourceStack> context)
        throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(context, "power");
        ResourceLocation source = ResourceLocationArgument.getId(context, "legacy_source");
        int affected = 0;
        for (Entity entity : EntityArgument.getEntities(context, "targets")) {
            PowerContainer container = PowerContainer.of(entity);
            if (container != null && container.removePower(power, source)) affected++;
        }
        final int count = affected;
        context.getSource().sendSuccess(() -> Component.literal(
            "Revoked " + power + " from source " + source + " for " + count + " target(s)."), true);
        return affected;
    }
}
