package org.overgrowns.migration;
import dev.overgrown.apoli.power.PowerLookup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import java.util.List;
public final class LegacyBridgeClient implements ClientModInitializer {
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            List<LegacyFluidRenderPower.Config> active = PowerLookup.active(client.player, LegacyFluidRenderPower.ID, LegacyFluidRenderPower.Config.class);
            if (!active.equals(previous)) {
                previous = List.copyOf(active);
                if (client.level != null && client.player != null) rebuildNearbySections(client);
            }
        });
    }
    /** Re-meshes loaded sections instead of reloading the whole renderer each time a fluid render power toggles. */
    private static void rebuildNearbySections(Minecraft client) {
        int radius = client.options.getEffectiveRenderDistance() + 1;
        int centerX = SectionPos.blockToSectionCoord(client.player.getBlockX());
        int centerZ = SectionPos.blockToSectionCoord(client.player.getBlockZ());
        for (int x = centerX - radius; x <= centerX + radius; x++)
            for (int z = centerZ - radius; z <= centerZ + radius; z++)
                for (int y = client.level.getMinSection(); y < client.level.getMaxSection(); y++)
                    client.levelRenderer.setSectionDirty(x, y, z);
    }
    private List<LegacyFluidRenderPower.Config> previous = List.of();
}
