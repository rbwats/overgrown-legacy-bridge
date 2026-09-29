package org.overgrowns.migration;
import dev.overgrown.apoli.power.PowerLookup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.util.List;
public final class LegacyBridgeClient implements ClientModInitializer {
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            List<LegacyFluidRenderPower.Config> active = PowerLookup.active(client.player, LegacyFluidRenderPower.ID, LegacyFluidRenderPower.Config.class);
            if (!active.equals(previous)) {
                previous = List.copyOf(active);
                if (client.level != null) client.levelRenderer.allChanged();
            }
        });
    }
    private List<LegacyFluidRenderPower.Config> previous = List.of();
}
