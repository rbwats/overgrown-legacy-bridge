package org.overgrowns.migration;
import com.google.gson.*;
import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.util.*;
/** Diagnostic candidates only: native loaders remain the authority on load conditions and parsing. */
public final class LegacyCompatibilityReport {
    private LegacyCompatibilityReport() {}
    public static Path path() { return FabricLoader.getInstance().getConfigDir().resolve(LegacyBridge.MOD_ID).resolve("compatibility-report.json"); }
    public static volatile int issueCount;
    public static void write(Map<ResourceLocation, JsonElement> data, int changed) {
        JsonArray issues = new JsonArray();
        data.forEach((id, json) -> LegacySchema.walk(json, LegacySchema.Context.POWER, "$", (ctx, node, location) -> {
            String name = LegacySchema.string(node, "type"); ResourceLocation type = ResourceLocation.tryParse(name);
            Object registered = type == null ? null : switch (ctx) {
                case POWER -> PowerTypeRegistry.get(type);
                case ENTITY_ACTION -> ActionTypes.ENTITY.get(type);
                case BI_ENTITY_ACTION -> ActionTypes.BI_ENTITY.get(type);
                case BLOCK_ACTION -> ActionTypes.BLOCK.get(type);
                case ITEM_ACTION -> ActionTypes.ITEM.get(type);
                case ENTITY_CONDITION -> ConditionTypes.ENTITY.get(type);
                case BI_ENTITY_CONDITION -> ConditionTypes.BI_ENTITY.get(type);
                case BLOCK_CONDITION -> ConditionTypes.BLOCK.get(type);
                case ITEM_CONDITION -> ConditionTypes.ITEM.get(type);
                case DAMAGE_CONDITION -> ConditionTypes.DAMAGE.get(type);
                case FLUID_CONDITION -> ConditionTypes.FLUID.get(type);
                case BIOME_CONDITION -> ConditionTypes.BIOME.get(type);
            };
            if (registered == null) issue(issues, id, location, ctx, name,
                "Unknown factory in this context. Install a compatible provider or port it; the bridge does not substitute a dummy.");
        }));
        issueCount = issues.size();
        JsonObject report = new JsonObject(); report.addProperty("scope", "Normalized JSON factory checks; not a gameplay certification. Optional load conditions may intentionally disable reported entries.");
        report.addProperty("power_files", data.size()); report.addProperty("changed_files", changed); report.add("issues", issues);
        try { Files.createDirectories(path().getParent()); Files.writeString(path(), new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
        catch (Exception error) { LegacyBridge.LOGGER.warn("Could not write compatibility report", error); }
        if (!issues.isEmpty()) LegacyBridge.LOGGER.warn("Legacy compatibility found {} diagnostic candidate(s); see {}", issues.size(), path());
    }
    private static void issue(JsonArray into, ResourceLocation id, String path, LegacySchema.Context ctx, String type, String message) {
        JsonObject row = new JsonObject(); row.addProperty("power", id.toString()); row.addProperty("path", path);
        row.addProperty("context", ctx.name()); row.addProperty("type", type); row.addProperty("message", message); into.add(row);
    }
}
