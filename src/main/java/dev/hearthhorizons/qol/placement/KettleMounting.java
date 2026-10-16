package dev.hearthhorizons.qol.placement;

import net.favouriteless.enchanted.common.blocks.KettleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Native TYPE records the player's mounting choice; no extra saved properties are needed. */
public final class KettleMounting {
    public static BlockState resolve(KettleBlock block, Level level, BlockPos pos, Direction facing,
                                      KettleBlock.Type preferred, Direction clickedSupport) {
        boolean floor = hasSupport(level, pos.below());
        boolean ceiling = hasSupport(level, pos.above());
        Direction side = sideSupport(level, pos, facing, clickedSupport);
        if (preferred == KettleBlock.Type.GROUND && floor) return state(block, preferred, facing);
        if (preferred == KettleBlock.Type.HANGING && ceiling) return state(block, preferred, facing);
        if (preferred == KettleBlock.Type.HANGING_BEAM && side != null) return beam(block, facing, side);
        // Reattach when the chosen support disappears. Unsupported kettles use the ordinary model.
        if (floor) return state(block, KettleBlock.Type.GROUND, facing);
        if (side != null) return beam(block, facing, side);
        return state(block, ceiling ? KettleBlock.Type.HANGING : KettleBlock.Type.GROUND, facing);
    }

    private static boolean hasSupport(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static Direction sideSupport(Level level, BlockPos pos, Direction facing, Direction clicked) {
        if (clicked != null && clicked.getAxis().isHorizontal() && hasSupport(level, pos.relative(clicked))) return clicked;
        for (Direction side : new Direction[] {facing.getCounterClockWise(), facing.getClockWise(), facing, facing.getOpposite()}) {
            if (hasSupport(level, pos.relative(side))) return side;
        }
        return null;
    }

    private static BlockState beam(KettleBlock block, Direction facing, Direction side) {
        return state(block, KettleBlock.Type.HANGING_BEAM,
            facing.getAxis() == side.getAxis() ? facing.getClockWise() : facing);
    }

    private static BlockState state(KettleBlock block, KettleBlock.Type type, Direction facing) {
        return block.defaultBlockState().setValue(KettleBlock.TYPE, type).setValue(KettleBlock.FACING, facing);
    }

    private KettleMounting() {}
}
