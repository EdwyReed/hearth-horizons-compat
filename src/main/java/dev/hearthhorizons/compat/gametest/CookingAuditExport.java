package dev.hearthhorizons.compat.gametest;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import java.nio.file.*;

/** Development-only evidence: all runtime recipes and all food/remainder items.
 * Includes generated recipes and resolved tags. Never included in the release JAR.
 */
public final class CookingAuditExport {
    public static void write(MinecraftServer server) throws Exception {
        var output = new JsonObject();
        var items = new JsonArray();
        for (var item : BuiltInRegistries.ITEM) {
            var stack = new ItemStack(item);
            var food = stack.get(DataComponents.FOOD);
            var remainder = stack.getCraftingRemainingItem();
            if (food == null && remainder.isEmpty()) continue;
            var row = new JsonObject();
            row.addProperty("id", BuiltInRegistries.ITEM.getKey(item).toString());
            if (food != null) {
                row.addProperty("nutrition", food.nutrition()); row.addProperty("saturation", food.saturation());
                row.addProperty("eating_container", food.usingConvertsTo().map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()).orElse(""));
            }
            row.addProperty("crafting_remainder", remainder.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(remainder.getItem()).toString());
            items.add(row);
        }
        var recipes = new JsonArray();
        for (var holder : server.getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            var row = new JsonObject();
            row.addProperty("id", holder.id().toString());
            row.addProperty("type", BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString());
            row.addProperty("serializer", BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()).toString());
            ItemStack result;
            try { result = recipe.getResultItem(server.registryAccess()); }
            catch (RuntimeException ex) { row.addProperty("result_exception", ex.toString()); result = ItemStack.EMPTY; }
            row.addProperty("result", result.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
            row.addProperty("count", result.getCount());
            row.addProperty("food", result.has(DataComponents.FOOD));
            try {
                row.add("encoded", net.minecraft.world.item.crafting.Recipe.CODEC.encodeStart(
                    server.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),recipe).getOrThrow());
            } catch (RuntimeException ex) { row.addProperty("encoding_exception",ex.toString()); }
            var resourceId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(holder.id().getNamespace(),"recipe/"+holder.id().getPath()+".json");
            server.getResourceManager().getResource(resourceId).ifPresent(resource -> row.addProperty("source_pack",resource.sourcePackId()));
            var ingredients = new JsonArray();
            for (var ingredient : recipe.getIngredients()) {
                var alternatives = new JsonArray();
                for (var candidate : ingredient.getItems()) alternatives.add(BuiltInRegistries.ITEM.getKey(candidate.getItem()).toString());
                ingredients.add(alternatives);
            }
            row.add("ingredients", ingredients);
            recipes.add(row);
        }
        output.add("items", items); output.add("recipes", recipes);
        Path path = Path.of("../../validation/cooking-audit/runtime.json").toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(output));
        System.out.println("COOKING_AUDIT_RUNTIME " + recipes.size() + " active recipes; " + items.size() + " food/remainder items exported to " + path);
    }
}
