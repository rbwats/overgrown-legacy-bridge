package org.overgrowns.migration;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import java.util.function.*;
/** Legacy side wrapper for every action context, including item and block actions. */
public final class LegacySideAction<C, W> implements ActionType<C, LegacySideAction.Config<W>> {
    private final Codec<W> action;
    private final BiConsumer<W, C> runner;
    private final Predicate<C> client;
    public LegacySideAction(Codec<W> action, BiConsumer<W,C> runner, Predicate<C> client) {
        this.action = action; this.runner = runner; this.client = client;
    }
    public record Config<W>(String side, W action) {}
    public MapCodec<Config<W>> codec() {
        Codec<String> side = Codec.STRING.flatXmap(s -> s.equalsIgnoreCase("server") || s.equalsIgnoreCase("client")
            ? DataResult.success(s) : DataResult.error(() -> "Side must be server or client"), DataResult::success);
        return RecordCodecBuilder.mapCodec(i -> i.group(
            side.fieldOf("side").forGetter(Config::side), action.fieldOf("action").forGetter(Config::action)
        ).apply(i, Config::new));
    }
    public void run(Config<W> cfg, C ctx) {
        if (client.test(ctx) == cfg.side().equalsIgnoreCase("client")) runner.accept(cfg.action(), ctx);
    }
}
