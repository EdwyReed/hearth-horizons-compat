package dev.hearthhorizons.qol.culinary;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import com.mojang.serialization.MapCodec;

/** Cold assembly is ordinary crafting, not instant heating. A filled ingredient
 * returns its original eating vessel only when no native crafting remainder
 * exists. Thus a bowl/bottle is returned once, and source tools keep their logic.
 */
public final class ColdPreparationRecipe extends ShapelessRecipe {
    private final ShapelessRecipe delegate;
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,HearthHorizonsQualityOfLife.ID);
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<ColdPreparationRecipe>> SERIALIZER=SERIALIZERS.register("cold_preparation",() -> new RecipeSerializer<>() {
        private final MapCodec<ColdPreparationRecipe> codec=new ShapelessRecipe.Serializer().codec().xmap(ColdPreparationRecipe::new,r -> r.delegate);
        private final StreamCodec<RegistryFriendlyByteBuf,ColdPreparationRecipe> stream=ShapelessRecipe.Serializer.STREAM_CODEC.map(ColdPreparationRecipe::new,r -> r.delegate);
        @Override public MapCodec<ColdPreparationRecipe> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,ColdPreparationRecipe> streamCodec() { return stream; }
    });
    public static void register(IEventBus bus) { dev.hearthhorizons.qol.migration.LegacyIds.register(bus, SERIALIZERS); }
    public ColdPreparationRecipe(ShapelessRecipe recipe) { super(recipe.getGroup(),recipe.category(),recipe.getResultItem(null),recipe.getIngredients()); delegate=recipe; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return ContainerLedger.remaining(input,delegate.getResultItem(null),super.getRemainingItems(input));
    }
}
