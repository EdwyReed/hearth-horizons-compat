package dev.hearthhorizons.qol.mixin;

import dev.hearthhorizons.qol.culinary.KitchenContents;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.satisfy.farm_and_charm.core.recipe.StoveRecipe;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

/** Farm & Charm assembles through its block entity, not Recipe.assemble().
 * Capture only the exact ingredient slots that its own matcher will consume.
 */
@Mixin(targets="net.satisfy.farm_and_charm.core.block.entity.StoveBlockEntity",remap=false)
public abstract class StoveServingMixin {
    @Shadow private NonNullList<ItemStack> inventory;
    @Shadow private List<Integer> getPlannedIngredientSlots(StoveRecipe recipe,NonNullList<ItemStack> inventory) { throw new AssertionError(); }
    @Inject(method="generateOutputItem",at=@At("RETURN"),cancellable=true)
    private void hh$capture(StoveRecipe recipe,RegistryAccess registries,CallbackInfoReturnable<ItemStack> cir) {
        var output=cir.getReturnValue();
        if(!KitchenContents.isPastry(output)) return;
        var slots=getPlannedIngredientSlots(recipe,inventory);
        if(slots==null) return;
        var inputs=slots.stream().map(i -> inventory.get(i).copyWithCount(1)).toList();
        cir.setReturnValue(KitchenContents.capture(output.copy(),inputs));
    }
}
