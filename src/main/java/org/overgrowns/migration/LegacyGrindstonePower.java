package org.overgrowns.migration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.builtin.ModifyGrindstonePower.ResultType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Legacy modify_grindstone: Overgrown parses the power but never applies it, and drops block_condition. */
public final class LegacyGrindstonePower extends PowerType<LegacyGrindstonePower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "modify_grindstone");

    public record Config(
        ResultType resultType,
        Optional<BlockCondition> blockCondition,
        Optional<BlockAction> blockAction,
        Optional<ItemCondition> topCondition,
        Optional<ItemCondition> bottomCondition,
        Optional<ItemCondition> outputCondition,
        Optional<ItemAction> itemAction,
        Optional<ItemAction> itemActionAfterGrinding,
        Optional<ItemStackData> resultStack,
        Optional<EntityAction> entityAction,
        Optional<AttributeModifier> xpModifier
    ) {}

    /** Calio accepted enum names in either case, or ordinals. */
    private static final Codec<ResultType> RESULT_TYPE = Codec.either(Codec.INT, Codec.STRING).comapFlatMap(value -> value.map(
        ordinal -> ordinal >= 0 && ordinal < ResultType.values().length
            ? DataResult.success(ResultType.values()[ordinal]) : DataResult.<ResultType>error(() -> "Unknown result_type " + ordinal),
        name -> {
            for (ResultType type : ResultType.values())
                if (type.name().equalsIgnoreCase(name)) return DataResult.success(type);
            return DataResult.<ResultType>error(() -> "Unknown result_type " + name);
        }), type -> Either.right(type.getSerializedName()));

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            RESULT_TYPE.optionalFieldOf("result_type", ResultType.UNCHANGED).forGetter(Config::resultType),
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            BlockAction.CODEC.optionalFieldOf("block_action").forGetter(Config::blockAction),
            ItemCondition.CODEC.optionalFieldOf("top_condition").forGetter(Config::topCondition),
            ItemCondition.CODEC.optionalFieldOf("bottom_condition").forGetter(Config::bottomCondition),
            ItemCondition.CODEC.optionalFieldOf("output_condition").forGetter(Config::outputCondition),
            ItemAction.CODEC.optionalFieldOf("item_action").forGetter(Config::itemAction),
            ItemAction.CODEC.optionalFieldOf("item_action_after_grinding").forGetter(Config::itemActionAfterGrinding),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            AttributeModifier.CODEC.optionalFieldOf("xp_modifier").forGetter(Config::xpModifier)
        ).apply(i, Config::new));
    }

    /** Any matching power lets the slot accept items the grindstone would normally refuse. */
    public static boolean allowsInSlot(Player player, ItemStack stack, boolean top) {
        if (player == null) return false;
        return PowerLookup.anyActive(player, ID, Config.class, cfg ->
            test(top ? cfg.topCondition() : cfg.bottomCondition(), player, stack));
    }

    /** Matching powers rewrite the output in order; the list is kept for the take actions and XP. */
    public static List<Config> modifyResult(Player player, Optional<BlockPos> pos, ItemStack top, ItemStack bottom,
                                            ItemStack output, Function<ItemStack, ItemStack> setResult) {
        List<Config> applied = new ArrayList<>();
        if (player == null) return applied;
        PowerLookup.forEach(player, ID, Config.class, cfg -> {
            if (test(cfg.topCondition(), player, top) && test(cfg.bottomCondition(), player, bottom)
                && test(cfg.outputCondition(), player, output) && blockMatches(cfg, player, pos)) applied.add(cfg);
        });
        if (applied.isEmpty()) return applied;
        ItemStack result = output;
        for (Config cfg : applied) {
            ItemStack next = switch (cfg.resultType()) {
                case SPECIFIED -> cfg.resultStack().map(data -> data.stack().copy()).orElse(ItemStack.EMPTY);
                case FROM_TOP -> top.copy();
                case FROM_BOTTOM -> bottom.copy();
                case UNCHANGED -> result.copy();
            };
            result = run(cfg.itemAction(), player, next);
        }
        setResult.apply(result);
        return applied;
    }

    public static void onTake(Player player, Optional<BlockPos> pos, List<Config> applied, ItemStack taken) {
        if (player == null) return;
        for (Config cfg : applied) {
            run(cfg.itemActionAfterGrinding(), player, taken);
            cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(player, player.level())));
            if (pos.isPresent()) cfg.blockAction().ifPresent(action ->
                action.run(new BlockCtx(pos.get(), player.level().getBlockState(pos.get()), player.level(), player)));
        }
    }

    public static int modifyExperience(Player player, List<Config> applied, int experience) {
        List<AttributeModifier> modifiers = new ArrayList<>();
        for (Config cfg : applied) cfg.xpModifier().ifPresent(modifiers::add);
        if (player == null || modifiers.isEmpty()) return experience;
        double result = AttributeModifierHelper.apply((double) experience, modifiers, player);
        return Double.isFinite(result) ? (int) result : experience;
    }

    private static boolean blockMatches(Config cfg, Player player, Optional<BlockPos> pos) {
        // Legacy skipped the block check when the menu had no world position.
        if (cfg.blockCondition().isEmpty() || pos.isEmpty()) return true;
        BlockPos at = pos.get();
        return cfg.blockCondition().get().test(new BlockCtx(at, player.level().getBlockState(at), player.level(), player));
    }

    private static boolean test(Optional<ItemCondition> condition, Player player, ItemStack stack) {
        return condition.isEmpty() || condition.get().test(new ItemCtx(stack, player.level(), player));
    }

    /** Runs an item action on the stack, honoring actions that replace it. */
    private static ItemStack run(Optional<ItemAction> action, Player player, ItemStack stack) {
        if (action.isEmpty()) return stack;
        ItemStack[] current = {stack};
        action.get().run(new ItemCtx(stack, player.level(), player, replacement -> current[0] = replacement));
        return current[0];
    }
}
