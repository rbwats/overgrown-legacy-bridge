package org.overgrowns.migration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Apoli 2.9.0's modifier engine (ModifierUtil / ModifierOperation), free of game types. Operations are grouped:
 * each receives all its values at once, base-phase multipliers use the unmodified value, every total-phase
 * operation re-bases on the value before it, and add_total_late sets the value. Overgrown applies modifiers one
 * by one, so results differ when several operations or values combine.
 */
public final class LegacyModifierEngine {
    private LegacyModifierEngine() {}

    /** Legacy operations in their phase and order. */
    public enum Op {
        ADD_BASE_EARLY(0, 0), MULTIPLY_BASE_ADDITIVE(0, 100), MULTIPLY_BASE_MULTIPLICATIVE(0, 200), ADD_BASE_LATE(0, 300),
        MIN_BASE(0, 400), MAX_BASE(0, 500), SET_BASE(0, 600),
        MULTIPLY_TOTAL_ADDITIVE(1, 0), MULTIPLY_TOTAL_MULTIPLICATIVE(1, 100), ADD_TOTAL_LATE(1, 200),
        MIN_TOTAL(1, 300), MAX_TOTAL(1, 400), SET_TOTAL(1, 500);

        final int phase, order;
        Op(int phase, int order) { this.phase = phase; this.order = order; }

        double apply(List<Double> values, double base, double current) {
            double value = current;
            switch (this) {
                case ADD_BASE_EARLY -> { value = base; for (double v : values) value += v; }
                case MULTIPLY_BASE_ADDITIVE, MULTIPLY_TOTAL_ADDITIVE -> { double sum = 0; for (double v : values) sum += v; value = current + base * sum; }
                case MULTIPLY_BASE_MULTIPLICATIVE, MULTIPLY_TOTAL_MULTIPLICATIVE -> { for (double v : values) value *= 1 + v; }
                case ADD_BASE_LATE -> { for (double v : values) value += v; }
                case MIN_BASE, MIN_TOTAL -> { for (double v : values) value = Math.max(v, value); }
                case MAX_BASE, MAX_TOTAL -> { for (double v : values) value = Math.min(v, value); }
                // The original add_total_late assigned instead of adding.
                case SET_BASE, SET_TOTAL, ADD_TOTAL_LATE -> { for (double v : values) value = v; }
            }
            return value;
        }
    }

    public record Term(Op op, double value) {}

    /** Legacy ModifierUtil.applyModifiers: operations sorted by phase and order, each given all of its values. */
    public static double apply(double baseValue, List<Term> terms) {
        if (terms.isEmpty()) return baseValue;
        Map<Op, List<Double>> buckets = new EnumMap<>(Op.class);
        for (Term term : terms) buckets.computeIfAbsent(term.op(), op -> new ArrayList<>()).add(term.value());
        List<Op> operations = new ArrayList<>(buckets.keySet());
        operations.sort((a, b) -> a.phase != b.phase ? a.phase - b.phase : a.order - b.order);
        double currentBase = baseValue, currentValue = baseValue;
        for (Op op : operations) {
            // Legacy compared against a phase variable it never updated, so every total operation re-bases.
            if (op.phase != 0) currentBase = currentValue;
            currentValue = op.apply(buckets.get(op), currentBase, currentValue);
        }
        return currentValue;
    }

}
