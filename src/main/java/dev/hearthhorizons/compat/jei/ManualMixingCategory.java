package dev.hearthhorizons.compat.jei;

import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.mixer.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** The same loaded recipes as Create, filtered solely by HeatCondition.NONE. */
public final class ManualMixingCategory implements IRecipeCategory<MixingRecipe> {
    public static final RecipeType<MixingRecipe> TYPE=RecipeType.create(HearthHorizonsCompat.ID,"manual_mixing",MixingRecipe.class);
    public static final Map<MixingRecipe,ResourceLocation> IDS=new IdentityHashMap<>();
    private final IDrawable icon,slot,arrow;
    public ManualMixingCategory(IGuiHelper gui){icon=gui.createDrawableItemStack(new ItemStack(ManualMixer.ITEM.get()));slot=gui.getSlotDrawable();arrow=gui.getRecipeArrow();}
    @Override public RecipeType<MixingRecipe> getRecipeType(){return TYPE;}
    @Override public Component getTitle(){return Component.translatable("jei.hearth_horizons_compat.manual_mixing");}
    @Override public IDrawable getIcon(){return icon;}
    @Override public int getWidth(){return 180;}
    @Override public int getHeight(){return 112;}
    @Override public ResourceLocation getRegistryName(MixingRecipe recipe){return IDS.get(recipe);}
    @Override public void setRecipe(IRecipeLayoutBuilder b,MixingRecipe recipe,IFocusGroup focuses){
        for(int i=0;i<recipe.getIngredients().size();i++)b.addSlot(RecipeIngredientRole.INPUT,3+i%3*19,3+i/3*19).setBackground(slot,-1,-1).addIngredients(recipe.getIngredients().get(i));
        for(int i=0;i<recipe.getFluidIngredients().size();i++){
            var fluids=recipe.getFluidIngredients().get(i);
            b.addSlot(RecipeIngredientRole.INPUT,3+i*19,66).setBackground(slot,-1,-1).addIngredients(NeoForgeTypes.FLUID_STACK,Arrays.asList(fluids.getFluids())).setFluidRenderer(Math.max(1000,fluids.amount()),false,16,16);
        }
        var outputs=recipe.getRollableResults();
        for(int i=0;i<outputs.size();i++){
            var result=outputs.get(i);var s=b.addSlot(RecipeIngredientRole.OUTPUT,123+i%3*19,3+i/3*19).setBackground(slot,-1,-1).addItemStack(result.getStack());
            if(result.getChance()<1)s.addTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.hearth_horizons_compat.manual_mixing.chance",Math.round(result.getChance()*100))));
        }
        for(int i=0;i<recipe.getFluidResults().size();i++){
            var fluid=recipe.getFluidResults().get(i);
            b.addSlot(RecipeIngredientRole.OUTPUT,123+i*19,66).setBackground(slot,-1,-1).addIngredient(NeoForgeTypes.FLUID_STACK,fluid).setFluidRenderer(Math.max(1000,fluid.getAmount()),false,16,16);
        }
    }
    @Override public void draw(MixingRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
        arrow.draw(g,79,25);
        g.drawString(Minecraft.getInstance().font,Component.translatable("jei.hearth_horizons_compat.manual_mixing.time",ManualMixerBlockEntity.duration(recipe)/20),3,92,0xff555555,false);
    }
}
