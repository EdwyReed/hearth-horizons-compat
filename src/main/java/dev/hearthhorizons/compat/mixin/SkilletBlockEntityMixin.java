package dev.hearthhorizons.compat.mixin;

import dev.hearthhorizons.compat.culinary.SkilletCookingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Mixin(targets="vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity",remap=false)
public abstract class SkilletBlockEntityMixin extends BlockEntity {
    protected SkilletBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }
    @Inject(method="getMatchingRecipe",at=@At("HEAD"),cancellable=true)
    private void hh$frying(ItemStack stack, CallbackInfoReturnable<Optional<RecipeHolder<CampfireCookingRecipe>>> cir) {
        if (level != null) {
            var recipe=SkilletCookingRecipe.match(stack,level);
            if (recipe.isPresent()) cir.setReturnValue(recipe);
        }
    }
}
