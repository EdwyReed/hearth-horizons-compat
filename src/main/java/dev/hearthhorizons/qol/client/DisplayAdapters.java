package dev.hearthhorizons.qol.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

/** Client extension API. New item families may prepare cached data and wrap their native draw call. */
public final class DisplayAdapters {
    public interface Prepared { void render(Runnable nativeDraw); }
    public interface Adapter {
        Prepared prepare(ItemStack item);
        default double modelScaleCorrection() { return 1; }
        default double baseLift() { return 0; }
    }
    private static final Adapter DEFAULT = item -> Runnable::run;
    private static final Map<ResourceLocation, Adapter> ADAPTERS = new HashMap<>();
    static {
        register(ResourceLocation.parse("hexerei:herb_jar"), new Adapter() {
            public Prepared prepare(ItemStack item) {
                var snapshot = HerbJarRenderScope.Snapshot.from(item);
                return draw -> HerbJarRenderScope.render(snapshot, draw);
            }
            public double modelScaleCorrection() { return 1 / 0.75; }
            // Native Hexerei item transforms put the glass base .03 model units below its authored origin.
            public double baseLift() { return 0.03; }
        });
    }
    public static void register(ResourceLocation item, Adapter adapter) { ADAPTERS.put(item, adapter); }
    public static Adapter forItem(ItemStack item) { return ADAPTERS.getOrDefault(BuiltInRegistries.ITEM.getKey(item.getItem()), DEFAULT); }
    private DisplayAdapters() {}
}
