package dev.hearthhorizons.qol.culinary;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import com.mojang.serialization.MapCodec;
import java.util.*;

/** Native shapeless matching/JEI, with actual input stacks carried into the serving.
 * Source mod, custom components and exact food values survive the craft.
 */
public final class ServingRecipe extends ShapelessRecipe {
    private final ShapelessRecipe delegate;
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, HearthHorizonsQualityOfLife.ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ServingRecipe>> SERIALIZER = SERIALIZERS.register("serving", () -> new RecipeSerializer<>() {
        private final MapCodec<ServingRecipe> codec = new ShapelessRecipe.Serializer().codec().xmap(ServingRecipe::new, r -> r.delegate);
        private final StreamCodec<RegistryFriendlyByteBuf, ServingRecipe> stream = ShapelessRecipe.Serializer.STREAM_CODEC.map(ServingRecipe::new, r -> r.delegate);
        @Override public MapCodec<ServingRecipe> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ServingRecipe> streamCodec() { return stream; }
    });
    public static void register(IEventBus bus) { dev.hearthhorizons.qol.migration.LegacyIds.register(bus, SERIALIZERS); }
    public ServingRecipe(ShapelessRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.getResultItem(null), recipe.getIngredients());
        delegate = recipe;
    }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return KitchenContents.capture(delegate.assemble(input, registries),input.items());
    }
    @Override public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return ContainerLedger.remaining(input,delegate.getResultItem(null),super.getRemainingItems(input));
    }
}
