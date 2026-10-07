package org.overgrowns.migration;
import com.google.gson.*;
import com.mojang.serialization.*;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.action.*;
import dev.overgrown.apoli.condition.*;
import dev.overgrown.apoli.condition.context.*;
import dev.overgrown.apoli.keybind.KeyDispatch;
import dev.overgrown.apoli.loader.ApoliReloadListener;
import dev.overgrown.apoli.power.*;
import dev.overgrown.apoli.power.builtin.NightVisionPower;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.util.*;
/** Behavioral regressions run only in the isolated opt-in compatibility server. */
public final class LegacyRegressionTests {
    private final JsonArray results = new JsonArray();
    private void test(String name, ThrowingRunnable body) {
        JsonObject row = new JsonObject(); row.addProperty("name", name);
        try { body.run(); row.addProperty("passed", true); }
        catch (Throwable error) { row.addProperty("passed", false); row.addProperty("error", error.toString()); LegacyBridge.LOGGER.error("REGRESSION {} failed", name, error); }
        results.add(row);
    }
    private interface ThrowingRunnable { void run() throws Exception; }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void eq(double actual, double expected) { check(Math.abs(actual - expected) < 1e-6, "Expected " + expected + ", got " + actual); }
    private static JsonObject json(String data) { return JsonParser.parseString(data).getAsJsonObject(); }
    private static JsonObject normalized(String data, LegacySchema.Context context) {
        JsonObject obj = json(data); LegacySchema.normalize(obj, context); return obj;
    }
    private static <T> T decode(Codec<T> codec, JsonElement data) { return codec.parse(JsonOps.INSTANCE, data).getOrThrow(false, message -> {}); }
    private static Power power(String data) {
        return power(new ResourceLocation("bridge_test", "test"), data);
    }
    /** Parses as the loader does under this id, so fields recorded from the raw JSON (tick_rate, priority) apply to it. */
    private static Power power(ResourceLocation id, String data) {
        JsonObject obj = normalized(data, LegacySchema.Context.POWER);
        return decode(Power.CODEC, ApoliReloadListener.prepare(new Dynamic<>(JsonOps.INSTANCE, obj), id).getValue());
    }
    public static JsonArray run(MinecraftServer server) {
        var suite = new LegacyRegressionTests();
        var level = server.overworld();
        Pig pig = EntityType.PIG.create(level);
        pig.setPos(level.getSharedSpawnPos().getX(), level.getSharedSpawnPos().getY(), level.getSharedSpawnPos().getZ());
        pig.addTag("legacy_bridge_test"); level.addFreshEntity(pig);
        PowerContainerImpl holder = (PowerContainerImpl) PowerContainerAttachment.getOrCreate(pig);
        Map<ResourceLocation,Power> original = new HashMap<>(ApoliPowers.view());
        var powers = new HashMap<>(original);
        var source = new ResourceLocation("bridge_test", "source");
        var resource = new ResourceLocation("bridge_test", "resource");
        var insomnia = new ResourceLocation("bridge_test", "insomnia");
        var insomnia2 = new ResourceLocation("bridge_test", "insomnia2");
        var insomniaOff = new ResourceLocation("bridge_test", "insomnia_off");
        var lava = new ResourceLocation("bridge_test", "lava");
        var night = new ResourceLocation("bridge_test", "night");
        var camera = new ResourceLocation("bridge_test", "camera");
        var dot = new ResourceLocation("bridge_test", "dot");
        var transfer = new ResourceLocation("bridge_test", "transfer");
        powers.put(resource, power("{\"type\":\"origins:resource\",\"min\":-100,\"max\":100,\"start_value\":10}"));
        powers.put(insomnia, power("{\"type\":\"origins:modify_insomnia_ticks\",\"modifier\":{\"operation\":\"addition\",\"value\":10}}"));
        powers.put(insomnia2, power("{\"type\":\"apoli:modify_insomnia_ticks\",\"modifiers\":[{\"operation\":\"multiply_total\",\"value\":1}]}"));
        powers.put(insomniaOff, power("{\"type\":\"origins:modify_insomnia_ticks\",\"condition\":{\"type\":\"origins:constant\",\"value\":false},\"modifier\":{\"operation\":\"addition\",\"value\":1000}}"));
        powers.put(lava, power("{\"type\":\"origins:modify_lava_speed\",\"modifier\":{\"operation\":\"addition\",\"value\":0.2}}"));
        powers.put(night, power("{\"type\":\"origins:toggle_night_vision\",\"strength\":0.6,\"key\":{\"key\":\"origins.primary_active\",\"continous\":false}}"));
        powers.put(dot, power("{\"type\":\"origins:damage_over_time\",\"damage\":1,\"interval\":2,\"onset_delay\":0}"));
        powers.put(camera, power("{\"type\":\"origins:modify_camera_submersion\",\"to\":\"water\"}"));
        powers.put(transfer, power("{\"type\":\"origins:attribute_modify_transfer\",\"class\":\"modify_air_speed\",\"attribute\":\"minecraft:generic.movement_speed\"}"));
        var jumpTransfer = new ResourceLocation("bridge_test", "jump_transfer");
        powers.put(jumpTransfer, power("{\"type\":\"origins:attribute_modify_transfer\",\"class\":\"ModifyJumpPower\",\"attribute\":\"minecraft:generic.movement_speed\",\"multiplier\":2}"));
        var grind = new ResourceLocation("bridge_test", "grind");
        powers.put(grind, power(grind, "{\"type\":\"origins:modify_grindstone\",\"result_type\":\"SPECIFIED\",\"result_stack\":{\"item\":\"minecraft:diamond\",\"amount\":2},\"bottom_condition\":{\"type\":\"origins:empty\"}}"));
        var xpGain = new ResourceLocation("bridge_test", "xp_gain");
        powers.put(xpGain, power(xpGain, "{\"type\":\"origins:modify_xp_gain\",\"modifier\":{\"operation\":\"multiply_total\",\"value\":1}}"));
        var useZero = new ResourceLocation("bridge_test", "use_zero");
        powers.put(useZero, power(useZero, "{\"type\":\"origins:action_on_entity_use\",\"bientity_action\":{\"type\":\"origins:target_action\",\"action\":{\"type\":\"origins:heal\",\"amount\":1}}}"));
        var useFirst = new ResourceLocation("bridge_test", "use_first");
        powers.put(useFirst, power(useFirst, "{\"type\":\"origins:action_on_entity_use\",\"priority\":1,\"action_result\":\"consume\",\"bientity_action\":{\"type\":\"origins:target_action\",\"action\":{\"type\":\"origins:heal\",\"amount\":2}}}"));
        var unconditional = new ResourceLocation("bridge_test", "unconditional_attribute");
        powers.put(unconditional, power(unconditional, "{\"type\":\"origins:attribute\",\"condition\":{\"type\":\"origins:constant\",\"value\":false},\"modifier\":{\"attribute\":\"minecraft:generic.armor\",\"operation\":\"addition\",\"value\":3}}"));
        var legacyJump = new ResourceLocation("bridge_test", "legacy_jump");
        powers.put(legacyJump, power(legacyJump, "{\"type\":\"origins:modify_jump\",\"modifiers\":[{\"operation\":\"multiply_base\",\"value\":0.5},{\"operation\":\"multiply_base\",\"value\":0.5},{\"operation\":\"addition\",\"value\":1}]}"));
        var nestedJump = new ResourceLocation("bridge_test", "nested_jump");
        powers.put(nestedJump, power(nestedJump, "{\"type\":\"apoli:modify_jump\",\"modifier\":{\"operation\":\"addition\",\"value\":1,\"modifier\":[{\"operation\":\"multiply_total\",\"value\":1},{\"operation\":\"addition\",\"value\":2}]}}"));
        var namedNestedJump = new ResourceLocation("bridge_test", "named_nested_jump");
        powers.put(namedNestedJump, power(namedNestedJump, "{\"type\":\"origins:modify_jump\",\"modifier\":{\"name\":\"kept\",\"operation\":\"addition\",\"value\":1,\"modifier\":[{\"operation\":\"multiply_total\",\"value\":1}]}}"));
        var yVelocity = new ResourceLocation("bridge_test", "y_velocity");
        powers.put(yVelocity, power(yVelocity, "{\"type\":\"origins:modify_velocity\",\"axes\":[\"y\"],\"modifier\":{\"operation\":\"addition\",\"value\":1}}"));
        var velocityTransfer = new ResourceLocation("bridge_test", "velocity_transfer");
        powers.put(velocityTransfer, power(velocityTransfer, "{\"type\":\"origins:attribute_modify_transfer\",\"class\":\"modify_velocity\",\"attribute\":\"minecraft:generic.movement_speed\"}"));
        var cooldown = new ResourceLocation("bridge_test", "cooldown");
        powers.put(cooldown, power(cooldown, "{\"type\":\"origins:cooldown\",\"cooldown\":100}"));
        var slowAttribute = new ResourceLocation("bridge_test", "slow_attribute");
        powers.put(slowAttribute, power(slowAttribute, "{\"type\":\"origins:conditioned_attribute\",\"tick_rate\":5,\"modifier\":{\"attribute\":\"minecraft:generic.armor\",\"operation\":\"addition\",\"value\":1}}"));
        ApoliPowers.replaceAll(powers);
        try {
            for (var id : List.of(resource, insomnia, insomnia2, insomniaOff, lava, night, dot, camera, transfer, jumpTransfer)) check(holder.addPower(id, source), "Cannot grant " + id);
            suite.test("insomnia: aggregate active modifiers without modifying state", () -> {
                eq(LegacyInsomniaPower.modify(pig, 100), 220); eq(holder.getAuxIntOr(resource, -1), 10);
                holder.suppressPower(insomnia2, source); eq(LegacyInsomniaPower.modify(pig, 100), 110); holder.unsuppressPower(insomnia2, source);
            });
            suite.test("lava: 10-tick condition updates, modifier arithmetic and native travel injection", () -> {
                eq(LegacyLavaSpeedPower.modify(pig, 0.5), 0.5);
                var lavaType = new LegacyLavaSpeedPower(); var lavaCfg = (LegacyLavaSpeedPower.Config) powers.get(lava).config();
                int savedTick = pig.tickCount;
                pig.tickCount = 11; lavaType.tick(lava, lavaCfg, holder); eq(LegacyLavaSpeedPower.modify(pig, 0.5), 0.5);
                pig.tickCount = 20; lavaType.tick(lava, lavaCfg, holder); eq(LegacyLavaSpeedPower.modify(pig, 0.5), 0.7);
                pig.tickCount = savedTick;
                pig.travel(net.minecraft.world.phys.Vec3.ZERO);
                holder.suppressPower(lava, source); eq(LegacyLavaSpeedPower.modify(pig, 0.5), 0.5); holder.unsuppressPower(lava, source);
            });
            suite.test("toggle night vision: defaults, key dispatch, active condition, suppression", () -> {
                eq(NightVisionPower.strengthFor(pig), 0);
                check(KeyDispatch.press(pig, "key.origins.primary_active") == 1, "Legacy control did not dispatch");
                eq(NightVisionPower.strengthFor(pig), 0.6);
                var condition = decode(EntityCondition.CODEC, json("{\"type\":\"origins:power_active\",\"power\":\"bridge_test:night\"}"));
                check(condition.test(new EntityCtx(pig, level)), "Toggled power is not active");
                holder.suppressPower(night, source); eq(NightVisionPower.strengthFor(pig), 0); holder.unsuppressPower(night, source);
                KeyDispatch.press(pig, "key.origins.primary_active");
                eq(NightVisionPower.strengthFor(pig), 0); check(!condition.test(new EntityCtx(pig, level)), "Disabled night vision is active");
            });
            suite.test("toggle: persisted state survives container round trip", () -> {
                KeyDispatch.press(pig, "key.origins.primary_active");
                var encoded = PowerContainerImpl.CODEC.encodeStart(JsonOps.INSTANCE, holder).getOrThrow(false, message -> {});
                var restored = decode(PowerContainerImpl.CODEC, encoded);
                eq(restored.getAuxIntOr(night, -1), 1); KeyDispatch.press(pig, "key.origins.primary_active");
            });
            suite.test("modify_resource: singular and plural, legacy truncation", () -> {
                PowerResources.write(holder, resource, 3);
                var action = decode(EntityAction.CODEC, normalized("{\"type\":\"origins:modify_resource\",\"resource\":\"bridge_test:resource\",\"modifier\":{\"operation\":\"multiply_total\",\"value\":-0.5}}", LegacySchema.Context.ENTITY_ACTION));
                action.run(new EntityCtx(pig, level)); eq(PowerResources.read(holder, resource).orElseThrow(), 1);
                action = decode(EntityAction.CODEC, normalized("{\"type\":\"origins:modify_resource\",\"resource\":\"bridge_test:resource\",\"modifier\":{\"operation\":\"addition\",\"value\":2},\"modifiers\":[{\"operation\":\"multiply_total\",\"value\":1}]}", LegacySchema.Context.ENTITY_ACTION));
                action.run(new EntityCtx(pig, level)); eq(PowerResources.read(holder, resource).orElseThrow(), 6);
            });
            var board = server.getScoreboard();
            var objective = board.addObjective("bridge_test", ObjectiveCriteria.DUMMY, net.minecraft.network.chat.Component.literal("Bridge test"), ObjectiveCriteria.RenderType.INTEGER);
            var score = board.getOrCreatePlayerScore("$fake", objective);
            String[] ops = {"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"};
            int[] initial = {10,10,10,10,-7,-7,10,10,10};
            int[] operand = {3,3,3,3,3,3,3,13,3};
            int[] expected = {3,13,7,30,-3,2,3,13,3};
            for (int index=0;index<ops.length;index++) {
                final int n=index;
                suite.test("resource command: " + ops[n] + " with fake scoreboard holder", () -> {
                    PowerResources.write(holder, resource, initial[n]); score.setScore(operand[n]);
                    int result = server.getCommands().getDispatcher().execute("resource operation @e[tag=legacy_bridge_test,limit=1] bridge_test:resource " + ops[n] + " $fake bridge_test", server.createCommandSourceStack().withSuppressedOutput());
                    eq(result, 1); eq(PowerResources.read(holder, resource).orElseThrow(), expected[n]);
                    eq(score.getScore(), ops[n].equals("><") ? initial[n] : operand[n]);
                });
            }
            suite.test("resource command: clamp and divide-by-zero atomicity", () -> {
                PowerResources.write(holder, resource, 90); score.setScore(50);
                server.getCommands().getDispatcher().execute("apoli:resource operation @e[tag=legacy_bridge_test,limit=1] bridge_test:resource += $fake bridge_test", server.createCommandSourceStack().withSuppressedOutput());
                eq(PowerResources.read(holder, resource).orElseThrow(), 100);
                score.setScore(0);
                try { server.getCommands().getDispatcher().execute("resource operation @e[tag=legacy_bridge_test,limit=1] bridge_test:resource /= $fake bridge_test", server.createCommandSourceStack()); throw new AssertionError("Division by zero accepted"); }
                catch (com.mojang.brigadier.exceptions.CommandSyntaxException expectedError) { eq(PowerResources.read(holder, resource).orElseThrow(), 100); }
            });
            board.removeObjective(objective);
            suite.test("fluid identity: still fluid does not equal flowing fluid", () -> {
                var condition = decode(FluidCondition.CODEC, json("{\"type\":\"origins:fluid\",\"fluid\":\"minecraft:water\"}"));
                check(condition.test(new FluidCtx(Fluids.WATER.defaultFluidState(), BlockPos.ZERO, level)), "Still water failed");
                check(!condition.test(new FluidCtx(Fluids.FLOWING_WATER.defaultFluidState(), BlockPos.ZERO, level)), "Flowing water incorrectly matched");
            });
            suite.test("material: singular/plural union", () -> {
                var condition = decode(BlockCondition.CODEC, normalized("{\"type\":\"origins:material\",\"material\":\"wood\",\"materials\":[\"stone\"]}", LegacySchema.Context.BLOCK_CONDITION));
                check(condition.test(new BlockCtx(BlockPos.ZERO, Blocks.STONE.defaultBlockState(), level)), "Stone tag failed");
                check(condition.test(new BlockCtx(BlockPos.ZERO, Blocks.OAK_PLANKS.defaultBlockState(), level)), "Wood tag failed");
                check(!condition.test(new BlockCtx(BlockPos.ZERO, Blocks.AIR.defaultBlockState(), level)), "Air matched");
            });
            suite.test("held power ID condition: does not compare factory type", () -> {
                var condition = decode(EntityCondition.CODEC, normalized("{\"type\":\"origins:power_type\",\"power_type\":\"bridge_test:resource\"}", LegacySchema.Context.ENTITY_CONDITION));
                check(condition.test(new EntityCtx(pig, level)), "Held power ID failed");
            });
            suite.test("damage source: source flags take priority and death name is retained", () -> {
                var spec = decode(LegacyDamageSource.CODEC, json("{\"name\":\"bridge_test\",\"fire\":true}"));
                var damage = spec.create(level, null);
                check(damage.is(net.minecraft.tags.DamageTypeTags.IS_FIRE), "Fire flag lost");
                check(((TranslatableContents)damage.getLocalizedDeathMessage(pig).getContents()).getKey().equals("death.attack.bridge_test"), "Custom death key lost");
            });
            suite.test("damage over time: exact onset and interval", () -> {
                var type = new LegacyDamageOverTimePower(); var cfg = (LegacyDamageOverTimePower.Config)powers.get(dot).config();
                pig.setHealth(pig.getMaxHealth()); float starting=pig.getHealth(); pig.invulnerableTime=0; type.tick(dot,cfg,holder); float health=pig.getHealth(); eq(health,starting-1);
                pig.invulnerableTime=0; type.tick(dot,cfg,holder); eq(pig.getHealth(),health);
                pig.invulnerableTime=0; type.tick(dot,cfg,holder); eq(pig.getHealth(),health-1);
            });
            suite.test("damage over time: protection adds level plus enchanted-item count", () -> {
                var cfg=(LegacyDamageOverTimePower.Config)power("{\"type\":\"origins:damage_over_time\",\"damage\":1,\"protection_enchantment\":\"minecraft:respiration\"}").config();
                ItemStack helmet=new ItemStack(Items.DIAMOND_HELMET);helmet.enchant(Enchantments.RESPIRATION,1);pig.setItemSlot(EquipmentSlot.HEAD,helmet);
                eq(LegacyDamageOverTimePower.onset(cfg,pig),140);pig.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            });
            suite.test("normalizer: NBT, recipe and arbitrary payloads are untouched", () -> {
                var input=json("{\"type\":\"origins:active_self\",\"entity_action\":{\"type\":\"origins:nothing\"},\"nbt\":{\"type\":\"origins:launch\",\"name\":\"payload\"},\"recipe\":{\"type\":\"origins:damage\",\"name\":\"recipe\"}}");
                JsonElement nbt=input.get("nbt").deepCopy(),recipe=input.get("recipe").deepCopy(); LegacySchema.normalize(input,LegacySchema.Context.POWER);
                check(nbt.equals(input.get("nbt")) && recipe.equals(input.get("recipe")),"Payload corrupted");
            });
            suite.test("normalizer: context disambiguates item damage and nested side/chance actions", () -> {
                var item=normalized("{\"type\":\"origins:damage\",\"amount\":1}",LegacySchema.Context.ITEM_ACTION);check(!item.has("damage_type"),"Item action got an entity damage type");
                var nested=normalized("{\"type\":\"origins:chance\",\"chance\":0,\"action\":{\"type\":\"origins:nothing\"},\"fail_action\":{\"type\":\"origins:side\",\"side\":\"server\",\"action\":{\"type\":\"origins:and\",\"actions\":{\"type\":\"origins:nothing\"}}}}",LegacySchema.Context.ITEM_ACTION);
                decode(ItemAction.CODEC,nested);
                check(nested.getAsJsonObject("fail_action").getAsJsonObject("action").get("actions").isJsonArray(),"Fail branch did not normalize");
            });
            suite.test("normalizer: combined status effects survive idempotent normalization", () -> {
                var effects=normalized("{\"type\":\"origins:apply_effect\",\"effect\":{\"effect\":\"minecraft:speed\",\"duration\":10},\"effects\":[{\"effect\":\"minecraft:strength\",\"duration\":10}]}",LegacySchema.Context.ENTITY_ACTION);
                eq(effects.getAsJsonArray("effect").size(),2);JsonObject once=effects.deepCopy();LegacySchema.normalize(effects,LegacySchema.Context.ENTITY_ACTION);check(once.equals(effects),"Second pass changed data");decode(EntityAction.CODEC,effects);
            });
            suite.test("camera submersion: omitted from matches every original fluid", () -> {
                for (var fog : net.minecraft.world.level.material.FogType.values())
                    check(dev.overgrown.apoli.power.builtin.ModifyCameraSubmersionPower.remap(pig, fog) == net.minecraft.world.level.material.FogType.WATER, "Omitted from failed for " + fog);
                holder.suppressPower(camera, source);
                check(dev.overgrown.apoli.power.builtin.ModifyCameraSubmersionPower.remap(pig, net.minecraft.world.level.material.FogType.LAVA) == net.minecraft.world.level.material.FogType.LAVA,"Suppressed camera power applied");
                holder.unsuppressPower(camera, source);
            });
            suite.test("layer overlays: accumulate exclusions and merge partial GUI titles", () -> {
                var before=json("{\"exclude_random\":[\"example:a\"],\"gui_title\":{\"choose_origin\":\"example.choose\"}}");
                var incoming=json("{\"exclude_random\":[\"example:b\"],\"gui_title\":{\"view_origin\":\"example.view\"}}");
                LegacyOriginNormalizer.mergeLayerFields(before,incoming);
                eq(incoming.getAsJsonArray("exclude_random").size(),2);
                check(incoming.getAsJsonObject("gui_title").has("choose_origin"),"Partial title override erased choose title");
                incoming=json("{\"exclude_random\":[\"example:b\"],\"replace_exclude_random\":true}");
                LegacyOriginNormalizer.mergeLayerFields(before,incoming);eq(incoming.getAsJsonArray("exclude_random").size(),1);
            });
            suite.test("schema: bi-entity fields do not become entity fields", () -> {
                var data=json("{\"type\":\"origins:raycast\",\"bientity_action\":{\"type\":\"origins:target_action\",\"action\":{\"type\":\"origins:nothing\"}}}");
                var contexts=new ArrayList<LegacySchema.Context>();
                LegacySchema.walk(data,LegacySchema.Context.ENTITY_ACTION,"$",(ctx,node,path) -> {
                    if (path.equals("$.bientity_action")) contexts.add(ctx);
                });
                check(contexts.equals(List.of(LegacySchema.Context.BI_ENTITY_ACTION)),"Bi-entity context was inferred as entity");
            });
            suite.test("keys: Origins' bound primary control is kept, legacy aliases map onto it", () -> {
                var kept = normalized("{\"type\":\"origins:active_self\",\"key\":\"key.origins.primary_active\"}", LegacySchema.Context.POWER);
                check(kept.get("key").getAsString().equals("key.origins.primary_active"), "Origins primary key was rewritten");
                var alias = normalized("{\"type\":\"apugli:addon_power\",\"key\":{\"key\":\"secondary\"}}", LegacySchema.Context.POWER);
                check(alias.getAsJsonObject("key").get("key").getAsString().equals("key.origins.secondary_active"), "Legacy secondary alias not mapped");
            });
            suite.test("damage source: legacy name is the message ID matched by origins:name", () -> {
                var damage = decode(LegacyDamageSource.CODEC, json("{\"name\":\"bridge_named\",\"magic\":true}")).create(level, null);
                check(damage.getMsgId().equals("bridge_named"), "Message ID is " + damage.getMsgId());
            });
            suite.test("built-in IDs: legacy Origins powers load under their original IDs", () -> {
                check(original.containsKey(new ResourceLocation("origins", "fall_immunity")), "origins:fall_immunity missing");
                check(original.containsKey(new ResourceLocation("origins", "like_air")), "origins:like_air failed to load");
                check(original.containsKey(new ResourceLocation("origins", "master_of_webs_no_slowdown")), "Legacy sub-power IDs missing");
                var breathing = original.get(new ResourceLocation("origins", "water_breathing"));
                check(breathing != null && breathing.typeId().equals(new ResourceLocation("apoli", "water_breathing")), "water_breathing is not native");
                check(original.containsKey(new ResourceLocation("origins", "scare_creepers_flee")), "scare_creepers was not expanded");
            });
            suite.test("attribute transfer: modifiers reach air speed and jump; legacy class names resolve as Calio did", () -> {
                var speed = pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(UUID.randomUUID(), "bridge_test", 0.5,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL);
                speed.addTransientModifier(modifier);
                try {
                    eq(dev.overgrown.apoli.power.builtin.ModifyAirSpeedPower.modify(pig, 0.02f), 0.03f);
                    eq(dev.overgrown.apoli.power.builtin.ModifyJumpHandler.modify(pig, 0.5f), 1.0f);
                } finally { speed.removeModifier(modifier); }
                eq(dev.overgrown.apoli.power.builtin.ModifyJumpHandler.modify(pig, 0.5f), 0.5f);
                for (String name : List.of("modify_swim_speed", "io.github.apace100.apoli.power.ModifyHealingPower", "modifyExhaustion"))
                    power("{\"type\":\"origins:attribute_modify_transfer\",\"class\":\"" + name + "\",\"attribute\":\"minecraft:generic.armor\"}");
                var missing = Power.CODEC.parse(JsonOps.INSTANCE, ApoliReloadListener.prepare(new Dynamic<>(JsonOps.INSTANCE,
                    normalized("{\"type\":\"origins:attribute_modify_transfer\",\"class\":\"modify_nothing_at_all\",\"attribute\":\"minecraft:generic.armor\"}", LegacySchema.Context.POWER)),
                    new ResourceLocation("bridge_test", "missing")).getValue());
                check(missing.error().isPresent(), "A class legacy Apoli could not resolve was accepted");
            });
            suite.test("mixin targets: server-side injections apply", () -> {
                // Built from class literals so the names are right in both development and production mappings.
                String grindstone = net.minecraft.world.inventory.GrindstoneMenu.class.getName();
                for (String name : List.of(grindstone + "$2", grindstone + "$3", grindstone + "$4",
                        net.minecraft.server.PlayerAdvancements.class.getName(), ExperienceOrb.class.getName(),
                        dev.overgrown.apoli.power.builtin.ActionOnUseHandler.class.getName(), dev.overgrown.apoli.power.builtin.AttributePower.class.getName()))
                    Class.forName(name, true, LegacyRegressionTests.class.getClassLoader());
            });
            suite.test("tick_rate: conditioned_attribute re-checks every tick_rate ticks", () -> {
                int saved = pig.tickCount;
                try {
                    pig.tickCount = 10; check(LegacyTickRates.attributeTicks(slowAttribute, pig), "Tick 10 skipped at rate 5");
                    pig.tickCount = 11; check(!LegacyTickRates.attributeTicks(slowAttribute, pig), "Tick 11 ran at rate 5");
                    check(LegacyTickRates.attributeTicks(resource, pig), "Power without a legacy rate was gated");
                } finally { pig.tickCount = saved; }
            });
            var fake = net.fabricmc.fabric.api.entity.FakePlayer.get(level);
            var fakeHolder = (PowerContainerImpl) PowerContainerAttachment.getOrCreate(fake);
            suite.test("modify_grindstone: inputs, result and conditions apply", () -> {
                fakeHolder.addPower(grind, source);
                try {
                    var menu = new net.minecraft.world.inventory.GrindstoneMenu(0, fake.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.NULL);
                    check(menu.getSlot(0).mayPlace(new ItemStack(Items.STICK)), "Top slot refused an item the power allows");
                    check(!menu.getSlot(1).mayPlace(new ItemStack(Items.STICK)), "Bottom condition was ignored");
                    menu.getSlot(0).set(new ItemStack(Items.STICK));
                    var result = menu.getSlot(2).getItem();
                    check(result.is(Items.DIAMOND) && result.getCount() == 2, "Result was " + result);
                    menu.removed(fake);
                } finally { fakeHolder.removePower(grind, source); }
            });
            suite.test("modify_xp_gain: experience orbs pay the modified value", () -> {
                fakeHolder.addPower(xpGain, source);
                try {
                    int before = fake.totalExperience;
                    fake.takeXpDelay = 0;
                    var orb = new ExperienceOrb(level, fake.getX(), fake.getY(), fake.getZ(), 10);
                    orb.playerTouch(fake);
                    eq(fake.totalExperience - before, 20);
                } finally { fakeHolder.removePower(xpGain, source); }
            });
            suite.test("entity use priority: 0 keeps vanilla running, positive cancels it", () -> {
                fakeHolder.addPower(useZero, source);
                try {
                    pig.setHealth(2);
                    check(dev.overgrown.apoli.power.builtin.ActionOnUseHandler.fire(fake, pig, net.minecraft.world.InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS, "Priority 0 cancelled vanilla");
                    eq(pig.getHealth(), 3);
                    check(fake.interactOn(pig, net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(), "Priority 0 result was not applied after vanilla");
                    fakeHolder.addPower(useFirst, source);
                    check(dev.overgrown.apoli.power.builtin.ActionOnUseHandler.fire(fake, pig, net.minecraft.world.InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.CONSUME, "Priority 1 did not cancel vanilla");
                    eq(pig.getHealth(), 5);
                } finally { fakeHolder.removePower(useZero, source); fakeHolder.removePower(useFirst, source); pig.setHealth(pig.getMaxHealth()); }
            });
            suite.test("save migration: legacy origins, sourced powers and resource values", () -> {
                var state = dev.overgrown.origins.component.PlayerOriginsAttachment.getOrCreate(fake);
                for (var layer : new ArrayList<>(state.snapshot().keySet())) state.clearOrigin(layer);
                var save = new net.minecraft.nbt.CompoundTag();
                var components = new net.minecraft.nbt.CompoundTag();
                var originTag = new net.minecraft.nbt.CompoundTag(); var layers = new net.minecraft.nbt.ListTag(); var layerTag = new net.minecraft.nbt.CompoundTag();
                layerTag.putString("Layer", "origins:origin"); layerTag.putString("Origin", "origins:avian"); layers.add(layerTag);
                originTag.put("OriginLayers", layers); originTag.putBoolean("HadOriginBefore", true); components.put("origins:origin", originTag);
                var powerTag = new net.minecraft.nbt.CompoundTag(); var powerList = new net.minecraft.nbt.ListTag(); var entry = new net.minecraft.nbt.CompoundTag();
                entry.putString("Type", resource.toString()); var sources = new net.minecraft.nbt.ListTag(); sources.add(net.minecraft.nbt.StringTag.valueOf("apoli:command"));
                entry.put("Sources", sources); entry.put("Data", net.minecraft.nbt.IntTag.valueOf(42)); powerList.add(entry);
                powerTag.put("Powers", powerList); components.put("apoli:powers", powerTag); save.put(LegacySaveMigration.COMPONENTS, components);
                var legacy = LegacySaveMigration.capture(save);
                check(legacy != null, "Legacy components were not captured");
                var written = new net.minecraft.nbt.CompoundTag(); LegacySaveMigration.writeBack(written, legacy);
                check(written.getCompound(LegacySaveMigration.COMPONENTS).contains("origins:origin"), "Unmigrated data would be lost on save");
                var holderView = (LegacySaveMigration.Holder) fake;
                holderView.overgrownLegacyBridge$setLegacyComponents(legacy);
                try {
                    LegacySaveMigration.restoreOrigins(fake);
                    check(new ResourceLocation("origins", "avian").equals(state.getOrigin(new ResourceLocation("origins", "origin"))), "Origin not restored: " + state.snapshot());
                    check(fakeHolder.sourcesOf(resource).contains(new ResourceLocation("apoli", "command")), "Command-granted power not restored");
                    LegacySaveMigration.restorePowerData(fake);
                    eq(PowerResources.read(fakeHolder, resource).orElseThrow(), 42);
                    check(holderView.overgrownLegacyBridge$legacyComponents() == null, "Migrated data kept for another pass");
                } finally {
                    holderView.overgrownLegacyBridge$setLegacyComponents(null);
                    for (var layer : new ArrayList<>(state.snapshot().keySet())) state.clearOrigin(layer);
                    fakeHolder.clear();
                }
            });
            suite.test("origins:attribute: applies regardless of its condition, as legacy did", () -> {
                var armor = pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
                double before = armor.getValue();
                check(LegacyTickRates.ignoresCondition(unconditional), "origins:attribute was not recorded");
                holder.addPower(unconditional, source);
                try { eq(armor.getValue(), before + 3); }
                finally { holder.removePower(unconditional, source); }
                eq(armor.getValue(), before);
            });
            suite.test("modifier engine: legacy lists use grouped legacy math and merge transfers", () -> {
                holder.addPower(legacyJump, source);
                var speed = pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(UUID.randomUUID(), "bridge_test", 0.5,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE);
                try {
                    // Legacy: 10 + 1 = 11, then + 10 * (0.5 + 0.5); Overgrown would compound to 24.75.
                    eq(dev.overgrown.apoli.power.builtin.ModifyJumpHandler.modify(pig, 10f), 21);
                    // The jump transfer (multiplier 2) joins the same multiply_base group: 11 + 10 * 2.
                    speed.addTransientModifier(modifier);
                    eq(dev.overgrown.apoli.power.builtin.ModifyJumpHandler.modify(pig, 10f), 31);
                } finally { speed.removeModifier(modifier); holder.removePower(legacyJump, source); }
            });
            suite.test("modifier engine: multi-element nested modifier lists apply as a group", () -> {
                holder.addPower(nestedJump, source);
                // Nested: 1 + 2 = 3, times 2 = 6; outer addition: 10 + 6.
                try { eq(dev.overgrown.apoli.power.builtin.ModifyJumpHandler.modify(pig, 10f), 16); }
                finally { holder.removePower(nestedJump, source); }
            });
            suite.test("modifier engine: a named modifier keeps its single nested modifier", () -> {
                holder.addPower(namedNestedJump, source);
                // Nested: 1 doubled = 2; outer addition: 10 + 2.
                try { eq(dev.overgrown.apoli.power.builtin.ModifyJumpHandler.modify(pig, 10f), 12); }
                finally { holder.removePower(namedNestedJump, source); }
            });
            suite.test("attribute transfer: velocity transfers reach every axis, and a scope merges once", () -> {
                var speed = pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(UUID.randomUUID(), "bridge_test", 0.5,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL);
                holder.addPower(yVelocity, source); holder.addPower(velocityTransfer, source);
                speed.addTransientModifier(modifier);
                try {
                    // Y: (1 + 1) * 1.5 in one legacy pass; X and Z have no velocity modifiers and still take the transfer.
                    var moved = dev.overgrown.apoli.power.builtin.ModifyVelocityHandler.modify(pig, new net.minecraft.world.phys.Vec3(1, 1, 1));
                    eq(moved.x, 1.5); eq(moved.y, 3); eq(moved.z, 1.5);
                    LegacyAttributeTransferPower.begin(pig, "modify_velocity");
                    eq(LegacyAttributeTransferPower.afterPass(pig, 2), 3);
                    eq(LegacyAttributeTransferPower.afterPass(pig, 2), 2);
                    eq(LegacyAttributeTransferPower.end(2.0), 2);
                } finally {
                    LegacyAttributeTransferPower.clearScopes();
                    speed.removeModifier(modifier); holder.removePower(yVelocity, source); holder.removePower(velocityTransfer, source);
                }
            });
            suite.test("normalizer: enum fields of types redirected to the bridge accept legacy case", () ->
                power("{\"type\":\"origins:modify_camera_submersion\",\"to\":\"WATER\"}"));
            suite.test("badges: toggle night vision shows the toggle badge", () -> {
                var badges = dev.overgrown.origins.badge.BadgeManager.collectForSend(server).get(night);
                check(badges != null && !badges.isEmpty() && badges.get(0) instanceof dev.overgrown.origins.badge.KeybindBadge keybind
                    && keybind.text().equals("origins.gui.badge.toggle"), "Badges were " + badges);
            });
            suite.test("layers: missing order reproduces the legacy load index", () -> {
                var ids = List.of(new ResourceLocation("origins", "origin"), new ResourceLocation("mypack", "class"), new ResourceLocation("other", "race"));
                var indices = LegacyOriginNormalizer.legacyLayerIndices(ids);
                check(new TreeSet<>(indices.values()).equals(new TreeSet<>(List.of(0, 1, 2))), "Indices " + indices);
                var layer = json("{\"origins\":[]}"); LegacyOriginNormalizer.defaultLayerOrder(layer, indices.get(ids.get(1)));
                eq(layer.get("order").getAsInt(), indices.get(ids.get(1)));
                var ordered = json("{\"order\":7}"); LegacyOriginNormalizer.defaultLayerOrder(ordered, 3); eq(ordered.get("order").getAsInt(), 7);
            });
            suite.test("save migration: cooldowns resume and damage-over-time timers carry over", () -> {
                var save = new net.minecraft.nbt.CompoundTag();
                var components = new net.minecraft.nbt.CompoundTag(); var powerTag = new net.minecraft.nbt.CompoundTag(); var list = new net.minecraft.nbt.ListTag();
                var command = new net.minecraft.nbt.ListTag(); command.add(net.minecraft.nbt.StringTag.valueOf("apoli:command"));
                var cd = new net.minecraft.nbt.CompoundTag(); cd.putString("Type", cooldown.toString()); cd.put("Sources", command.copy());
                cd.put("Data", net.minecraft.nbt.LongTag.valueOf(level.getGameTime() - 40)); list.add(cd);
                var timer = new net.minecraft.nbt.CompoundTag(); timer.putString("Type", dot.toString()); timer.put("Sources", command.copy());
                var timerData = new net.minecraft.nbt.CompoundTag(); timerData.putInt("InDamage", 5); timerData.putInt("OutDamage", 3); timer.put("Data", timerData); list.add(timer);
                powerTag.put("Powers", list); components.put("apoli:powers", powerTag); save.put(LegacySaveMigration.COMPONENTS, components);
                var holderView = (LegacySaveMigration.Holder) fake;
                holderView.overgrownLegacyBridge$setLegacyComponents(LegacySaveMigration.capture(save));
                try {
                    LegacySaveMigration.restoreOrigins(fake);
                    LegacySaveMigration.restorePowerData(fake);
                    eq(PowerResources.read(fakeHolder, cooldown).orElseThrow(), 60);
                    var timers = fakeHolder.getAuxInts(dot);
                    check(timers != null && timers[0] == 5 && timers[1] == 3, "Timers " + java.util.Arrays.toString(timers));
                } finally { holderView.overgrownLegacyBridge$setLegacyComponents(null); fakeHolder.clear(); }
            });
            suite.test("origin upgrades: legacy upgrades fire on advancement completion", () -> {
                var state = dev.overgrown.origins.component.PlayerOriginsAttachment.getOrCreate(fake);
                var layer = new ResourceLocation("origins", "origin");
                var advancement = new ResourceLocation("minecraft", "story/root");
                var upgrade = json("{\"condition\":\"minecraft:story/root\",\"origin\":\"origins:avian\",\"announcement\":\"bridge.upgrade\"}");
                state.setOrigin(layer, new ResourceLocation("origins", "human"));
                LegacyOriginUpgrades.load(Map.of(new ResourceLocation("origins", "human"), List.of(upgrade)));
                try {
                    LegacyOriginUpgrades.onCompleted(fake, new ResourceLocation("minecraft", "story/mine_stone"));
                    check(new ResourceLocation("origins", "human").equals(state.getOrigin(layer)), "Another advancement triggered the upgrade");
                    LegacyOriginUpgrades.onCompleted(fake, advancement);
                    check(new ResourceLocation("origins", "avian").equals(state.getOrigin(layer)), "Upgrade did not apply: " + state.snapshot());
                } finally {
                    LegacyOriginUpgrades.load(Map.of());
                    state.clearOrigin(layer);
                    fakeHolder.clear();
                }
            });
            suite.test("resource conditions: sub-powers and apoli namespace conditions", () -> {
                var id = new ResourceLocation("bridge_test", "conditional");
                var multiple = (json("{\"type\":\"origins:multiple\",\"kept\":{\"type\":\"origins:simple\",\"fabric:load_conditions\":[{\"condition\":\"apoli:any_namespace_loaded\",\"namespaces\":[\"minecraft\"]}]},\"dropped\":{\"type\":\"origins:simple\",\"fabric:load_conditions\":[{\"condition\":\"apoli:all_namespaces_loaded\",\"namespaces\":[\"minecraft\",\"bridge_absent_namespace\"]}]}}"));
                LegacyPowerNormalizer.filterSubPowers(id, multiple);
                check(multiple.has("kept") && !multiple.has("dropped"), "Sub-power conditions not applied: " + multiple);
            });
            suite.test("origin upgrades: condition objects stay native Overgrown upgrades", () -> {
                var origin = json("{\"upgrades\":[{\"condition\":{\"type\":\"origins:advancement\",\"advancement\":\"minecraft:story/root\"},\"origin\":\"origins:human\",\"announcement\":\"bridge.upgrade\"}]}");
                check(LegacyOriginNormalizer.extractAdvancementUpgrades(origin).isEmpty(), "Condition object treated as an advancement id");
                LegacyOriginNormalizer.origin(origin);
                decode(dev.overgrown.origins.origin.OriginUpgrade.CODEC, origin.getAsJsonArray("upgrades").get(0));
            });
            suite.test("temporary cobweb: behaves as a cobweb", () -> check(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new ResourceLocation("origins", "temporary_cobweb")) instanceof net.minecraft.world.level.block.WebBlock, "Temporary cobweb does not slow entities"));
            suite.test("origin text: translation keys are preserved as components", () -> {
                var origin=json("{\"name\":\"origin.example.name\",\"description\":\"origin.example.description\"}"); LegacyOriginNormalizer.origin(origin);
                check(origin.getAsJsonObject("name").get("translate").getAsString().equals("origin.example.name"),"Origin name became literal");
            });
            suite.test("commands: legacy grant/revoke sources and list, sources, clear", () -> {
                var dispatcher = server.getCommands().getDispatcher();
                var stack = server.createCommandSourceStack().withSuppressedOutput();
                String target = "@e[tag=legacy_bridge_test,limit=1]";
                dispatcher.execute("power grant " + target + " bridge_test:resource", stack);
                check(holder.sourcesOf(resource).contains(LegacyPowerCommands.COMMAND_SOURCE), "Grant did not use apoli:command");
                eq(dispatcher.execute("power sources " + target + " bridge_test:resource", stack), 2);
                dispatcher.execute("power revoke " + target + " bridge_test:resource", stack);
                check(holder.hasPower(resource) && !holder.sourcesOf(resource).contains(LegacyPowerCommands.COMMAND_SOURCE), "Revoke removed more than the command grant");
                eq(dispatcher.execute("power revoke " + target + " bridge_test:resource", stack), 0);
                check(holder.hasPower(resource), "Revoke without a command grant removed the power");
                check(dispatcher.execute("power list " + target, stack) > 0, "List found no powers");
                check(dispatcher.execute("power clear " + target, stack) > 0 && holder.isEmpty(), "Clear left powers behind");
            });
        } finally { holder.clear();pig.discard();ApoliPowers.replaceAll(original); }
        return suite.results;
    }
}
