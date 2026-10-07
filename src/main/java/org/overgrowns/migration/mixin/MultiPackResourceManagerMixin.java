package org.overgrowns.migration.mixin;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.overgrowns.migration.LegacyResourceConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

/** Captures the data namespaces before reload listeners evaluate apoli:*_namespace(s)_loaded conditions. */
@Mixin(MultiPackResourceManager.class)
public abstract class MultiPackResourceManagerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void overgrownLegacyBridge$namespaces(PackType type, List<PackResources> packs, CallbackInfo ci) {
        if (type == PackType.SERVER_DATA)
            LegacyResourceConditions.loadedNamespaces = Set.copyOf(((MultiPackResourceManager) (Object) this).getNamespaces());
    }
}
