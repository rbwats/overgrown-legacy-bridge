package org.overgrowns.migration;

import com.google.gson.JsonObject;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Origins 1.10.0 upgrades: applied when the named advancement is completed, never retroactively. */
public final class LegacyOriginUpgrades {
    private LegacyOriginUpgrades() {}

    public record Upgrade(ResourceLocation advancement, ResourceLocation target, String announcement) {}

    private static volatile Map<ResourceLocation, List<Upgrade>> byOrigin = Map.of();

    public static void load(Map<ResourceLocation, List<JsonObject>> raw) {
        Map<ResourceLocation, List<Upgrade>> parsed = new HashMap<>();
        raw.forEach((origin, upgrades) -> {
            for (JsonObject json : upgrades) {
                ResourceLocation advancement = ResourceLocation.tryParse(LegacySchema.string(json, "condition"));
                ResourceLocation target = ResourceLocation.tryParse(LegacySchema.string(json, "origin"));
                String announcement = LegacySchema.string(json, "announcement");
                if (advancement == null || target == null) {
                    LegacyBridge.LOGGER.error("Origin {} has an upgrade with an invalid advancement or origin id; skipping it", origin);
                    continue;
                }
                parsed.computeIfAbsent(origin, key -> new ArrayList<>()).add(new Upgrade(advancement, target, announcement == null ? "" : announcement));
            }
        });
        byOrigin = parsed;
    }

    public static List<Upgrade> upgradesOf(ResourceLocation origin) {
        return byOrigin.getOrDefault(origin, List.of());
    }

    /** The first upgrade of an origin naming this advancement, as Origin.getUpgrade did. */
    static Upgrade matching(ResourceLocation origin, ResourceLocation advancement) {
        for (Upgrade upgrade : upgradesOf(origin))
            if (upgrade.advancement().equals(advancement)) return upgrade;
        return null;
    }

    public static void onCompleted(ServerPlayer player, ResourceLocation advancement) {
        if (byOrigin.isEmpty()) return;
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null || state.isSelectingOrigin()) return;
        // Snapshots: an upgrade applied here is not upgraded again by the same advancement.
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : state.snapshot().entrySet()) {
            Upgrade upgrade = resolve(entry.getValue(), advancement);
            if (upgrade != null && OriginManager.upgradeOrigin(player, entry.getKey(), upgrade.target())) announce(player, upgrade);
        }
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : state.poolSnapshot().entrySet()) {
            for (ResourceLocation origin : List.copyOf(entry.getValue())) {
                Upgrade upgrade = resolve(origin, advancement);
                if (upgrade != null && OriginManager.upgradePooledOrigin(player, entry.getKey(), origin, upgrade.target())) announce(player, upgrade);
            }
        }
    }

    private static Upgrade resolve(ResourceLocation origin, ResourceLocation advancement) {
        Upgrade upgrade = matching(origin, advancement);
        if (upgrade == null) return null;
        if (OriginRegistry.get(upgrade.target()) == null) {
            LegacyBridge.LOGGER.error("Could not perform Origins upgrade from {} to {}, as the upgrade origin did not exist!", origin, upgrade.target());
            return null;
        }
        return upgrade;
    }

    private static void announce(ServerPlayer player, Upgrade upgrade) {
        if (!upgrade.announcement().isEmpty())
            player.sendSystemMessage(Component.translatable(upgrade.announcement()).withStyle(ChatFormatting.GOLD));
    }
}
