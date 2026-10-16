package dev.hearthhorizons.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hearthhorizons.compat.placement.ShelfBlockEntity;
import net.joefoxe.hexerei.item.custom.HerbJarItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import java.util.Map;
import java.util.WeakHashMap;

public final class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity> {
    private record Cache(int revision, long resourceGeneration, DisplayAdapters.Prepared[] prepared) {}
    private final Map<ShelfBlockEntity, Cache> cache = new WeakHashMap<>();
    public static long resourceGeneration;
    private static final boolean QA = Boolean.getBoolean("hh.shelfQa");
    private final java.util.Set<net.minecraft.core.BlockPos> audited = new java.util.HashSet<>();
    @Override public int getViewDistance() { return 32; }
    @Override public void render(ShelfBlockEntity shelf, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (shelf.size() == 0) return;
        BoundsAudit audit = QA && audited.add(shelf.getBlockPos()) ? new BoundsAudit(buffers, pose.last().pose()) : null;
        MultiBufferSource targetBuffers = audit == null ? buffers : audit;
        Cache entry = cache.get(shelf);
        if (entry == null || entry.revision != shelf.revision() || entry.resourceGeneration != resourceGeneration) {
            var prepared = new DisplayAdapters.Prepared[2];
            for (int i = 0; i < shelf.size(); i++) prepared[i] = DisplayAdapters.forItem(shelf.item(i)).prepare(shelf.item(i));
            entry = new Cache(shelf.revision(), resourceGeneration, prepared);
            cache.put(shelf, entry);
        }
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        float angle = switch (shelf.getBlockState().getValue(HorizontalDirectionalBlock.FACING)) {
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-0.5, 0, -0.5);
        var layout = shelf.poses();
        for (int i = 0; i < shelf.size(); i++) {
            var p = layout.get(i);
            ItemStack item = shelf.item(i);
            pose.pushPose();
            // FIXED item transforms are applied by the native renderer; undo jar's .75 model scale.
            double modelScale = p.scale() * DisplayAdapters.forItem(item).modelScaleCorrection();
            pose.translate(p.x(), p.y() + p.bounds().getYsize() / 2 + p.scale() * DisplayAdapters.forItem(item).baseLift(), p.z());
            pose.scale((float) modelScale, (float) modelScale, (float) modelScale);
            Runnable draw = () -> Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, light, overlay, pose, targetBuffers, shelf.getLevel(), 0);
            entry.prepared[i].render(draw);
            pose.popPose();
        }
        pose.popPose();
        if (audit != null) System.out.println("SHELF_QA_GEOMETRY " + shelf.getBlockPos() + " count=" + shelf.size() + " " + audit.report());
    }
}
