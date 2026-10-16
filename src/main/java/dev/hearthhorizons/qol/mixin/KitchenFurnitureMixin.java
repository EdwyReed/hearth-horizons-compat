package dev.hearthhorizons.qol.mixin;

import dev.hearthhorizons.qol.culinary.*;
import net.mehvahdjukaar.moonlight.api.block.IOptionalEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.redchujelly.cluttered.block.custom.CustomHorizontalBlock;
import net.redchujelly.cluttered.block.custom.furniture.SmallFurnitureBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(value = SmallFurnitureBlock.class, remap = false)
public abstract class KitchenFurnitureMixin extends CustomHorizontalBlock implements EntityBlock, IOptionalEntityBlock {
    protected KitchenFurnitureMixin(Properties properties) { super(properties); }
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void hh$portions(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        if (KitchenContents.portions((Block)(Object)this)) builder.add(KitchenContents.PORTIONS);
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    private void hh$fullByDefault(Properties properties, CallbackInfo ci) {
        if (defaultBlockState().hasProperty(KitchenContents.PORTIONS)) registerDefaultState(defaultBlockState().setValue(KitchenContents.PORTIONS, 4));
    }
    @Override public boolean shouldHaveBlockEntity(BlockBehaviour.BlockStateBase state) { return KitchenContents.supported((Block)(Object)this); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return KitchenContents.supported((Block)(Object)this) ? new KitchenBlockEntity(pos,state) : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if (level.getBlockEntity(pos) instanceof KitchenBlockEntity contents) contents.place(stack);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof KitchenBlockEntity contents) return List.of(contents.packed());
        return super.getDrops(state,params);
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) {
        return KitchenContents.supported((Block)(Object)this) ? PushReaction.BLOCK : super.getPistonPushReaction(state);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide && player.isCreative() && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)
            && level.getBlockEntity(pos) instanceof KitchenBlockEntity contents && contents.hasSnapshot()) {
            var packed=contents.packed();
            net.minecraft.world.Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.3,pos.getZ()+.5,packed);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
}
