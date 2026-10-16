package dev.hearthhorizons.qol.gametest;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import dev.hearthhorizons.qol.culinary.*;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(HearthHorizonsQualityOfLife.ID)
@PrefixGameTestTemplate(false)
public final class ContainerGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void reviewedColdMealsRequireCookedMeat(GameTestHelper h) {
        com.google.gson.JsonArray changes;
        try (var input = java.util.Objects.requireNonNull(ContainerGameTests.class.getResourceAsStream("/qa/cold-meat-changes.json"))) {
            changes=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
        } catch(Exception error) { throw new RuntimeException(error); }
        for(var change:changes) {
            var row=change.getAsJsonObject();String id=row.get("id").getAsString();
            var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow().value();
            var ingredients=recipe.getIngredients();
            var grid=new ArrayList<ItemStack>();
            for(var ingredient:ingredients) grid.add(ingredient.getItems()[0].copyWithCount(1));
            while(grid.size()<9) grid.add(ItemStack.EMPTY);
            h.assertTrue(recipe.matches(CraftingInput.of(3,3,grid),h.getLevel()),"Cold serving rejects cooked input: "+id);
            for(var substitution:row.getAsJsonArray("substitutions")) {
                var slot=substitution.getAsJsonObject();int index=slot.get("slot").getAsInt();
                var expected=new TreeSet<String>();for(var allowed:slot.getAsJsonArray("allowed")) expected.add(allowed.getAsString());
                var actual=new TreeSet<String>();for(var item:ingredients.get(index).getItems()) actual.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
                h.assertTrue(actual.equals(expected),"Cold serving alternatives drifted: "+id);
                for(var raw:slot.getAsJsonObject("mapping").keySet()) {
                    var test=new ArrayList<>(grid);
                    test.set(index,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(raw))));
                    h.assertTrue(!recipe.matches(CraftingInput.of(3,3,test),h.getLevel()),"Cold meal accepts uncooked meat: "+id+" / "+raw);
                }
            }
        }
        System.out.println("COLD_MEAT_QA "+changes.size()+" actual cold recipes accept cooked alternatives and reject original raw meat");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void coldMealsReturnSodaGlassAndNeverDuplicateServingBowls(GameTestHelper h) {
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("pamhc2foodextended:friedfeastitem")).orElseThrow().value();
        var items=new ArrayList<ItemStack>();
        for(var ingredient:recipe.getIngredients()) items.add(ingredient.getItems()[0].copyWithCount(1));
        while(items.size()<9) items.add(ItemStack.EMPTY);
        var input=CraftingInput.of(3,3,items);
        h.assertTrue(recipe.matches(input,h.getLevel()),"Real cold meal recipe no longer accepts its ingredients");
        var remaining=recipe.getRemainingItems(input);
        h.assertTrue(remaining.stream().filter(s -> s.is(Items.GLASS_BOTTLE)).mapToInt(ItemStack::getCount).sum()==1,"Meal loses/duplicates soda glass");
        var stewInput=CraftingInput.of(1,1,List.of(new ItemStack(Items.MUSHROOM_STEW)));
        var carried=ContainerLedger.remaining(stewInput,new ItemStack(Items.RABBIT_STEW),NonNullList.withSize(1,ItemStack.EMPTY));
        h.assertTrue(carried.getFirst().isEmpty(),"A bowl carried into the finished serving was also returned empty");
        var reused=ContainerLedger.remaining(stewInput,new ItemStack(Items.RABBIT_STEW),NonNullList.of(ItemStack.EMPTY,new ItemStack(Items.BOWL)));
        h.assertTrue(reused.getFirst().isEmpty(),"Native remainder duplicated a carried serving bowl");
        var unpacked=ContainerLedger.remaining(stewInput,new ItemStack(Items.BREAD),NonNullList.withSize(1,ItemStack.EMPTY));
        h.assertTrue(unpacked.getFirst().is(Items.BOWL),"A non-carried ingredient bowl disappeared");
        System.out.println("CONTAINER_QA native cold meal returns one soda bottle; FOOD vessels are returned once or retained inside the final serving, including native crafting remainders");
        h.succeed();
    }
}
