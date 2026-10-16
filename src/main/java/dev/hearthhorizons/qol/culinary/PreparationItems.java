package dev.hearthhorizons.qol.culinary;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Preparations have real identities and never inherit a cooked item's FOOD or food tags. */
public final class PreparationItems {
    public record Definition(String recipe, String carrier, String raw) {
        public ResourceLocation rawId() { return ResourceLocation.fromNamespaceAndPath(HearthHorizonsQualityOfLife.ID, raw); }
        public String translationKey() { return "item." + HearthHorizonsQualityOfLife.ID + "." + raw; }
        public ItemStack stack() { return new ItemStack(ITEMS_BY_RECIPE.get(recipe).get()); }
    }
    public static final List<Definition> DEFINITIONS = List.of(
        new Definition("raisin_oatmeal_cookie", "raisin_oatmeal_cookie", "raisin_oatmeal_cookie_preparation"),
        new Definition("cashew_chicken", "cashew_chicken", "cashew_chicken_preparation"),
        new Definition("shaped_beef_stir_fry", "beef_stir_fry", "beef_stir_fry_preparation"));
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HearthHorizonsQualityOfLife.ID);
    private static final Map<String, DeferredItem<Item>> ITEMS_BY_RECIPE = new LinkedHashMap<>();
    static {
        for (Definition definition : DEFINITIONS) {
            ITEMS_BY_RECIPE.put(definition.recipe(), ITEMS.registerSimpleItem(definition.raw(), new Item.Properties()
                .component(DataComponents.ITEM_NAME, Component.translatable(definition.translationKey()).withStyle(ChatFormatting.GOLD))
                .component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable(definition.translationKey() + ".stage").withStyle(ChatFormatting.GRAY),
                    Component.translatable("item.hearth_horizons_qol.preparation.not_food").withStyle(ChatFormatting.DARK_GRAY))))));
        }
    }
    public static void register(IEventBus bus) {
        dev.hearthhorizons.qol.migration.LegacyIds.register(bus, ITEMS);
        bus.addListener(PreparationItems::creative);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS))
            DEFINITIONS.forEach(definition -> event.accept(definition.stack()));
    }
    public static Definition definition(String recipe) {
        return DEFINITIONS.stream().filter(value -> value.recipe().equals(recipe)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown preparation: " + recipe));
    }
    private PreparationItems() {}
}
