package org.overgrowns.migration;
import com.google.gson.*;
import com.mojang.serialization.*;
import dev.overgrown.apoli.action.*;
import dev.overgrown.apoli.condition.*;
import dev.overgrown.apoli.loader.ApoliReloadListener;
import dev.overgrown.apoli.power.Power;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.util.*;
/** Opt-in development verification; has no effect without the explicit JVM property. */
public final class LegacyCompatibilityTests {
    private LegacyCompatibilityTests() {}
    public static Codec<?> codec(LegacySchema.Context context) {
        return switch(context) {
            case POWER -> Power.CODEC;
            case ENTITY_ACTION -> EntityAction.CODEC;
            case BI_ENTITY_ACTION -> BiEntityAction.CODEC;
            case BLOCK_ACTION -> BlockAction.CODEC;
            case ITEM_ACTION -> ItemAction.CODEC;
            case ENTITY_CONDITION -> EntityCondition.CODEC;
            case BI_ENTITY_CONDITION -> BiEntityCondition.CODEC;
            case BLOCK_CONDITION -> BlockCondition.CODEC;
            case ITEM_CONDITION -> ItemCondition.CODEC;
            case DAMAGE_CONDITION -> DamageCondition.CODEC;
            case FLUID_CONDITION -> FluidCondition.CODEC;
            case BIOME_CONDITION -> BiomeCondition.CODEC;
        };
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static DataResult<?> roundTrip(Codec codec, Object value) {
        return codec.encodeStart(JsonOps.INSTANCE, value).flatMap(encoded -> codec.parse(JsonOps.INSTANCE, encoded));
    }
    public static void runIfRequested(MinecraftServer server) {
        String corpus = System.getProperty("overgrown_legacy_bridge.testCorpus");
        if (corpus == null) return;
        try {
            JsonArray cases = JsonParser.parseString(Files.readString(Path.of(corpus))).getAsJsonArray();
            JsonArray results = new JsonArray(); int passed = 0;
            for (JsonElement element : cases) {
                JsonObject test = element.getAsJsonObject();
                LegacySchema.Context context = LegacySchema.Context.valueOf(test.get("context").getAsString());
                JsonElement input = test.get("data").deepCopy();
                LegacySchema.normalize(input, context);
                if (context == LegacySchema.Context.POWER) {
                    if (input.getAsJsonObject().get("type").getAsString().endsWith(":multiple")) input.getAsJsonObject().add("sub_powers", new JsonArray());
                    input = ApoliReloadListener.prepare(new Dynamic<>(JsonOps.INSTANCE, input), new ResourceLocation("bridge_test", "codec")).getValue();
                }
                DataResult<?> parsed = codec(context).parse(JsonOps.INSTANCE, input);
                JsonObject result = new JsonObject(); result.add("name", test.get("name"));
                boolean ok = parsed.error().isEmpty();
                if (ok) {
                    JsonElement once = input.deepCopy(); LegacySchema.normalize(once, context);
                    if (!once.equals(input)) { parsed = DataResult.error(() -> "Normalization is not idempotent"); ok = false; }
                    else { DataResult<?> roundTrip = roundTrip(codec(context), parsed.result().orElseThrow());
                        if (roundTrip.error().isPresent()) { parsed = roundTrip; ok = false; }
                    }
                }
                result.addProperty("passed", ok);
                if (ok) passed++; else {
                    String error = parsed.error().get().message(); result.addProperty("error", error); result.add("normalized", input);
                    LegacyBridge.LOGGER.error("COMPATIBILITY CASE {}: {}", test.get("name").getAsString(), error);
                }
                results.add(result);
            }
            Path output = Path.of(corpus).resolveSibling("codec-results.json");
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(results));
            LegacyBridge.LOGGER.info("COMPATIBILITY CORPUS: {}/{} passed; results at {}", passed, cases.size(), output);
            JsonArray regressions = LegacyRegressionTests.run(server);
            Files.writeString(Path.of(corpus).resolveSibling("regression-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(regressions));
            long regressionPassed = java.util.stream.StreamSupport.stream(regressions.spliterator(), false).filter(row -> row.getAsJsonObject().get("passed").getAsBoolean()).count();
            LegacyBridge.LOGGER.info("BEHAVIORAL REGRESSIONS: {}/{} passed", regressionPassed, regressions.size());
            server.halt(false);
        } catch (Exception e) { LegacyBridge.LOGGER.error("Compatibility test failed", e); server.halt(false); }
    }
}
