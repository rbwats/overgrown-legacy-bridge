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
        JsonObject obj = normalized(data, LegacySchema.Context.POWER);
        return decode(Power.CODEC, ApoliReloadListener.prepare(new Dynamic<>(JsonOps.INSTANCE, obj), new ResourceLocation("bridge_test", "test")).getValue());
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
        powers.put(resource, power("{\"type\":\"origins:resource\",\"min\":-100,\"max\":100,\"start_value\":10}"));
        powers.put(insomnia, power("{\"type\":\"origins:modify_insomnia_ticks\",\"modifier\":{\"operation\":\"addition\",\"value\":10}}"));
        powers.put(insomnia2, power("{\"type\":\"apoli:modify_insomnia_ticks\",\"modifiers\":[{\"operation\":\"multiply_total\",\"value\":1}]}"));
        powers.put(insomniaOff, power("{\"type\":\"origins:modify_insomnia_ticks\",\"condition\":{\"type\":\"origins:constant\",\"value\":false},\"modifier\":{\"operation\":\"addition\",\"value\":1000}}"));
        powers.put(lava, power("{\"type\":\"origins:modify_lava_speed\",\"modifier\":{\"operation\":\"addition\",\"value\":0.2}}"));
        powers.put(night, power("{\"type\":\"origins:toggle_night_vision\",\"strength\":0.6,\"key\":{\"key\":\"origins.primary_active\",\"continous\":false}}"));
        powers.put(dot, power("{\"type\":\"origins:damage_over_time\",\"damage\":1,\"interval\":2,\"onset_delay\":0}"));
        powers.put(camera, power("{\"type\":\"origins:modify_camera_submersion\",\"to\":\"water\"}"));
        ApoliPowers.replaceAll(powers);
        try {
            for (var id : List.of(resource, insomnia, insomnia2, insomniaOff, lava, night, dot, camera)) check(holder.addPower(id, source), "Cannot grant " + id);
            suite.test("insomnia: aggregate active modifiers without modifying state", () -> {
                eq(LegacyInsomniaPower.modify(pig, 100), 220); eq(holder.getAuxIntOr(resource, -1), 10);
                holder.suppressPower(insomnia2, source); eq(LegacyInsomniaPower.modify(pig, 100), 110); holder.unsuppressPower(insomnia2, source);
            });
            suite.test("lava: modifier arithmetic and native travel injection", () -> {
                eq(LegacyLavaSpeedPower.modify(pig, 0.5), 0.7);
                pig.travel(net.minecraft.world.phys.Vec3.ZERO);
                holder.suppressPower(lava, source); eq(LegacyLavaSpeedPower.modify(pig, 0.5), 0.5); holder.unsuppressPower(lava, source);
            });
            suite.test("toggle night vision: defaults, key dispatch, active condition, suppression", () -> {
                eq(NightVisionPower.strengthFor(pig), 0);
                check(KeyDispatch.press(pig, "key.apoli.primary_active") == 1, "Legacy control did not dispatch");
                eq(NightVisionPower.strengthFor(pig), 0.6);
                var condition = decode(EntityCondition.CODEC, json("{\"type\":\"origins:power_active\",\"power\":\"bridge_test:night\"}"));
                check(condition.test(new EntityCtx(pig, level)), "Toggled power is not active");
                holder.suppressPower(night, source); eq(NightVisionPower.strengthFor(pig), 0); holder.unsuppressPower(night, source);
                KeyDispatch.press(pig, "key.apoli.primary_active");
                eq(NightVisionPower.strengthFor(pig), 0); check(!condition.test(new EntityCtx(pig, level)), "Disabled night vision is active");
            });
            suite.test("toggle: persisted state survives container round trip", () -> {
                KeyDispatch.press(pig, "key.apoli.primary_active");
                var encoded = PowerContainerImpl.CODEC.encodeStart(JsonOps.INSTANCE, holder).getOrThrow(false, message -> {});
                var restored = decode(PowerContainerImpl.CODEC, encoded);
                eq(restored.getAuxIntOr(night, -1), 1); KeyDispatch.press(pig, "key.apoli.primary_active");
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
            suite.test("origin text: translation keys are preserved as components", () -> {
                var origin=json("{\"name\":\"origin.example.name\",\"description\":\"origin.example.description\"}"); LegacyOriginNormalizer.origin(origin);
                check(origin.getAsJsonObject("name").get("translate").getAsString().equals("origin.example.name"),"Origin name became literal");
            });
        } finally { holder.clear();pig.discard();ApoliPowers.replaceAll(original); }
        return suite.results;
    }
}
