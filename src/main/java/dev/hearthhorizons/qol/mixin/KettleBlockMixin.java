package dev.hearthhorizons.qol.mixin;

import dev.hearthhorizons.qol.placement.KettleMounting;
import net.favouriteless.enchanted.common.blocks.KettleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KettleBlock.class, remap = false)
public abstract class KettleBlockMixin {
    @Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
    private void hh$chooseClickedSupport(BlockPlaceContext context, CallbackInfoReturnable<BlockState> result) {
        Direction face = context.getClickedFace();
        var preferred = face == Direction.UP ? KettleBlock.Type.GROUND
            : face == Direction.DOWN ? KettleBlock.Type.HANGING : KettleBlock.Type.HANGING_BEAM;
        result.setReturnValue(KettleMounting.resolve((KettleBlock) (Object) this, context.getLevel(),
            context.getClickedPos(), context.getHorizontalDirection().getOpposite(), preferred, face.getOpposite()));
    }

    @Inject(method = "getKettleState", at = @At("HEAD"), cancellable = true)
    private void hh$keepChosenSupport(Level level, BlockPos pos, Direction facing,
                                      CallbackInfoReturnable<BlockState> result) {
        var block = (KettleBlock) (Object) this;
        var current = level.getBlockState(pos);
        var preferred = current.is(block) ? current.getValue(KettleBlock.TYPE) : KettleBlock.Type.GROUND;
        result.setReturnValue(KettleMounting.resolve(block, level, pos, facing, preferred, null));
    }
}
