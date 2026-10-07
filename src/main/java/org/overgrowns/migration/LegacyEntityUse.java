package org.overgrowns.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerTypes;
import dev.overgrown.apoli.power.builtin.ActionOnUsePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Legacy action_on_entity_use / action_on_being_used ran in priority buckets around vanilla's Player.interact:
 * positive buckets (highest first) before vanilla and able to cancel it, priority 0 before vanilla without
 * cancelling it (its result applied afterwards), and negative buckets only when vanilla passed.
 * Overgrown fires every action_on_use power before vanilla and cancels on the first result.
 */
public final class LegacyEntityUse {
    private LegacyEntityUse() {}

    private static final ResourceLocation ACTION_ON_USE = new ResourceLocation("apoli", "action_on_use");
    private static final Map<ResourceLocation, Integer> PRIORITY = new ConcurrentHashMap<>();
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private record Pending(long time, int target, InteractionHand hand, InteractionResult zero) {}
    private record Entry(ResourceLocation id, ActionOnUsePower.Config cfg, int priority) {}

    public static void clear() {
        PRIORITY.clear();
        PENDING.clear();
    }

    public static void record(ResourceLocation id, JsonElement json) {
        if (json == null || !json.isJsonObject()) return;
        JsonObject power = json.getAsJsonObject();
        String type = LegacySchema.string(power, "type");
        if (type == null) return;
        switch (type) {
            case "origins:action_on_entity_use", "apoli:action_on_entity_use",
                 "origins:action_on_being_used", "apoli:action_on_being_used" -> PRIORITY.put(id, priority(power));
            default -> { }
        }
    }

    static int priority(JsonObject power) {
        JsonElement value = power.get("priority");
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : 0;
    }

    public static boolean hasLegacyPowers() {
        return !PRIORITY.isEmpty();
    }

    /** Replaces Overgrown's pre-vanilla dispatch; Overgrown-native action_on_use powers keep their behavior and run first. */
    public static InteractionResult beforeVanilla(Player actor, Entity target, InteractionHand hand) {
        List<Entry> legacy = new ArrayList<>();
        List<Entry> actorNative = collect(actor, false, legacy), targetNative = collect(target, true, legacy);
        for (List<Entry> side : List.of(actorNative, targetNative))
            for (Entry entry : side) {
                InteractionResult result = PowerTypes.ACTION_ON_USE.tryFire(entry.cfg(), actor, target, hand);
                if (result != InteractionResult.PASS) return result;
            }
        InteractionResult zero = InteractionResult.PASS;
        legacy.sort(Comparator.comparingInt(Entry::priority).reversed());
        for (Bucket bucket : buckets(legacy, true)) {
            InteractionResult result = bucket.fire(actor, target, hand);
            if (bucket.priority() == 0) zero = result;
            else if (result != InteractionResult.PASS) {
                if (result.shouldSwing()) actor.swing(hand);
                return result;
            }
        }
        PENDING.put(actor.getUUID(), new Pending(actor.level().getGameTime(), target.getId(), hand, zero));
        return InteractionResult.PASS;
    }

    /** Applied at the end of vanilla's Player.interact on the server. */
    public static InteractionResult afterVanilla(Player actor, Entity target, InteractionHand hand, InteractionResult original) {
        Pending pending = PENDING.remove(actor.getUUID());
        if (pending == null || pending.time() != actor.level().getGameTime() || pending.target() != target.getId() || pending.hand() != hand)
            return original;
        InteractionResult custom = pending.zero();
        if (custom == InteractionResult.PASS && original == InteractionResult.PASS) {
            List<Entry> legacy = new ArrayList<>();
            collect(actor, false, legacy);
            collect(target, true, legacy);
            legacy.sort(Comparator.comparingInt(Entry::priority).reversed());
            for (Bucket bucket : buckets(legacy, false)) {
                InteractionResult result = bucket.fire(actor, target, hand);
                if (result != InteractionResult.PASS) { custom = result; break; }
            }
        }
        if (custom.shouldSwing()) actor.swing(hand);
        if (original.consumesAction() && !custom.consumesAction()) return original;
        if (original.shouldSwing() && !custom.shouldSwing()) return original;
        return custom;
    }

    /** Splits a holder's active action_on_use powers into Overgrown-native ones (returned) and legacy ones. */
    private static List<Entry> collect(Entity holder, boolean targetSide, List<Entry> legacy) {
        List<Entry> natives = new ArrayList<>();
        PowerLookup.forEachEntry(holder, ACTION_ON_USE, ActionOnUsePower.Config.class, (id, cfg) -> {
            if (cfg.targetUsed() != targetSide) return;
            Integer priority = PRIORITY.get(id);
            if (priority == null) natives.add(new Entry(id, cfg, 0));
            else legacy.add(new Entry(id, cfg, priority));
        });
        return natives;
    }

    private record Bucket(int priority, List<Entry> entries) {
        /** Every power in a bucket fires; the first accepted (else first swinging) result wins. */
        InteractionResult fire(Player actor, Entity target, InteractionHand hand) {
            InteractionResult result = InteractionResult.PASS;
            for (Entry entry : entries) {
                InteractionResult current = PowerTypes.ACTION_ON_USE.tryFire(entry.cfg(), actor, target, hand);
                if (current.consumesAction() && !result.consumesAction()) result = current;
                else if (current.shouldSwing() && !result.shouldSwing()) result = current;
            }
            return result;
        }
    }

    private static List<Bucket> buckets(List<Entry> sorted, boolean beforeVanilla) {
        List<Bucket> buckets = new ArrayList<>();
        for (Entry entry : sorted) {
            if (beforeVanilla != entry.priority() >= 0) continue;
            if (buckets.isEmpty() || buckets.get(buckets.size() - 1).priority() != entry.priority())
                buckets.add(new Bucket(entry.priority(), new ArrayList<>()));
            buckets.get(buckets.size() - 1).entries().add(entry);
        }
        return buckets;
    }
}
