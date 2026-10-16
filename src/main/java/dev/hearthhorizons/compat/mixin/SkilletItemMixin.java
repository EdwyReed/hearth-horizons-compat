package dev.hearthhorizons.compat.mixin;

import dev.hearthhorizons.compat.culinary.SkilletCookingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Mixin(targets="vectorwing.farmersdelight.common.item.SkilletItem",remap=false)
public abstract class SkilletItemMixin {
    @Inject(method="getCookingRecipe",at=@At("HEAD"),cancellable=true)
    private static void hh$frying(ItemStack stack, Level level, CallbackInfoReturnable<Optional<RecipeHolder<CampfireCookingRecipe>>> cir) {
        var recipe=SkilletCookingRecipe.match(stack,level);
        if (recipe.isPresent()) cir.setReturnValue(recipe);
    }
}
