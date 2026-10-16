package dev.hearthhorizons.qol.gametest;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import dev.hearthhorizons.qol.migration.LegacyIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(HearthHorizonsQualityOfLife.ID)
@PrefixGameTestTemplate(false)
public final class MigrationGameTests {
    // Exported by 0.5.1, before renaming, using that version's actual save methods.
    private static CompoundTag fixture() throws Exception {
        try (var input = java.util.Objects.requireNonNull(MigrationGameTests.class.getResourceAsStream("/qa/legacy-0.5.1.nbt"))) {
            return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
    }

    private static Tag migrated(Tag original) {
        if (original instanceof StringTag text) {
            String value = text.getAsString();
            return StringTag.valueOf(value.startsWith(LegacyIds.NAMESPACE + ":")
                ? HearthHorizonsQualityOfLife.ID + value.substring(LegacyIds.NAMESPACE.length()) : value);
        }
        if (original instanceof CompoundTag compound) {
            var copy = new CompoundTag();
            for (String key : compound.getAllKeys()) copy.put(key, migrated(compound.get(key)));
            return copy;
        }
        if (original instanceof ListTag list) {
            var copy = new ListTag();
            for (Tag value : list) copy.add(migrated(value));
            return copy;
        }
        return original.copy();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void everyOldRegistryIdResolvesWithoutDuplicates(GameTestHelper h) {
        int checked = 0;
        for (var registry : BuiltInRegistries.REGISTRY) {
            for (var entry : registry.entrySet()) {
                var id = entry.getKey().location();
                if (!id.getNamespace().equals(HearthHorizonsQualityOfLife.ID)) continue;
                var old = ResourceLocation.fromNamespaceAndPath(LegacyIds.NAMESPACE, id.getPath());
                h.assertTrue(registry.get(old) == entry.getValue(), "Missing migration: " + registry.key() + " / " + old);
                h.assertTrue(!registry.keySet().contains(old), "Legacy ID registered a duplicate object: " + old);
                checked++;
            }
        }
        h.assertTrue(checked > 18, "Migration did not exercise all owned registries");
        System.out.println("MIGRATION_QA " + checked + " registry aliases; no duplicate legacy entries");
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void oldInventoriesAndAllSodasReadAndWriteNewIds(GameTestHelper h) throws Exception {
        var data = fixture();
        var registries = h.getLevel().registryAccess();
        var items = data.getList("items", Tag.TAG_COMPOUND);
        h.assertTrue(items.size() == 756, "Incomplete 0.5.1 inventory fixture");
        int retainedItems = 0;
        for (Tag old : items) {
            if (((CompoundTag)old).getString("id").contains(":uncooked/")) continue;
            retainedItems++;
            var stack = ItemStack.parse(registries, old).orElseThrow();
            h.assertTrue(stack.getCount() == 7, "Legacy item count changed");
            h.assertTrue(stack.has(DataComponents.CUSTOM_NAME), "Legacy custom name lost");
            h.assertTrue(stack.save(registries).equals(migrated(old)), "Legacy item/component round-trip changed: " + old);
        }
        h.assertTrue(retainedItems == 4, "Expected only the mixer and three reviewed preparations");
        var fluids = data.getList("fluids", Tag.TAG_COMPOUND);
        h.assertTrue(fluids.size() == 9, "Incomplete soda fixture");
        for (Tag old : fluids) {
            var fluid = FluidStack.parse(registries, old).orElseThrow();
            h.assertTrue(fluid.getAmount() == 625, "Legacy soda amount changed");
            h.assertTrue(fluid.save(registries).equals(migrated(old)), "Legacy soda changed: " + old);
        }
        System.out.println("MIGRATION_QA 4 retained old inventory stacks and nested containers; all 9 sodas preserved");
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void oldMixerShelvesAndKitchenPreserveTheirContents(GameTestHelper h) throws Exception {
        var registries = h.getLevel().registryAccess();
        var blocks = fixture().getList("blocks", Tag.TAG_COMPOUND);
        h.assertTrue(blocks.size() == 3, "Incomplete legacy block fixture");
        for (Tag value : blocks) {
            var row = (CompoundTag)value;
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), row.getCompound("state"));
            h.assertTrue(NbtUtils.writeBlockState(state).equals(migrated(row.get("state"))), "Old block state was lost");
            var old = row.getCompound("block_entity");
            var entity = BlockEntity.loadStatic(BlockPos.ZERO, state, old, registries);
            h.assertTrue(entity != null, "Legacy block entity failed to load: " + old.getString("id"));
            var saved = entity.saveWithFullMetadata(registries);
            h.assertTrue(saved.equals(migrated(old)), "Legacy block contents/progress changed: " + old.getString("id") + "\n" + saved + "\n" + migrated(old));
        }
        System.out.println("MIGRATION_QA old mixer (items, soda, progress), two filled Hexerei jars and kitchen container preserved");
        h.succeed();
    }
}
