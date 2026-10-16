package dev.hearthhorizons.compat.mixer;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

public final class ManualMixer {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HearthHorizonsCompat.ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HearthHorizonsCompat.ID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HearthHorizonsCompat.ID);
    public static final DeferredBlock<ManualMixerBlock> BLOCK = BLOCKS.register("manual_mixer", () ->
        new ManualMixerBlock(BlockBehaviour.Properties.of().strength(2.5f).noOcclusion().sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final DeferredItem<BlockItem> ITEM = ITEMS.registerSimpleBlockItem(BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManualMixerBlockEntity>> TYPE = TYPES.register("manual_mixer", () ->
        BlockEntityType.Builder.of(ManualMixerBlockEntity::new, BLOCK.get()).build(null));
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); TYPES.register(bus);
        bus.addListener(ManualMixer::capabilities);
        bus.addListener(ManualMixer::creative);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TYPE.get(), (be, side) -> be.items());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TYPE.get(), (be, side) -> be.fluids());
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) event.accept(ITEM.get());
    }
    private ManualMixer() {}
}
