package dev.hearthhorizons.compat.gametest;

import com.google.gson.JsonParser;
import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.culinary.PreparationItems;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HearthHorizonsCompat.ID)
@PrefixGameTestTemplate(false)
public final class CulinaryGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void embeddedPackOverridesAndPreparationRecipes(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var resources = server.getResourceManager();
        try (var input = CulinaryGameTests.class.getResourceAsStream("/qa/culinary-baseline.json")) {
            var baseline = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            int checked = 0;
            int activeRecipes = 0;
            int disabledRecipes = 0;
            for (var entry : baseline.entrySet()) {
                var parts = entry.getKey().substring("data/".length()).split("/", 2);
                var id = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
                var resource = resources.getResource(id).orElseThrow();
                helper.assertTrue(resource.sourcePackId().equals("mod/hearth_horizons_compat:packs/culinary"),
                    "Compatibility override lost priority: " + id + " from " + resource.sourcePackId());
                try (var stream = resource.open()) {
                    var bytes = stream.readAllBytes();
                    var hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                    helper.assertTrue(hash.equals(entry.getValue().getAsString()), "Changed data: " + id);
                    if (parts[1].startsWith("recipe/") && ModList.get().isLoaded("croptopia")) {
                        var recipeId = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1].substring(7, parts[1].length() - 5));
                        var json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                        if (!json.has("neoforge:conditions")) {
                            helper.assertTrue(server.getRecipeManager().byKey(recipeId).isPresent(), "Recipe did not decode: " + recipeId);
                            activeRecipes++;
                        } else if (json.getAsJsonArray("neoforge:conditions").toString().contains("neoforge:false")) {
                            helper.assertTrue(server.getRecipeManager().byKey(recipeId).isEmpty(), "Disabled recipe returned: " + recipeId);
                            disabledRecipes++;
                        }
                    }
                }
                checked++;
            }
            helper.assertTrue(checked == baseline.size(), "Incomplete resource baseline");
            System.out.println("CULINARY_QA_RESOURCES " + checked + " migration-manifest resources loaded at highest priority");
            System.out.println("CULINARY_QA_LOADED " + activeRecipes + " unconditional recipes; " + disabledRecipes + " intentionally disabled recipes absent");
        }
        if (ModList.get().isLoaded("croptopia") && ModList.get().isLoaded("create") && ModList.get().isLoaded("farm_and_charm")) {
            for (String dish : new String[] {"raisin_oatmeal_cookie", "cashew_chicken", "shaped_beef_stir_fry"}) {
                var preparation = server.getRecipeManager().byKey(ResourceLocation.parse("croptopia:" + dish)).orElseThrow();
                var raw = preparation.value().getResultItem(server.registryAccess());
                helper.assertTrue(!raw.has(DataComponents.FOOD), "Raw preparation regained FOOD: " + dish);
                var definition = PreparationItems.definition(dish);
                helper.assertTrue(raw.is(definition.stack().getItem()), "Preparation still uses a cooked carrier: " + dish);
                var cookedId = "hearth_horizons:" + (dish.equals("raisin_oatmeal_cookie") ? "baking/" : "frying/") + dish;
                var cookingRecipe = server.getRecipeManager().byKey(ResourceLocation.parse(cookedId)).orElseThrow().value();
                var cooked = cookingRecipe.getResultItem(server.registryAccess());
                helper.assertTrue(cooked.has(DataComponents.FOOD), "Cooked dish lost FOOD: " + dish);
                helper.assertTrue(cooked.getCount() == (dish.equals("raisin_oatmeal_cookie") ? 4 : 1), "Changed cooking yield");
                helper.assertTrue(cookingRecipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(raw)),
                    "Cooking recipe rejects its actual raw preparation: " + dish);
                helper.assertTrue(cookingRecipe.getIngredients().stream().noneMatch(ingredient -> ingredient.test(cooked)),
                    "Cooking recipe accepts a cooked dish as a raw preparation: " + dish);
                helper.assertTrue(server.getRecipeManager().byKey(ResourceLocation.parse("hearth_horizons:preparation_migration/" + dish)).isEmpty(),
                    "Unused legacy migration recipe is still loaded: " + dish);
            }
            System.out.println("CULINARY_QA_RECIPES 3 independent raw/cooked stages decoded; FOOD and 4/1/1 yields retained; no legacy migration recipes");
        }

        helper.succeed();
    }
}
