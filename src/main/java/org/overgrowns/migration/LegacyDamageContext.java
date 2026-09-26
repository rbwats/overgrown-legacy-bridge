package org.overgrowns.migration;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/** A stack is needed because a damage action can cause another damage event. */
public final class LegacyDamageContext {
    private static final ThreadLocal<Deque<Hit>> CURRENT = new ThreadLocal<>();

    public record Hit(LivingEntity target, float incomingAmount) {}

    private LegacyDamageContext() {}

    public static void push(LivingEntity target, float amount) {
        Deque<Hit> stack = CURRENT.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            CURRENT.set(stack);
        }
        stack.push(new Hit(target, amount));
    }

    public static void pop() {
        Deque<Hit> stack = CURRENT.get();
        if (stack == null) return;
        if (!stack.isEmpty()) stack.pop();
        if (stack.isEmpty()) CURRENT.remove();
    }

    public static Hit current() {
        Deque<Hit> stack = CURRENT.get();
        return stack == null ? null : stack.peek();
    }
}
