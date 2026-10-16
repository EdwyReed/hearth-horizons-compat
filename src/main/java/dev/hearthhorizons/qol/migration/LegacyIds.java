package dev.hearthhorizons.qol.migration;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Read old saves through NeoForge aliases; all subsequent writes use the new IDs. */
public final class LegacyIds {
    public static final String NAMESPACE = "hearth_horizons_compat";

    public static void register(IEventBus bus, DeferredRegister<?> registry) {
        for (var entry : registry.getEntries()) {
            registry.addAlias(ResourceLocation.fromNamespaceAndPath(NAMESPACE, entry.getId().getPath()), entry.getId());
        }
        registry.register(bus);
    }

    public static ResourceLocation migrate(ResourceLocation id) {
        return id != null && id.getNamespace().equals(NAMESPACE)
            ? ResourceLocation.fromNamespaceAndPath(HearthHorizonsQualityOfLife.ID, id.getPath()) : id;
    }

    private LegacyIds() {}
}
