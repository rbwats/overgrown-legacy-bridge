package org.overgrowns.migration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Legacy /power syntax: {@code revoke targets power source}, and the {@code list}, {@code sources} and
 * {@code clear} subcommands. Function files containing an unknown subcommand fail to load as a whole.
 */
public final class LegacyPowerCommands {
    private LegacyPowerCommands() {}

    /** Legacy Apoli granted and revoked under this source when a command named none. */
    public static final ResourceLocation COMMAND_SOURCE = new ResourceLocation("apoli", "command");

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
        if (root.getChild("list") == null) {
            root.addChild(Commands.literal("list")
                .then(Commands.argument("target", EntityArgument.entity())
                    .executes(context -> list(context, false))
                    .then(Commands.argument("subpowers", BoolArgumentType.bool())
                        .executes(context -> list(context, BoolArgumentType.getBool(context, "subpowers")))))
                .build());
        }
        if (root.getChild("sources") == null) {
            root.addChild(Commands.literal("sources")
                .then(Commands.argument("target", EntityArgument.entity())
                    .then(Commands.argument("power", ResourceLocationArgument.id())
                        .executes(LegacyPowerCommands::sources)))
                .build());
        }
        if (root.getChild("clear") == null) {
            root.addChild(Commands.literal("clear")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .executes(LegacyPowerCommands::clear))
                .build());
        }
        LegacyBridge.LOGGER.info("Attached legacy /power revoke, remove, list, sources and clear syntax");
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

    /**
     * Revoke without a source. Legacy removed only the {@code apoli:command} grant; Overgrown removes the power
     * from every source. A power held from apoli:command and another source loses only the command grant;
     * otherwise the native behavior applies, so Overgrown packs keep working.
     */
    public static int revokeDefault(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(context, "power");
        int affected = 0;
        for (Entity entity : EntityArgument.getEntities(context, "targets")) {
            PowerContainer container = PowerContainer.of(entity);
            if (container == null || !container.hasPower(power)) continue;
            Set<ResourceLocation> sources = container.sourcesOf(power);
            boolean removed = sources.contains(COMMAND_SOURCE) && sources.size() > 1
                ? container.removePower(power, COMMAND_SOURCE)
                : container.removePowerCompletely(power);
            if (removed) affected++;
        }
        final int count = affected;
        if (count == 0) {
            context.getSource().sendFailure(Component.literal("No target had " + power + "."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Revoked " + power + " from " + count + " target(s)."), true);
        return count;
    }

    private static int list(CommandContext<CommandSourceStack> context, boolean includeSubpowers) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        PowerContainer container = PowerContainer.of(target);
        List<String> powers = new ArrayList<>();
        if (container != null) {
            for (ResourceLocation power : container.allPowers()) {
                if (!includeSubpowers && isSubPower(container, power)) continue;
                powers.add(power + " [" + join(container.sourcesOf(power)) + "]");
            }
        }
        if (powers.isEmpty()) {
            context.getSource().sendFailure(Component.literal(target.getName().getString() + " has no powers."));
            return 0;
        }
        powers.sort(null);
        context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " has " + powers.size()
            + " power(s): " + String.join(", ", powers)), false);
        return powers.size();
    }

    private static int sources(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        ResourceLocation power = ResourceLocationArgument.getId(context, "power");
        PowerContainer container = PowerContainer.of(target);
        Set<ResourceLocation> sources = container == null || !container.hasPower(power) ? Set.of() : container.sourcesOf(power);
        if (sources.isEmpty()) {
            context.getSource().sendFailure(Component.literal(target.getName().getString() + " does not have " + power + "."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " has " + power + " from "
            + sources.size() + " source(s): " + join(sources)), false);
        return sources.size();
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int cleared = 0;
        for (Entity entity : EntityArgument.getEntities(context, "targets")) {
            PowerContainer container = PowerContainer.of(entity);
            if (container == null || container.isEmpty()) continue;
            cleared += container.allPowers().size();
            container.clear();
        }
        final int count = cleared;
        if (count == 0) {
            context.getSource().sendFailure(Component.literal("No target had any powers."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Cleared " + count + " power(s)."), true);
        return count;
    }

    /** Sub-powers are granted by their multiple power, using its ID as the source. */
    private static boolean isSubPower(PowerContainer container, ResourceLocation power) {
        Set<ResourceLocation> sources = container.sourcesOf(power);
        if (sources.isEmpty()) return false;
        for (ResourceLocation source : sources) {
            Power parent = ApoliPowers.get(source);
            if (parent == null || !(parent.config() instanceof MultiplePower.Cfg cfg) || !cfg.subPowerIds().contains(power)) return false;
        }
        return true;
    }

    private static String join(Set<ResourceLocation> ids) {
        return ids.stream().map(ResourceLocation::toString).sorted().collect(Collectors.joining(", "));
    }
}
