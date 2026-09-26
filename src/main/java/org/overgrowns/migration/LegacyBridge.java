package org.overgrowns.migration;

import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
                AliasingOptions.builder().addTypeAlias("origins:modify_insomnia_ticks").build());
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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LegacyPowerCommands.attach(dispatcher);
            LegacyResourceCommands.attach(dispatcher);
        });

        LOGGER.info("Overgrown Legacy Bridge active; legacy power revoke syntax will attach during command registration");
    }
}
