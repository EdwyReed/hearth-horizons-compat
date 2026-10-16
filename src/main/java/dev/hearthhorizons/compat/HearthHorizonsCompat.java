package dev.hearthhorizons.compat;

import dev.hearthhorizons.compat.placement.ShelfBlockEntity;
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

@Mod(HearthHorizonsCompat.ID)
public final class HearthHorizonsCompat {
    public static final String ID = "hearth_horizons_compat";
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShelfBlockEntity>> SHELF = TYPES.register("placed_items", () ->
        BlockEntityType.Builder.of(ShelfBlockEntity::new,
            BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("cluttered", "small_shelf")),
            BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("cluttered", "small_shelf_pink"))).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.hearthhorizons.compat.culinary.KitchenBlockEntity>> KITCHEN = TYPES.register("kitchen_contents", () ->
        BlockEntityType.Builder.of(dev.hearthhorizons.compat.culinary.KitchenBlockEntity::new,
            BuiltInRegistries.BLOCK.stream().filter(dev.hearthhorizons.compat.culinary.KitchenContents::supported).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public HearthHorizonsCompat(IEventBus bus) {
        TYPES.register(bus);
        dev.hearthhorizons.compat.mixer.ManualMixer.register(bus);
        dev.hearthhorizons.compat.culinary.PreparationItems.register(bus);
        dev.hearthhorizons.compat.culinary.SodaFluids.register(bus);
        dev.hearthhorizons.compat.culinary.SodaParticles.register(bus);
        dev.hearthhorizons.compat.culinary.ServingRecipe.register(bus);
        dev.hearthhorizons.compat.culinary.SkilletCookingRecipe.register(bus);
        dev.hearthhorizons.compat.culinary.CookingPreparations.register(bus);
        dev.hearthhorizons.compat.culinary.ColdPreparationRecipe.register(bus);
        bus.addListener(HearthHorizonsCompat::addCompatibilityPack);
    }

    private static void addCompatibilityPack(AddPackFindersEvent event) {
        // A required, high-priority embedded pack preserves the original datapack overrides.
        // Register it for both server data and client assets using the same versioned contents.
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(ID, "packs/culinary"),
            event.getPackType(), Component.literal("Hearth & Horizons Quality of Life — Culinary"),
            PackSource.BUILT_IN, true, Pack.Position.TOP);
    }
}
