package org.overgrowns.migration;

import dev.overgrown.apoli.action.*;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LegacyBridge implements ModInitializer {
    public static final String MOD_ID = "overgrown_legacy_bridge";
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        if (ConditionTypes.ENTITY.get(new ResourceLocation("origins", "in_daylight")) == null) {
            ConditionTypes.ENTITY.aliases().registerTypeAlias(
                new ResourceLocation("origins", "in_daylight"),
                new ResourceLocation("apoli", "exposed_to_sun"));
            LOGGER.info("Registered origins:in_daylight condition alias");
        }
        ConditionTypes.ENTITY.register(new ResourceLocation(MOD_ID, "passive_mob"),
            new LegacyPassiveMobCondition());
        if (ConditionTypes.ENTITY.get(new ResourceLocation("origins", "damage_taken")) == null) {
            ConditionTypes.ENTITY.register(new ResourceLocation(MOD_ID, "damage_taken"),
                new LegacyDamageTakenCondition(), AliasingOptions.builder()
                    .addTypeAlias("origins:damage_taken")
                    .addTypeAlias("apoli:damage_taken")
                    .build());
        }
        ConditionTypes.DAMAGE.register(new ResourceLocation(MOD_ID, "attacker_distance"),
            new LegacyAttackerDistanceCondition());
        ResourceLocation webId = new ResourceLocation("origins", "temporary_cobweb");
        if (!BuiltInRegistries.BLOCK.containsKey(webId)) {
            Registry.register(BuiltInRegistries.BLOCK, webId, new TemporaryCobwebBlock());
            LOGGER.info("Registered decaying origins:temporary_cobweb block");
        }

        ResourceLocation insomniaId = new ResourceLocation(MOD_ID, "modify_insomnia_ticks");
        if (PowerTypeRegistry.get(new ResourceLocation("origins", "modify_insomnia_ticks")) == null) {
            PowerTypeRegistry.register(insomniaId, new LegacyInsomniaPower(),
                AliasingOptions.builder().addTypeAlias("origins:modify_insomnia_ticks").addTypeAlias("apoli:modify_insomnia_ticks").build());
            LOGGER.info("Registered legacy modify_insomnia_ticks power");
        }
        if (PowerTypeRegistry.get(new ResourceLocation("origins", "modify_gravity")) == null) {
            PowerTypeRegistry.register(LegacyGravityPower.ID,
                new LegacyGravityPower(), AliasingOptions.builder()
                    .addTypeAlias("origins:modify_gravity")
                    .addTypeAlias("apoli:modify_gravity")
                    .build());
        }
        PowerTypeRegistry.register(new ResourceLocation(MOD_ID, "mobs_ignore"),
            new LegacyMobsIgnorePower(), AliasingOptions.builder()
                .addTypeAlias("sync:mobs_ignore")
                .addTypeAlias("apugli:mobs_ignore")
                .build());
        if (PowerTypeRegistry.get(new ResourceLocation("origins", "status_bar_texture")) == null) {
            PowerTypeRegistry.register(LegacyStatusBarTexturePower.ID,
                new LegacyStatusBarTexturePower(), AliasingOptions.builder()
                    .addTypeAlias("origins:status_bar_texture")
                    .addTypeAlias("apoli:status_bar_texture")
                    .build());
        }

        ResourceLocation legacySet = new ResourceLocation("origins", "set_resource");
        if (ActionTypes.ENTITY.get(legacySet) == null) {
            ActionTypes.ENTITY.register(new ResourceLocation(MOD_ID, "set_resource"),
                new LegacySetResourceAction(),
                AliasingOptions.builder()
                    .addTypeAlias(legacySet)
                    .addTypeAlias("apoli:set_resource")
                    .build());
            LOGGER.info("Registered legacy set_resource action");
        } else {
            LOGGER.info("Apoli already provides set_resource; using its implementation");
        }

        // The old Origins Support pack uses the same bi-entity command fields as Apoli.
        ResourceLocation syncCommand = new ResourceLocation("sync", "execute_command");
        if (ActionTypes.BI_ENTITY.get(syncCommand) == null) {
            ActionTypes.BI_ENTITY.aliases().registerTypeAlias(
                syncCommand, new ResourceLocation("apoli", "execute_command"));
            LOGGER.info("Registered sync:execute_command alias");
        }

        ActionTypes.BI_ENTITY.register(new ResourceLocation(MOD_ID, "add_velocity_relative"),
            new LegacyRelativeVelocityAction());
        if (ActionTypes.BI_ENTITY.get(new ResourceLocation("origins", "modify_velocity")) == null) {
            ActionTypes.BI_ENTITY.register(new ResourceLocation(MOD_ID, "modify_velocity"),
                new LegacyModifyVelocityAction(),
                AliasingOptions.builder().addTypeAlias("origins:modify_velocity").build());
        }


        registerGeneralAdapters();
        LegacyResourceConditions.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(LegacyCompatibilityTests::runIfRequested);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(LegacyTickRates::sweepArmor);
        // Handler scopes are opened at a method's start and closed at its return; one that threw left its scope
        // open. No handler is running between ticks, so whatever is left there is stale.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> clearHandlerScopes());
        registerSaveMigration();
        LOGGER.info("Overgrown Legacy Bridge active; legacy power revoke syntax will attach during command registration");
    }
    public static void clearHandlerScopes() {
        LegacyAttributeTransferPower.clearScopes();
        LegacyModifierMath.clearDamage();
        LegacyDamageContext.clear();
    }
    /** Origins go in before Overgrown's join handler would prompt for them; power data after it re-grants powers. */
    private static void registerSaveMigration() {
        var join = net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN;
        var origins = new ResourceLocation(MOD_ID, "legacy_origins");
        var powerData = new ResourceLocation(MOD_ID, "legacy_power_data");
        join.addPhaseOrdering(origins, net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
        join.addPhaseOrdering(net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE, powerData);
        join.register(origins, (handler, sender, server) -> LegacySaveMigration.restoreOrigins(handler.player));
        join.register(powerData, (handler, sender, server) -> LegacySaveMigration.restorePowerData(handler.player));
    }
    private static void registerGeneralAdapters() {
        PowerTypeRegistry.register(LegacyDamageOverTimePower.ID, new LegacyDamageOverTimePower());
        var damageId = new ResourceLocation(MOD_ID, "damage");
        ActionTypes.ENTITY.register(damageId, new LegacyDamageAction<>(dev.overgrown.apoli.condition.context.EntityCtx::entity, ctx -> null));
        ActionTypes.BI_ENTITY.register(damageId, new LegacyDamageAction<>(dev.overgrown.apoli.condition.context.BiEntityCtx::target, dev.overgrown.apoli.condition.context.BiEntityCtx::actor));
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, player, alive) -> {
            // Legacy onRespawn ran only for death respawns, not when leaving the End.
            if (!alive && dev.overgrown.apoli.power.PowerContainer.of(player) instanceof dev.overgrown.apoli.power.PowerContainerImpl holder)
                for (var id : holder.powersOfType(LegacyDamageOverTimePower.ID)) LegacyDamageOverTimePower.reset(holder, id);
        });
        PowerTypeRegistry.register(LegacyToggleNightVisionPower.ID, new LegacyToggleNightVisionPower());
        if (PowerTypeRegistry.get(new ResourceLocation("origins", "modify_lava_speed")) == null)
            PowerTypeRegistry.register(LegacyLavaSpeedPower.ID, new LegacyLavaSpeedPower(), AliasingOptions.builder()
                .addTypeAlias("origins:modify_lava_speed").addTypeAlias("apoli:modify_lava_speed").build());
        PowerTypeRegistry.register(LegacyCameraSubmersionPower.ID, new LegacyCameraSubmersionPower());
        PowerTypeRegistry.register(LegacyGrindstonePower.ID, new LegacyGrindstonePower());
        if (PowerTypeRegistry.get(new ResourceLocation("origins", "attribute_modify_transfer")) == null)
            PowerTypeRegistry.register(LegacyAttributeTransferPower.ID, new LegacyAttributeTransferPower(), AliasingOptions.builder()
                .addTypeAlias("origins:attribute_modify_transfer").addTypeAlias("apoli:attribute_modify_transfer").build());
        if (PowerTypeRegistry.get(new ResourceLocation("origins", "modify_fluid_render")) == null)
            PowerTypeRegistry.register(LegacyFluidRenderPower.ID, new LegacyFluidRenderPower(), AliasingOptions.builder()
                .addTypeAlias("origins:modify_fluid_render").addTypeAlias("apoli:modify_fluid_render").build());
        var fluid = new ResourceLocation(MOD_ID, "fluid");
        if (ConditionTypes.FLUID.get(new ResourceLocation("origins", "fluid")) == null)
            ConditionTypes.FLUID.register(fluid, new LegacyFluidCondition(), AliasingOptions.builder()
                .addTypeAlias("origins:fluid").addTypeAlias("apoli:fluid").build());
        var side = new ResourceLocation(MOD_ID, "side");
        var aliases = AliasingOptions.builder().addTypeAlias("origins:side").addTypeAlias("apoli:side").build();
        ActionTypes.ENTITY.register(side, new LegacySideAction<>(EntityAction.CODEC, EntityAction::run, ctx -> ctx.level().isClientSide), aliases);
        ActionTypes.BI_ENTITY.register(side, new LegacySideAction<>(BiEntityAction.CODEC, BiEntityAction::run, ctx -> ctx.level().isClientSide), aliases);
        ActionTypes.BLOCK.register(side, new LegacySideAction<>(BlockAction.CODEC, BlockAction::run, ctx -> ctx.level().isClientSide), aliases);
        ActionTypes.ITEM.register(side, new LegacySideAction<>(ItemAction.CODEC, ItemAction::run, ctx -> ctx.level().isClientSide), aliases);
        ActionTypes.ENTITY.register(new ResourceLocation(MOD_ID, "modify_resource"), new LegacyModifyResourceAction());
    }

}
