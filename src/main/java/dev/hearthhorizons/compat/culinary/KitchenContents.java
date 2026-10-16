package dev.hearthhorizons.compat.culinary;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.redchujelly.cluttered.block.custom.food.PastryBlock;
import java.util.*;

public final class KitchenContents {
    public static final IntegerProperty PORTIONS = IntegerProperty.create("hh_portions", 0, 4);
    public static String id(Block block) { return BuiltInRegistries.BLOCK.getKey(block).toString(); }
    // Constructors run before registry IDs are assigned; use the actual classes.
    public static boolean isBread(Block block) { return block instanceof net.redchujelly.cluttered.block.custom.furniture.SlicedBreadBlock; }
    public static boolean isKit(Block block) { return block instanceof net.redchujelly.cluttered.block.custom.furniture.BakingIngredientsBlock; }
    public static boolean portions(Block block) { return isBread(block) || isKit(block); }
    public static boolean supported(Block block) { return block instanceof PastryBlock || portions(block); }
    public static boolean isPastry(ItemStack stack) { return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof PastryBlock; }
    public static ItemStack pastryPlate() { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cluttered:pastry_plate"))); }
    public static List<ItemStack> unpack(ItemStack stack) {
        var list = new ArrayList<ItemStack>();
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems().forEach(s -> list.add(s.copy()));
        return list;
    }
    public static ItemStack capture(ItemStack output, Iterable<ItemStack> ingredients) {
        var contents = new ArrayList<ItemStack>();
        int nutrition = 0; float saturation = 0;
        var effects = new ArrayList<net.minecraft.world.food.FoodProperties.PossibleEffect>();
        ItemStack plate = pastryPlate();
        for (var stack : ingredients) {
            if (stack.isEmpty()) continue;
            contents.add(stack.copyWithCount(1));
            if (stack.is(plate.getItem())) plate = stack.copyWithCount(1);
            var food = stack.get(DataComponents.FOOD);
            if (food != null) { nutrition += food.nutrition(); saturation += food.saturation(); effects.addAll(food.effects()); }
        }
        output.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        if (isPastry(output) && nutrition > 0) output.set(DataComponents.FOOD,
            new net.minecraft.world.food.FoodProperties(nutrition, saturation, false, 1.6F, Optional.of(plate), List.copyOf(effects)));
        return output;
    }
    public static boolean vessel(ItemStack stack) { return stack.is(Items.BOWL) || stack.is(Items.HEAVY_WEIGHTED_PRESSURE_PLATE); }
    private KitchenContents() {}
}
