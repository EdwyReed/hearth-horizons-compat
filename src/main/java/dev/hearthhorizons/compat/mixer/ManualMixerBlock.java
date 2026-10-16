package dev.hearthhorizons.compat.mixer;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.basin.*;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Native basin storage/recipe semantics, but an independent one-cell hand machine. */
public final class ManualMixerBlock extends BasinBlock {
    public static final MapCodec<ManualMixerBlock> CODEC = simpleCodec(ManualMixerBlock::new);
    public static final DirectionProperty ORIENTATION = DirectionProperty.create("orientation",Direction.Plane.HORIZONTAL);
    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(0,0,2.5,16,2,13.5), Block.box(0.5,1.5,3.5,9.5,7,12.5),
        Block.box(10.3,1.5,6,14.5,16,10), Block.box(2,10.8,6,14.5,15.5,10),
        Block.box(10.3,8,10,14.5,15,16));
    public ManualMixerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ORIENTATION, Direction.SOUTH).setValue(BasinBlock.FACING,Direction.DOWN));
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(ORIENTATION);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ORIENTATION, context.getHorizontalDirection().getOpposite());
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return true; }
    @Override public Class<BasinBlockEntity> getBlockEntityClass() { return BasinBlockEntity.class; }
    @Override public BlockEntityType<? extends BasinBlockEntity> getBlockEntityType() { return ManualMixer.TYPE.get(); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        var direction=state.getValue(ORIENTATION);
        if(direction==Direction.SOUTH) return SHAPE;
        VoxelShape rotated=Shapes.empty();
        for(AABB b:SHAPE.toAabbs()) {
            if(direction==Direction.NORTH) rotated=Shapes.or(rotated,Shapes.create(1-b.maxX,b.minY,1-b.maxZ,1-b.minX,b.maxY,1-b.minZ));
            else if(direction==Direction.EAST) rotated=Shapes.or(rotated,Shapes.create(b.minZ,b.minY,1-b.maxX,b.maxZ,b.maxY,1-b.minX));
            else rotated=Shapes.or(rotated,Shapes.create(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX));
        }
        return rotated;
    }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override public VoxelShape getInteractionShape(BlockState s,BlockGetter l,BlockPos p){return getShape(s,l,p,CollisionContext.empty());}
    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if(!context.getLevel().isClientSide)context.getLevel().setBlock(context.getClickedPos(),state.setValue(ORIENTATION,state.getValue(ORIENTATION).getClockWise()),3);
        return InteractionResult.SUCCESS;
    }
    public static double localX(BlockState state, BlockPos pos, Vec3 hit) {
        double x=hit.x-pos.getX(),z=hit.z-pos.getZ();
        return switch(state.getValue(ORIENTATION)){case NORTH->1-x;case EAST->1-z;case WEST->z;default->x;};
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player.isSpectator()||!player.mayBuild())return InteractionResult.PASS;
        useItemOn(ItemStack.EMPTY,state,level,pos,player,InteractionHand.MAIN_HAND,hit);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(player.isSpectator()||!player.mayBuild())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!(level.getBlockEntity(pos) instanceof ManualMixerBlockEntity mixer))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // The upright/crank area is distinct from the bowl, including when facing E/W.
        if(localX(state,pos,hit.getLocation())>.60 && hit.getLocation().y-pos.getY()>.44) {
            if(!level.isClientSide)mixer.turn(player);
            return ItemInteractionResult.SUCCESS;
        }
        if(level.isClientSide)return ItemInteractionResult.SUCCESS;
        if(!held.isEmpty()) {
            if(FluidHelper.tryEmptyItemIntoBE(level,player,hand,held,mixer)||FluidHelper.tryFillItemFromBE(level,player,hand,held,mixer))return ItemInteractionResult.SUCCESS;
            if(GenericItemEmptying.canItemBeEmptied(level,held)||GenericItemFilling.canItemBeFilled(level,held))return ItemInteractionResult.SUCCESS;
            int count=player.isShiftKeyDown()?held.getCount():1;
            var remaining=ItemHandlerHelper.insertItemStacked(mixer.getInputInventory(),held.copyWithCount(count),false);
            int inserted=count-remaining.getCount();
            if(inserted>0){if(!player.isCreative())held.shrink(inserted);mixer.notifyUpdate();}
        }else {
            var inv=mixer.items();
            for(int i=0;i<inv.getSlots();i++){
                var taken=inv.extractItem(i,player.isShiftKeyDown()?64:1,false);
                if(taken.isEmpty())continue;
                player.getInventory().placeItemBackInInventory(taken);mixer.notifyUpdate();
                if(!player.isShiftKeyDown())break;
            }
        }
        return ItemInteractionResult.SUCCESS;
    }
    @Override public void updateEntityAfterFallOn(BlockGetter level,net.minecraft.world.entity.Entity entity) {
        // The machine is not an open vanilla basin into which dropped entities auto-insert.
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1,0,1));
    }
}
