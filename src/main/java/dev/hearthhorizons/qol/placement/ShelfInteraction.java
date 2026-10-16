package dev.hearthhorizons.qol.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ShelfInteraction {
    public static ItemInteractionResult use(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        ItemStack held = player.getItemInHand(hand);
        int count = state.getValue(PlacementRules.COUNT);
        if (!held.isEmpty() && !PlacementRules.accepts(held)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // Consume the click on BOTH sides; never place the jar as an adjacent vanilla block.
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!player.mayBuild() || !player.canInteractWithBlock(pos, 0)) return ItemInteractionResult.FAIL;
        if (!held.isEmpty()) {
            if (count == 2) return ItemInteractionResult.FAIL;
            if (count == 0) level.setBlock(pos, state.setValue(PlacementRules.COUNT, 1), 3);
            if (!(level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) || !shelf.insert(held)) {
                if (count == 0) level.setBlock(pos, state, 3);
                return ItemInteractionResult.FAIL;
            }
            held.consume(1, player);
            update(level, pos, state, shelf.size());
            return ItemInteractionResult.SUCCESS;
        }
        if (count == 0 || !(level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int slot = select(shelf, state.getValue(HorizontalDirectionalBlock.FACING), player, pos, hit);
        if (slot < 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // Main hand is empty, so removal never needs inventory space and never drops the jar accidentally.
        player.setItemInHand(hand, shelf.remove(slot));
        update(level, pos, state, shelf.size());
        return ItemInteractionResult.SUCCESS;
    }
    private static void update(Level level, BlockPos pos, BlockState previous, int count) {
        BlockState next = previous.setValue(PlacementRules.COUNT, count);
        if (level.getBlockState(pos) != next) level.setBlock(pos, next, 3);
        level.sendBlockUpdated(pos, previous, next, 3);
    }
    private static int select(ShelfBlockEntity shelf, Direction facing, Player player, BlockPos pos, BlockHitResult hit) {
        Vec3 start = player.getEyePosition().subtract(Vec3.atLowerCornerOf(pos));
        Vec3 end = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)).add(player.getLookAngle().scale(0.02));
        double best = Double.MAX_VALUE;
        int selected = -1;
        var poses = shelf.poses();
        for (int i = 0; i < poses.size(); i++) {
            var box = PlacementRules.rotate(poses.get(i).bounds(), facing).inflate(0.001);
            var intersection = box.clip(start, end);
            if (intersection.isPresent()) {
                double distance = start.distanceToSqr(intersection.get());
                if (distance < best) { best = distance; selected = i; }
            }
        }
        return selected;
    }
    private ShelfInteraction() {}
}
