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
    private static DataResult<?> parse(LegacySchema.Context context, JsonElement normalized) {
        JsonElement input = normalized.deepCopy();
        if (context == LegacySchema.Context.POWER) {
            if (input.getAsJsonObject().get("type").getAsString().endsWith(":multiple")) input.getAsJsonObject().add("sub_powers", new JsonArray());
            input = ApoliReloadListener.prepare(new Dynamic<>(JsonOps.INSTANCE, input), new ResourceLocation("bridge_test", "codec")).getValue();
        }
        return codec(context).parse(JsonOps.INSTANCE, input);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static JsonElement encode(LegacySchema.Context context, Object value) {
        DataResult<JsonElement> encoded = ((Codec) codec(context)).encodeStart(JsonOps.INSTANCE, value);
        return encoded.result().orElse(null);
    }
    /** Top-level fields whose removal decodes to an identical value. */
    private static JsonArray ignoredFields(LegacySchema.Context context, JsonElement normalized, Object decoded) {
        JsonArray unused = new JsonArray();
        JsonElement reference = encode(context, decoded);
        if (reference == null || !normalized.isJsonObject()) return unused;
        for (String key : normalized.getAsJsonObject().keySet()) {
            if (key.equals("type")) continue;
            JsonObject without = normalized.getAsJsonObject().deepCopy(); without.remove(key);
            DataResult<?> reduced = parse(context, without);
            if (reduced.result().isPresent() && reference.equals(encode(context, reduced.result().get()))) unused.add(key);
        }
        return unused;
    }
    public static void runIfRequested(MinecraftServer server) {
        String corpus = System.getProperty("overgrown_legacy_bridge.testCorpus");
        if (corpus == null) return;
        try {
            JsonArray cases = JsonParser.parseString(Files.readString(Path.of(corpus))).getAsJsonArray();
            JsonArray results = new JsonArray(); int passed = 0;
            int fullDecoded = 0, ignored = 0;
            for (JsonElement element : cases) {
                JsonObject test = element.getAsJsonObject();
                LegacySchema.Context context = LegacySchema.Context.valueOf(test.get("context").getAsString());
                boolean full = test.has("full") && test.get("full").getAsBoolean();
                JsonElement input = test.get("data").deepCopy();
                LegacySchema.normalize(input, context);
                DataResult<?> parsed = parse(context, input);
                JsonObject result = new JsonObject(); result.add("name", test.get("name"));
                boolean ok = parsed.error().isEmpty();
                if (ok) {
                    JsonElement once = input.deepCopy(); LegacySchema.normalize(once, context);
                    if (!once.equals(input)) { parsed = DataResult.error(() -> "Normalization is not idempotent"); ok = false; }
                    else { DataResult<?> roundTrip = roundTrip(codec(context), parsed.result().orElseThrow());
                        if (roundTrip.error().isPresent()) { parsed = roundTrip; ok = false; }
                    }
                }
                if (full) {
                    // Diagnostic only: optional fields that decode but change nothing are ignored by the fork.
                    result.addProperty("full", true);
                    result.addProperty("decoded", ok);
                    if (ok) {
                        fullDecoded++;
                        JsonArray unused = ignoredFields(context, input, parsed.result().orElseThrow());
                        ignored += unused.size();
                        result.add("ignored_fields", unused);
                    } else result.addProperty("error", parsed.error().get().message());
                    result.addProperty("passed", true);
                    results.add(result);
                    continue;
                }
                result.addProperty("passed", ok);
                if (ok) passed++; else {
                    String error = parsed.error().get().message(); result.addProperty("error", error); result.add("normalized", input);
                    LegacyBridge.LOGGER.error("COMPATIBILITY CASE {}: {}", test.get("name").getAsString(), error);
                }
                results.add(result);
            }
            LegacyBridge.LOGGER.info("COMPATIBILITY FULL-FIELD CASES: {} decoded, {} ignored optional field(s)", fullDecoded, ignored);
            Path output = Path.of(corpus).resolveSibling("codec-results.json");
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(results));
            long baseCases = java.util.stream.StreamSupport.stream(cases.spliterator(), false).filter(c -> !c.getAsJsonObject().has("full")).count();
            LegacyBridge.LOGGER.info("COMPATIBILITY CORPUS: {}/{} passed; results at {}", passed, baseCases, output);
            JsonArray regressions = LegacyRegressionTests.run(server);
            Files.writeString(Path.of(corpus).resolveSibling("regression-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(regressions));
            long regressionPassed = java.util.stream.StreamSupport.stream(regressions.spliterator(), false).filter(row -> row.getAsJsonObject().get("passed").getAsBoolean()).count();
            LegacyBridge.LOGGER.info("BEHAVIORAL REGRESSIONS: {}/{} passed", regressionPassed, regressions.size());
            server.halt(false);
        } catch (Exception e) { LegacyBridge.LOGGER.error("Compatibility test failed", e); server.halt(false); }
    }
}
