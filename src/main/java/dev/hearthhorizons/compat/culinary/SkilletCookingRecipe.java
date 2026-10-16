package dev.hearthhorizons.compat.culinary;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.Optional;

/** Dedicated recipe type prevents campfires, ovens and recipe auto-converters
 * from claiming frying. FD's placed and handheld skillets opt in explicitly.
 */
public final class SkilletCookingRecipe extends CampfireCookingRecipe {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE,HearthHorizonsCompat.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER,HearthHorizonsCompat.ID);
    public static final DeferredHolder<RecipeType<?>,RecipeType<SkilletCookingRecipe>> TYPE = TYPES.register("skillet", () -> new RecipeType<>() { @Override public String toString() { return HearthHorizonsCompat.ID + ":skillet"; } });
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<SkilletCookingRecipe>> SERIALIZER = SERIALIZERS.register("skillet", () -> new SimpleCookingSerializer<>(SkilletCookingRecipe::new,200));
    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
    public SkilletCookingRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int time) {
        super(group,category,ingredient,result,experience,time);
    }
    @Override public RecipeType<?> getType() { return TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    public static Optional<RecipeHolder<CampfireCookingRecipe>> match(ItemStack stack, Level level) {
        return level.getRecipeManager().getRecipeFor(TYPE.get(),new SingleRecipeInput(stack),level)
            .map(r -> new RecipeHolder<CampfireCookingRecipe>(r.id(),r.value()));
    }
}
