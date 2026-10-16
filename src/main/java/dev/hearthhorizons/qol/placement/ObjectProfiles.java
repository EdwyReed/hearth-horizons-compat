package dev.hearthhorizons.qol.placement;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Extension API: item families can add authored bounds without changing the shelf implementation. */
public final class ObjectProfiles {
    private static final Map<ResourceLocation, PlacementRules.ObjectBounds> PROFILES = new ConcurrentHashMap<>();
    private static final PlacementRules.ObjectBounds DEFAULT = new PlacementRules.ObjectBounds(1, 1, 1);
    static { register(ResourceLocation.parse("hexerei:herb_jar"), new PlacementRules.ObjectBounds(9.1 / 16, 15.0 / 16, 9.1 / 16)); }
    public static void register(ResourceLocation item, PlacementRules.ObjectBounds bounds) {
        if (bounds.width() <= 0 || bounds.height() <= 0 || bounds.depth() <= 0) throw new IllegalArgumentException("Positive display bounds required");
        PROFILES.put(item, bounds);
    }
    public static PlacementRules.ObjectBounds bounds(ItemStack item) { return PROFILES.getOrDefault(BuiltInRegistries.ITEM.getKey(item.getItem()), DEFAULT); }
    private ObjectProfiles() {}
}
