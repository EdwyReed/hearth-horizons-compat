package dev.hearthhorizons.qol;

import dev.hearthhorizons.qol.placement.ShelfBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

@Mod(HearthHorizonsQualityOfLife.ID)
public final class HearthHorizonsQualityOfLife {
    public static final String ID = "hearth_horizons_qol";
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShelfBlockEntity>> SHELF = TYPES.register("placed_items", () ->
        BlockEntityType.Builder.of(ShelfBlockEntity::new,
            BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("cluttered", "small_shelf")),
            BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("cluttered", "small_shelf_pink"))).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.hearthhorizons.qol.culinary.KitchenBlockEntity>> KITCHEN = TYPES.register("kitchen_contents", () ->
        BlockEntityType.Builder.of(dev.hearthhorizons.qol.culinary.KitchenBlockEntity::new,
            BuiltInRegistries.BLOCK.stream().filter(dev.hearthhorizons.qol.culinary.KitchenContents::supported).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public HearthHorizonsQualityOfLife(IEventBus bus) {
        dev.hearthhorizons.qol.migration.LegacyIds.register(bus, TYPES);
        dev.hearthhorizons.qol.mixer.ManualMixer.register(bus);
        dev.hearthhorizons.qol.culinary.PreparationItems.register(bus);
        dev.hearthhorizons.qol.culinary.SodaFluids.register(bus);
        dev.hearthhorizons.qol.culinary.SodaParticles.register(bus);
        dev.hearthhorizons.qol.culinary.ServingRecipe.register(bus);
        dev.hearthhorizons.qol.culinary.SkilletCookingRecipe.register(bus);
        dev.hearthhorizons.qol.culinary.CookingPreparations.register(bus);
        dev.hearthhorizons.qol.culinary.ColdPreparationRecipe.register(bus);
        bus.addListener(HearthHorizonsQualityOfLife::addCompatibilityPack);
    }

    private static void addCompatibilityPack(AddPackFindersEvent event) {
        // A required, high-priority embedded pack preserves the original datapack overrides.
        // Register it for both server data and client assets using the same versioned contents.
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(ID, "packs/culinary"),
            event.getPackType(), Component.literal("Hearth & Horizons: Quality of Life — Culinary"),
            PackSource.BUILT_IN, true, Pack.Position.TOP);
    }
}
