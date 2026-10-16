package dev.hearthhorizons.qol.jei;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import dev.hearthhorizons.qol.culinary.PreparationItems;
import dev.hearthhorizons.qol.culinary.SkilletCookingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/** Registered raw items have independent JEI identities, search entries and recipe stages. */
@JeiPlugin
public final class PreparationJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    public static IJeiRuntime runtime() { return runtime; }
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(HearthHorizonsQualityOfLife.ID, "preparations"); }
    @Override public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(PreparationItems.DEFINITIONS.stream().map(PreparationItems.Definition::stack).toList());
        registration.addExtraItemStacks(dev.hearthhorizons.qol.culinary.CookingPreparations.ALL.stream().map(d -> new net.minecraft.world.item.ItemStack(d.item().get())).toList());
    }
    @Override public void registerCategories(mezz.jei.api.registration.IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SkilletCategory(registration.getJeiHelpers().getGuiHelper()),new ManualMixingCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) {
            var recipes = level.getRecipeManager().getAllRecipesFor(SkilletCookingRecipe.TYPE.get());
            SkilletCategory.IDS.clear();
            recipes.forEach(r -> SkilletCategory.IDS.put(r.value(),r.id()));
            registration.addRecipes(SkilletCategory.TYPE,recipes.stream().map(r -> r.value()).toList());
            var mixing=level.getRecipeManager().<net.minecraft.world.item.crafting.RecipeInput,com.simibubi.create.content.kinetics.mixer.MixingRecipe>getAllRecipesFor(com.simibubi.create.AllRecipeTypes.MIXING.getType());
            ManualMixingCategory.IDS.clear();
            var cold=mixing.stream().filter(r->dev.hearthhorizons.qol.mixer.ManualMixerBlockEntity.allowed(r.value())).toList();
            cold.forEach(r->ManualMixingCategory.IDS.put(r.value(),r.id()));
            registration.addRecipes(ManualMixingCategory.TYPE,cold.stream().map(r->r.value()).toList());
        }
    }
    @Override public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        var skillet = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("farmersdelight:skillet"));
        registration.addRecipeCatalyst(new net.minecraft.world.item.ItemStack(skillet),SkilletCategory.TYPE);
        registration.addRecipeCatalyst(new net.minecraft.world.item.ItemStack(dev.hearthhorizons.qol.mixer.ManualMixer.ITEM.get()),ManualMixingCategory.TYPE);
    }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    @Override public void onRuntimeUnavailable() { runtime = null; }
}
