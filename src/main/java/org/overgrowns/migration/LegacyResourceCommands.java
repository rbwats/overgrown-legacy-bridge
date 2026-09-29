package org.overgrowns.migration;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import dev.overgrown.apoli.power.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Score;
/** Use vanilla scoreboard parsing and operations, including fake players and floor arithmetic. */
public final class LegacyResourceCommands {
    private LegacyResourceCommands() {}
    public static void attach(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("apoli:resource");
        if (root == null || root.getChild("operation") != null) return;
        root.addChild(Commands.literal("operation")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("resource", ResourceLocationArgument.id())
                    .then(Commands.argument("operation", OperationArgument.operation())
                        .then(Commands.argument("score_targets", ScoreHolderArgument.scoreHolder())
                            .then(Commands.argument("objective", ObjectiveArgument.objective())
                                .executes(ctx -> {
                                    var board = ctx.getSource().getServer().getScoreboard();
                                    var objective = ObjectiveArgument.getObjective(ctx, "objective");
                                    var name = ScoreHolderArgument.getName(ctx, "score_targets");
                                    Score score = board.getOrCreatePlayerScore(name, objective);
                                    var operation = OperationArgument.getOperation(ctx, "operation");
                                    var resource = ResourceLocationArgument.getId(ctx, "resource");
                                    int affected = 0;
                                    for (var entity : EntityArgument.getEntities(ctx, "targets")) {
                                        PowerContainer holder = PowerContainer.of(entity);
                                        if (holder == null) continue;
                                        var current = PowerResources.read(holder, resource);
                                        if (current.isEmpty()) continue;
                                        Score working = new Score(board, objective, "overgrown_legacy_bridge:temporary") {
                                            private int value;
                                            @Override public int getScore() { return value; }
                                            @Override public void setScore(int next) { value = next; }
                                        };
                                        working.setScore(current.getAsInt());
                                        operation.apply(working, score);
                                        if (PowerResources.write(holder, resource, working.getScore()).isPresent()) affected++;
                                    }
                                    final int count = affected;
                                    ctx.getSource().sendSuccess(() -> Component.literal("Updated " + resource + " for " + count + " target(s)."), true);
                                    return affected;
                                })))))).build());
        LegacyBridge.LOGGER.info("Attached all legacy /resource operation operators");
    }
}
