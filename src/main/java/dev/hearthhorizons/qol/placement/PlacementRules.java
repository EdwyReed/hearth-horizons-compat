package dev.hearthhorizons.qol.placement;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;

/** Geometry is in block-local coordinates. A surface owns its layout; objects supply their bounds. */
public final class PlacementRules {
    public static final IntegerProperty COUNT = IntegerProperty.create("hh_items", 0, 2);
    public static final TagKey<Item> PLACEABLE = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(HearthHorizonsQualityOfLife.ID, "shelf_items"));
    public static final Surface SMALL_SHELF = new Surface(5.0 / 16, 12.5 / 16, 6.5 / 16, 10.5 / 16);
    public record ObjectBounds(double width, double height, double depth) {}
    public record Pose(double x, double y, double z, double scale, AABB bounds) {}
    public record Surface(double top, double centreZ, double depth, double height) {
        public List<Pose> layout(int count, ObjectBounds first, ObjectBounds second) {
            if (count == 0) return List.of();
            if (count == 1) return List.of(pose(0.5, 14.0 / 16, first));
            return List.of(pose(4.25 / 16, 5.5 / 16, first), pose(11.75 / 16, 5.5 / 16, second));
        }
        private Pose pose(double x, double width, ObjectBounds object) {
            double scale = Math.min(width / object.width, Math.min(height / object.height, depth / object.depth));
            double w = object.width * scale, h = object.height * scale, d = object.depth * scale;
            return new Pose(x, top, centreZ, scale, new AABB(x - w / 2, top, centreZ - d / 2, x + w / 2, top + h, centreZ + d / 2));
        }
    }
    public static ObjectBounds bounds(ItemStack item) {
        return ObjectProfiles.bounds(item);
    }
    public static boolean accepts(ItemStack item) { return !item.isEmpty() && item.is(PLACEABLE); }
    public static Vec3 rotate(Vec3 v, Direction facing) {
        return switch (facing) {
            case SOUTH -> new Vec3(1 - v.x, v.y, 1 - v.z);
            case EAST -> new Vec3(1 - v.z, v.y, v.x);
            case WEST -> new Vec3(v.z, v.y, 1 - v.x);
            default -> v;
        };
    }
    public static AABB rotate(AABB a, Direction facing) {
        Vec3 p = rotate(new Vec3(a.minX, a.minY, a.minZ), facing);
        Vec3 q = rotate(new Vec3(a.maxX, a.maxY, a.maxZ), facing);
        return new AABB(p, q);
    }
    public static VoxelShape shape(Direction facing, List<Pose> poses) {
        VoxelShape shape = Shapes.create(rotate(new AABB(0, 0, 9.0 / 16, 1, 5.0 / 16, 1), facing));
        for (Pose pose : poses) shape = Shapes.or(shape, Shapes.create(rotate(pose.bounds, facing)));
        return shape;
    }
    private PlacementRules() {}
}
