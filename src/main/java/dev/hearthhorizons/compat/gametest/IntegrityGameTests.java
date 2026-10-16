package dev.hearthhorizons.compat.gametest;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.culinary.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(HearthHorizonsCompat.ID)
@PrefixGameTestTemplate(false)
public final class IntegrityGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void allHotManualDishesRequireTheirCookingStage(GameTestHelper h) {
        var level=h.getLevel(); var manager=level.getRecipeManager();
        com.google.gson.JsonArray baseline;
        try(var stream=IntegrityGameTests.class.getResourceAsStream("/culinary-preparations.json")) {
            baseline=com.google.gson.JsonParser.parseString(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
        } catch(Exception error) { throw new RuntimeException(error); }
        var expected=new HashMap<String,List<Set<String>>>();
        for(var entry:baseline) {
            var row=entry.getAsJsonObject();var slots=new ArrayList<Set<String>>();
            for(var slot:row.getAsJsonArray("expected_ingredients")) {
                var ids=new TreeSet<String>();for(var id:slot.getAsJsonArray()) ids.add(id.getAsString());if(!ids.isEmpty()) slots.add(ids);
            }
            expected.put(row.get("source_recipe").getAsString(),slots);
        }
        for(var definition:CookingPreparations.ALL) {
            var raw=new ItemStack(definition.item().get());
            h.assertTrue(!raw.has(DataComponents.FOOD),"Uncooked dish edible: "+definition.recipe());
            var preparation=manager.byKey(ResourceLocation.parse(definition.recipe())).orElseThrow().value();
            h.assertTrue(preparation instanceof ColdPreparationRecipe,"Hot dish still instant: "+definition.recipe());
            h.assertTrue(preparation.getResultItem(level.registryAccess()).is(raw.getItem()),"Cold stage returns cooked dish");
            var actual=new ArrayList<Set<String>>();
            for(var ingredient:preparation.getIngredients()) {
                var ids=new TreeSet<String>();for(var stack:ingredient.getItems()) ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());actual.add(ids);
            }
            var before=new ArrayList<>(expected.get(definition.recipe()));
            for(var slot:actual) h.assertTrue(before.remove(slot),"Cold preparation changed an original ingredient: "+definition.recipe());
            h.assertTrue(before.isEmpty(),"Cold preparation lost an original ingredient: "+definition.recipe());
            var id=definition.recipe().replace(':','/');
            var cooking=manager.byKey(ResourceLocation.parse("hearth_horizons_compat:cooking/"+id)).orElseThrow().value();
            var expectedItem=BuiltInRegistries.ITEM.get(ResourceLocation.parse(definition.result()));
            var result=cooking.getResultItem(level.registryAccess());
            h.assertTrue(result.is(expectedItem) && result.getCount()==definition.count(),"Cooking changed final ID/yield: "+definition.recipe());
            h.assertTrue(cooking.getIngredients().size()==1 && cooking.getIngredients().getFirst().test(raw),"Cooking rejects raw batch");
            h.assertTrue(!cooking.getIngredients().getFirst().test(result),"Cooked dish can be cooked again for extra output");
            h.assertTrue(manager.getRecipeFor(RecipeType.CAMPFIRE_COOKING,new SingleRecipeInput(raw),level).isEmpty(),"Raw batch leaked into campfire");
            h.assertTrue(manager.getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(raw),level).isEmpty(),"Raw batch leaked into generic furnace");
        }
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
        System.out.println("INTEGRITY_QA all "+CookingPreparations.ALL.size()+" hot manual recipes produce independent inedible raw batches; oven/pot/skillet finals preserve IDs and yields; cooked substitutes/campfire/furnace rejected");
        h.succeed();
    }
}
