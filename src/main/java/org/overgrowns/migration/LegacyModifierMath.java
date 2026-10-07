package org.overgrowns.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.data.AttributeModifierOperation;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import org.overgrowns.migration.LegacyModifierEngine.Op;
import org.overgrowns.migration.LegacyModifierEngine.Term;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Overgrown modifiers marked as legacy by the normalizer, evaluated with Apoli 2.9.0's engine
 * ({@link LegacyModifierEngine}): resource values with legacy fallback, nested lists, and the damage passes.
 */
public final class LegacyModifierMath {
    private LegacyModifierMath() {}

    static @Nullable Op of(AttributeModifierOperation operation) {
        return switch (operation) {
            case ADD_BASE_EARLY -> Op.ADD_BASE_EARLY;
            case MULTIPLY_BASE_ADDITIVE -> Op.MULTIPLY_BASE_ADDITIVE;
            case MULTIPLY_BASE_MULTIPLICATIVE -> Op.MULTIPLY_BASE_MULTIPLICATIVE;
            case ADD_BASE_LATE -> Op.ADD_BASE_LATE;
            case MIN_BASE -> Op.MIN_BASE;
            case MAX_BASE -> Op.MAX_BASE;
            case SET_BASE -> Op.SET_BASE;
            case MULTIPLY_TOTAL_ADDITIVE -> Op.MULTIPLY_TOTAL_ADDITIVE;
            case MULTIPLY_TOTAL_MULTIPLICATIVE -> Op.MULTIPLY_TOTAL_MULTIPLICATIVE;
            case ADD_TOTAL_LATE -> Op.ADD_TOTAL_LATE;
            case MIN_TOTAL -> Op.MIN_TOTAL;
            case MAX_TOTAL -> Op.MAX_TOTAL;
            case SET_TOTAL -> Op.SET_TOTAL;
            // Overgrown-only operations never come from legacy data.
            default -> null;
        };
    }

    public static boolean isLegacy(AttributeModifier modifier) {
        return modifier.name().map(name -> name.startsWith(LegacySchema.LEGACY_MODIFIER)).orElse(false) && of(modifier.operation()) != null;
    }

    public static boolean allLegacy(List<AttributeModifier> modifiers) {
        if (modifiers.isEmpty()) return false;
        for (AttributeModifier modifier : modifiers) if (!isLegacy(modifier)) return false;
        return true;
    }

    public static double apply(double baseValue, List<Term> terms) {
        return LegacyModifierEngine.apply(baseValue, terms);
    }

    public static double apply(double baseValue, List<AttributeModifier> modifiers, @Nullable Entity entity, @Nullable PowerContainer container) {
        return apply(baseValue, terms(modifiers, entity, container, baseValue));
    }

    public static List<Term> terms(List<AttributeModifier> modifiers, @Nullable Entity entity, @Nullable PowerContainer container, double context) {
        List<Term> terms = new ArrayList<>(modifiers.size());
        for (AttributeModifier modifier : modifiers) terms.add(new Term(of(modifier.operation()), input(modifier, entity, container, context)));
        return terms;
    }

    /**
     * A modifier's value as legacy read it: a resource's current value when the holder has that power (otherwise
     * the plain value), then any nested modifiers applied to it as their own group.
     */
    public static double input(AttributeModifier modifier, @Nullable Entity entity, @Nullable PowerContainer container, double context) {
        PowerContainer holder = container != null || entity == null ? container : PowerContainer.of(entity);
        double value;
        if (modifier.resource().isPresent() && holder != null && holder.hasPower(modifier.resource().get())) {
            var resource = PowerResources.read(holder, modifier.resource().get());
            value = resource.isPresent() ? resource.getAsInt() : modifier.value().evalWith(entity, holder, context);
        } else {
            value = modifier.value().evalWith(entity, holder, context);
        }
        List<AttributeModifier> nested = nested(modifier);
        return nested.isEmpty() ? value : apply(value, nested, entity, holder);
    }

    private static final Map<String, List<AttributeModifier>> NESTED = new ConcurrentHashMap<>();

    /** The nested list carried in the marker name, or Overgrown's single nested modifier. */
    static List<AttributeModifier> nested(AttributeModifier modifier) {
        String name = modifier.name().orElse("");
        if (name.length() > LegacySchema.LEGACY_MODIFIER.length() && name.startsWith(LegacySchema.LEGACY_MODIFIER))
            return NESTED.computeIfAbsent(name, LegacyModifierMath::decodeNested);
        return modifier.nested().map(List::of).orElse(List.of());
    }

    private static List<AttributeModifier> decodeNested(String name) {
        try {
            JsonObject payload = JsonParser.parseString(name.substring(LegacySchema.LEGACY_MODIFIER.length())).getAsJsonObject();
            if (!payload.has("nested") || !payload.get("nested").isJsonArray()) return List.of();
            List<AttributeModifier> out = new ArrayList<>();
            for (JsonElement element : payload.getAsJsonArray("nested"))
                AttributeModifier.CODEC.parse(JsonOps.INSTANCE, element).result().ifPresent(out::add);
            return List.copyOf(out);
        } catch (RuntimeException error) {
            LegacyBridge.LOGGER.warn("Unreadable legacy nested modifiers in {}: {}", name, error.toString());
            return List.of();
        }
    }

    /** Overgrown's per-holder pairs, as ModifyDamageHandler builds them. */
    public static List<Term> ownedTerms(List<AttributeModifierHelper.Owned> owned, double context) {
        List<Term> terms = new ArrayList<>(owned.size());
        for (var entry : owned) terms.add(new Term(of(entry.modifier().operation()), input(entry.modifier(), entry.entity(), entry.container(), context)));
        return terms;
    }

    public static boolean allLegacyOwned(List<AttributeModifierHelper.Owned> owned) {
        if (owned.isEmpty()) return false;
        for (var entry : owned) if (!isLegacy(entry.modifier())) return false;
        return true;
    }

    // Damage: Overgrown pools dealt and taken modifiers into one pass; legacy ran dealt (never for projectile
    // damage), then taken, each with its holder's attribute_modify_transfer modifiers.
    private record DamageScope(@Nullable LivingEntity attacker, LivingEntity target, DamageSource source, boolean[] merged) {}
    private static final ThreadLocal<java.util.ArrayDeque<DamageScope>> DAMAGE = ThreadLocal.withInitial(java.util.ArrayDeque::new);

    public static void beginDamage(@Nullable LivingEntity attacker, LivingEntity target, DamageSource source) {
        DAMAGE.get().push(new DamageScope(attacker, target, source, new boolean[1]));
    }

    /** Ends the damage scope; true when its transfers were already merged into a legacy pass. */
    public static boolean endDamage() {
        DamageScope scope = DAMAGE.get().poll();
        return scope != null && scope.merged()[0];
    }

    public static double applyOwned(double amount, List<AttributeModifierHelper.Owned> owned) {
        DamageScope scope = DAMAGE.get().peek();
        if (scope == null) return apply(amount, ownedTerms(owned, amount));
        List<AttributeModifierHelper.Owned> dealt = new ArrayList<>(), taken = new ArrayList<>();
        for (var entry : owned) (entry.entity() == scope.target() ? taken : dealt).add(entry);
        double value = amount;
        if (scope.attacker() != null && !scope.source().is(DamageTypeTags.IS_PROJECTILE)) {
            List<Term> terms = ownedTerms(dealt, value);
            terms.addAll(LegacyAttributeTransferPower.terms(scope.attacker(), "modify_damage_dealt"));
            value = apply(value, terms);
        }
        List<Term> terms = ownedTerms(taken, value);
        terms.addAll(LegacyAttributeTransferPower.terms(scope.target(), "modify_damage_taken"));
        scope.merged()[0] = true;
        return apply(value, terms);
    }
}
