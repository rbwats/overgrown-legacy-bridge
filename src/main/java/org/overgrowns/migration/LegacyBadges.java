package org.overgrowns.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Set;

/** Inline power badges are read from raw power files, so they need their own legacy pass. */
public final class LegacyBadges {
    private LegacyBadges() {}

    private static final String LEGACY_PREFIX = "origins:textures/gui/badge/";
    private static final Set<String> MOVED = Set.of("active.png", "arrow_up.png", "info.png", "recipe.png", "star.png", "toggle.png");

    /** Moves the six Origins 1.10.0 badge sprites to their Overgrown locations; custom sprites are untouched. */
    public static boolean normalize(JsonElement element) {
        if (element == null || !element.isJsonObject()) return false;
        JsonObject badge = element.getAsJsonObject();
        String sprite = LegacySchema.string(badge, "sprite");
        if (sprite == null || !sprite.startsWith(LEGACY_PREFIX)) return false;
        String file = sprite.substring(LEGACY_PREFIX.length());
        if (!MOVED.contains(file)) return false;
        badge.addProperty("sprite", LEGACY_PREFIX + "isaacfanta/" + file);
        return true;
    }
}
