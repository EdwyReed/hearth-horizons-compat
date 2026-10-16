package dev.hearthhorizons.compat.culinary;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;

/** A vessel is either carried into the finished serving or returned once.
 * FOOD.usingConvertsTo is not a native crafting remainder in 1.21.1.
 */
public final class ContainerLedger {
    public static NonNullList<ItemStack> remaining(CraftingInput input,ItemStack output,NonNullList<ItemStack> nativeRemainders) {
        var finalFood=output.get(DataComponents.FOOD);
        int outputCount=output.getCount();
        if(finalFood==null) {
            for(var definition:CookingPreparations.ALL) if(output.is(definition.item().get())) {
                var ready=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(definition.result())));
                finalFood=ready.get(DataComponents.FOOD);outputCount=definition.count();break;
            }
        }
        var vessel=finalFood==null?ItemStack.EMPTY:finalFood.usingConvertsTo().orElse(ItemStack.EMPTY);
        int carry=vessel.isEmpty()?0:outputCount;
        for(var stack:input.items()) if(!vessel.isEmpty() && stack.is(vessel.getItem())) carry--;
        carry=Math.max(0,carry);
        for(int i=0;i<input.size();i++) {
            var remainder=nativeRemainders.get(i);
            if(remainder.isEmpty()) {
                var food=input.getItem(i).get(DataComponents.FOOD);
                if(food!=null) remainder=food.usingConvertsTo().map(ItemStack::copy).orElse(ItemStack.EMPTY);
            }
            if(!remainder.isEmpty() && !vessel.isEmpty() && remainder.is(vessel.getItem()) && carry>0) {
                int retained=Math.min(carry,remainder.getCount());carry-=retained;
                remainder=remainder.copy();remainder.shrink(retained);
            }
            nativeRemainders.set(i,remainder);
        }
        return nativeRemainders;
    }
    private ContainerLedger() {}
}
