package dev.hearthhorizons.qol.client;

import net.joefoxe.hexerei.item.custom.HerbJarItemRenderer;
import net.joefoxe.hexerei.tileentity.HerbJarTile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** A render-thread scope, never a second world BlockEntity or a server-side inventory. */
public final class HerbJarRenderScope {
    public static long decodedSnapshots;
    public static long reusedSnapshots;
    public static final boolean METRICS = Boolean.getBoolean("hh.shelfQa");
    public record Snapshot(ItemStack owner, CompoundTag data, HerbJarTile display) {
        public static Snapshot from(ItemStack item) {
            decodedSnapshots++;
            CompoundTag data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return new Snapshot(item, data, HerbJarItemRenderer.loadBlockEntityFromItem(data, item));
        }
    }
    private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();
    public static Snapshot current() { return CURRENT.get(); }
    public static void render(Snapshot snapshot, Runnable draw) {
        Snapshot previous = CURRENT.get();
        CURRENT.set(snapshot);
        try { draw.run(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    private HerbJarRenderScope() {}
}
