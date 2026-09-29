package org.overgrowns.migration.mixin;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import org.overgrowns.migration.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Runs after Fabric's command registration callback, independent of mod initialization order. */
@Mixin(value = Commands.class, priority = 500)
public abstract class LegacyCommandsMixin {
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void overgrownLegacyBridge$commands(CallbackInfo ci) {
        LegacyPowerCommands.attach(dispatcher);
        LegacyResourceCommands.attach(dispatcher);
        dispatcher.register(Commands.literal("legacybridge").requires(source -> source.hasPermission(2))
            .then(Commands.literal("report").executes(context -> {
                context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    LegacyCompatibilityReport.issueCount + " diagnostic candidate(s). Report: " + LegacyCompatibilityReport.path()), false);
                return LegacyCompatibilityReport.issueCount;
            })));
    }
}
