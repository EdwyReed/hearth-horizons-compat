package dev.hearthhorizons.qol.gametest;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import dev.hearthhorizons.qol.culinary.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(HearthHorizonsQualityOfLife.ID)
@PrefixGameTestTemplate(false)
public final class IntegrityGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void restoredRecipesProduceOriginalFoodWithoutAutomaticPreparations(GameTestHelper h) {
        var level=h.getLevel(); var manager=level.getRecipeManager();
        com.google.gson.JsonArray baseline;
        try(var stream=IntegrityGameTests.class.getResourceAsStream("/qa/restored-recipes.json")) {
            baseline=com.google.gson.JsonParser.parseString(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
        } catch(Exception error) { throw new RuntimeException(error); }
        h.assertTrue(baseline.size()==752,"Incomplete restored recipe baseline");
        for(var entry:baseline) {
            var row=entry.getAsJsonObject(); String id=row.get("source_recipe").getAsString();
            var recipe=manager.byKey(ResourceLocation.parse(id)).orElseThrow().value();
            h.assertTrue(recipe instanceof CraftingRecipe,"Restored dish is not crafting: "+id);
            h.assertTrue(!(recipe instanceof ColdPreparationRecipe),"Automatic wrapper still present: "+id);
            boolean shaped=row.get("type").getAsString().equals("minecraft:crafting_shaped");
            h.assertTrue((recipe instanceof ShapedRecipe)==shaped,"Original recipe shape changed: "+id);
            var result=recipe.getResultItem(level.registryAccess());
            h.assertTrue(BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals(row.get("result").getAsString()),"Original result not restored: "+id);
            h.assertTrue(result.getCount()==row.get("count").getAsInt(),"Original yield changed: "+id);
            var expected=new ArrayList<Set<String>>();
            for(var slot:row.getAsJsonArray("expected_ingredients")) {
                var ids=new TreeSet<String>();for(var item:slot.getAsJsonArray()) ids.add(item.getAsString());
                if(!ids.isEmpty()) expected.add(ids);
            }
            for(var ingredient:recipe.getIngredients()) {
                if(ingredient.isEmpty()) continue;
                var actual=new TreeSet<String>();
                for(var stack:ingredient.getItems()) actual.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                h.assertTrue(expected.remove(actual),"Restored ingredient alternatives changed: "+id+" / "+actual);
            }
            h.assertTrue(expected.isEmpty(),"Restored recipe lost ingredients: "+id);
            var cookingId=ResourceLocation.parse("hearth_horizons_qol:cooking/"+id.replace(':','/'));
            h.assertTrue(manager.byKey(cookingId).isEmpty(),"Removed automatic cooking stage remains: "+id);
        }
        h.assertTrue(BuiltInRegistries.ITEM.keySet().stream().noneMatch(id -> id.getNamespace().equals(HearthHorizonsQualityOfLife.ID) && id.getPath().startsWith("uncooked/")),"Automatic preparation items still registered");
        var reviewed=BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals(HearthHorizonsQualityOfLife.ID) && id.getPath().endsWith("_preparation")).count();
        h.assertTrue(reviewed==3,"Expected exactly three reviewed preparations");
        // Upstream cooked_chickens incorrectly included raw chicken cuts.
        // Check the actual resolved ingredients in both affected recipes.
        var rawCuts = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("farmersdelight:chicken_cuts")));
        var cookedCuts = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("farmersdelight:cooked_chicken_cuts")));
        for (String id : List.of("culturalrecipes:chicken_taco", "culturalrecipes:chicken_roll")) {
            var recipe = manager.byKey(ResourceLocation.parse(id)).orElseThrow().value();
            h.assertTrue(recipe.getIngredients().stream().noneMatch(i -> i.test(rawCuts)), "Ready chicken meal accepts raw cuts: " + id);
            h.assertTrue(recipe.getIngredients().stream().anyMatch(i -> i.test(cookedCuts)), "Ready chicken meal rejects cooked cuts: " + id);
        }
        System.out.println("COOKED_CHICKEN_TAG_QA taco and roll reject raw cuts and accept cooked cuts");
        System.out.println("RESTORED_RECIPES_QA all 752 original results, yields, shapes and ingredient alternatives restored; automatic items/stages absent; exactly three reviewed preparations remain");
        h.succeed();
    }
}
