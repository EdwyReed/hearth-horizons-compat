package dev.hearthhorizons.qol.jei;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import dev.hearthhorizons.qol.culinary.SkilletCookingRecipe;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class SkilletCategory implements IRecipeCategory<SkilletCookingRecipe> {
    public static final RecipeType<SkilletCookingRecipe> TYPE=RecipeType.create(HearthHorizonsQualityOfLife.ID,"skillet",SkilletCookingRecipe.class);
    public static final Map<SkilletCookingRecipe,ResourceLocation> IDS=new IdentityHashMap<>();
    private final IDrawable icon,arrow,slot;
    public SkilletCategory(IGuiHelper gui) {
        icon=gui.createDrawableItemStack(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("farmersdelight:skillet"))));
        arrow=gui.getRecipeArrow(); slot=gui.getSlotDrawable();
    }
    @Override public RecipeType<SkilletCookingRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.hearth_horizons_qol.skillet"); }
    @Override public int getWidth() { return 140; }
    @Override public int getHeight() { return 60; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public ResourceLocation getRegistryName(SkilletCookingRecipe recipe) { return IDS.get(recipe); }
    @Override public void setRecipe(IRecipeLayoutBuilder builder,SkilletCookingRecipe recipe,IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT,16,12).setBackground(slot,-1,-1).addIngredients(recipe.getIngredients().getFirst());
        builder.addSlot(RecipeIngredientRole.OUTPUT,105,12).setBackground(slot,-1,-1).addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }
    @Override public void draw(SkilletCookingRecipe recipe,IRecipeSlotsView slots,GuiGraphics graphics,double x,double y) {
        arrow.draw(graphics,57,12);
        graphics.drawString(Minecraft.getInstance().font,Component.translatable("jei.hearth_horizons_qol.skillet_heat",recipe.getCookingTime()/20),6,41,0xFF555555,false);
    }
}
