package org.overgrowns.migration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.OptionalInt;

/** Legacy /resource operations used by functions and command actions. */
public final class LegacyResourceCommands {
    private LegacyResourceCommands() {}

    public static void attach(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("apoli:resource");
        if (root == null) {
            LegacyBridge.LOGGER.error("Could not attach legacy resource operation: apoli:resource is unavailable");
            return;
        }
        if (root.getChild("operation") != null) return;
        root.addChild(Commands.literal("operation")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .then(Commands.argument("resource", ResourceLocationArgument.id())
                        .then(Commands.literal("=")
                            .then(Commands.argument("score_targets", EntityArgument.entities())
                                .then(Commands.argument("objective", StringArgumentType.word())
                                    .executes(ctx -> {
                                        Scoreboard board = ctx.getSource().getServer().getScoreboard();
                                        Objective objective = board.getObjective(
                                            StringArgumentType.getString(ctx, "objective"));
                                        if (objective == null) return 0;
                                        Collection<? extends Entity> scoreTargets =
                                            EntityArgument.getEntities(ctx, "score_targets");
                                        if (scoreTargets.isEmpty()) return 0;
                                        int value = board.getOrCreatePlayerScore(
                                            scoreTargets.iterator().next().getScoreboardName(), objective).getScore();
                                        return write(ctx.getSource(),
                                            EntityArgument.getEntities(ctx, "targets"),
                                            ResourceLocationArgument.getId(ctx, "resource"), value);
                                    })))))).build());
        LegacyBridge.LOGGER.info("Attached legacy /resource operation command");
    }

    private static int write(CommandSourceStack source, Collection<? extends Entity> targets,
                             ResourceLocation resource, int value)
            throws CommandSyntaxException {
        int changed = 0;
        for (Entity entity : targets) {
            PowerContainer holder = PowerContainer.of(entity);
            OptionalInt before = PowerResources.read(holder, resource);
            if (before.isEmpty()) continue;
            if (PowerResources.write(holder, resource, value).isPresent()) changed++;
        }
        final int count = changed;
        source.sendSuccess(() -> Component.literal("Updated " + resource + " for " + count + " target(s)."), true);
        return changed;
    }
}
