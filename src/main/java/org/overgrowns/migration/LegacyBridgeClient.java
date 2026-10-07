package org.overgrowns.migration;
import dev.overgrown.apoli.power.PowerLookup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.MixinEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
public final class LegacyBridgeClient implements ClientModInitializer {
    public void onInitializeClient() {
        if (Boolean.getBoolean("overgrown_legacy_bridge.auditMixins")) ClientLifecycleEvents.CLIENT_STARTED.register(LegacyBridgeClient::auditMixins);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LegacyBridge.clearHandlerScopes();
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
    /**
     * CI only: loads every mixin target (client ones included, which the test servers never apply), records the
     * outcome in mixin-audit.txt and closes the client. A required injector that misses throws here.
     */
    private static void auditMixins(Minecraft client) {
        String result;
        try {
            MixinEnvironment.getCurrentEnvironment().audit();
            result = "passed";
        } catch (Throwable error) {
            LegacyBridge.LOGGER.error("Mixin audit failed", error);
            result = "failed: " + error;
        }
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("mixin-audit.txt"), result + "\n");
        } catch (IOException error) {
            LegacyBridge.LOGGER.error("Could not write the mixin audit result", error);
        }
        client.stop();
    }
    private List<LegacyFluidRenderPower.Config> previous = List.of();
}
