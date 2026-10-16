package dev.hearthhorizons.compat.mixin;

import dev.hearthhorizons.compat.placement.*;
import net.mehvahdjukaar.moonlight.api.block.IOptionalEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.redchujelly.cluttered.block.custom.furniture.SmallFurnitureBlock;
import net.redchujelly.cluttered.block.custom.furniture.SmallShelfBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = SmallShelfBlock.class, remap = false)
public abstract class SmallShelfMixin extends SmallFurnitureBlock implements EntityBlock, IOptionalEntityBlock {
    protected SmallShelfMixin(Properties properties) { super(properties); }
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/redchujelly/cluttered/block/custom/furniture/SmallFurnitureBlock;<init>(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V"), index = 0)
    private static Properties hh$dynamicShape(Properties properties) { return properties.dynamicShape(); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PlacementRules.COUNT);
    }
    @Override public boolean shouldHaveBlockEntity(BlockBehaviour.BlockStateBase state) { return state.getValue(PlacementRules.COUNT) > 0; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return shouldHaveBlockEntity(state) ? new ShelfBlockEntity(pos, state) : null;
    }
    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void hh$shape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> result) {
        if (shouldHaveBlockEntity(state) && world.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) result.setReturnValue(shelf.shape(state.getValue(FACING)));
        else result.setReturnValue(PlacementRules.shape(state.getValue(FACING), List.of()));
    }
    // Collision depends on contents; bypass the default precomputed BlockState shape cache.
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ShelfInteraction.use(level, pos, state, player, hand, hit);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ItemInteractionResult result = ShelfInteraction.use(level, pos, state, player, InteractionHand.MAIN_HAND, hit);
        return result == ItemInteractionResult.SUCCESS ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    @Override public void onBlockStateChange(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockStateChange(world, pos, oldState, newState);
        if (world instanceof Level level && oldState.is(newState.getBlock()) && !shouldHaveBlockEntity(newState)) level.removeBlockEntity(pos);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) {
            if (!level.isClientSide) {
                for (int slot = 0; slot < shelf.size(); slot++) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5, shelf.item(slot).copy());
            }
            shelf.clearForDrop();
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) {
        return shouldHaveBlockEntity(state) ? PushReaction.BLOCK : super.getPistonPushReaction(state);
    }
}
