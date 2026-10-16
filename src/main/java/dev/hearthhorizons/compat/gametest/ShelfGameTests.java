package dev.hearthhorizons.compat.gametest;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.placement.*;
import net.joefoxe.hexerei.items.JarHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HearthHorizonsCompat.ID)
@PrefixGameTestTemplate(false)
public final class ShelfGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static BlockState shelf(boolean pink, Direction facing) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("cluttered:small_shelf" + (pink ? "_pink" : "")))
            .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }
    private static ItemStack jar(GameTestHelper h, int count, int colour) {
        ItemStack jar = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("hexerei:herb_jar")));
        JarHandler inventory = new JarHandler(1, 1024);
        inventory.setStackInSlot(0, new ItemStack(Items.DANDELION, count));
        var data = new net.minecraft.nbt.CompoundTag();
        data.put("Inventory", inventory.serializeNBT(h.getLevel().registryAccess()));
        data.putInt("ButtonToggled", 1);
        data.putString("UnrelatedData", "preserve-me");
        jar.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        jar.set(DataComponents.CUSTOM_NAME, Component.literal("Jar " + count));
        jar.set(DataComponents.DYED_COLOR, new DyedItemColor(colour, false));
        return jar;
    }
    private static FakePlayer player(GameTestHelper h) {
        FakePlayer p = FakePlayerFactory.getMinecraft(h.getLevel());
        p.getAbilities().mayBuild = true;
        p.getAbilities().instabuild = false;
        return p;
    }
    private static void click(GameTestHelper h, FakePlayer p, ItemStack held, Vec3 target, Direction facing) {
        BlockPos pos = h.absolutePos(POS);
        Vec3 world = Vec3.atLowerCornerOf(pos).add(target);
        p.setPos(world.x + facing.getStepX() * 2, world.y - p.getEyeHeight(), world.z + facing.getStepZ() * 2);
        p.setYRot(facing.getOpposite().toYRot());
        p.setXRot(0);
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        ShelfInteraction.use(h.getLevel(), pos, h.getBlockState(POS), p, InteractionHand.MAIN_HAND,
            new BlockHitResult(world, facing, pos, false));
    }
    @GameTest(template = "empty")
    public static void emptyShelvesHaveNoBlockEntity(GameTestHelper h) {
        for (boolean pink : new boolean[]{false, true}) for (Direction facing : Direction.Plane.HORIZONTAL) {
            h.setBlock(POS, shelf(pink, facing));
            h.assertTrue(!h.getBlockState(POS).hasBlockEntity(), "Empty state must report no BE");
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(POS)) == null, "Lookup must not allocate BE");
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(POS)) == null, "Repeated lookup must remain empty");
            h.assertTrue(h.getBlockState(POS).getCollisionShape(h.getLevel(), h.absolutePos(POS)).bounds().maxY == 5.0 / 16, "Empty shelf collision must move down with model");
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void rightClickRoundTripAndNoThirdJar(GameTestHelper h) {
        h.setBlock(POS, shelf(false, Direction.NORTH));
        h.setBlock(POS.above(), Blocks.STONE);
        h.setBlock(POS.below(), Blocks.STONE);
        FakePlayer p = player(h);
        ItemStack first = jar(h, 1024, 0x447722), second = jar(h, 65, 0x8833aa);
        click(h, p, first.copy(), new Vec3(.5, .3, .8), Direction.NORTH);
        h.assertTrue(p.getMainHandItem().isEmpty(), "First jar consumed once");
        ShelfBlockEntity be = (ShelfBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(POS));
        double singleScale = be.poses().getFirst().scale();
        click(h, p, second.copy(), new Vec3(.5, .3, .8), Direction.NORTH);
        h.assertTrue(be.size() == 2 && be.poses().getFirst().scale() < singleScale, "Two jars must shrink");
        click(h, p, first.copy(), new Vec3(.5, .3, .8), Direction.NORTH);
        h.assertTrue(ItemStack.matches(p.getMainHandItem(), first) && be.size() == 2, "Third jar must not be consumed");
        h.assertTrue(ItemStack.matches(first, be.item(0)) && ItemStack.matches(second, be.item(1)), "All components must survive insertion");
        Vec3 target = be.poses().get(1).bounds().getCenter();
        click(h, p, ItemStack.EMPTY, target, Direction.NORTH);
        h.assertTrue(ItemStack.matches(second, p.getMainHandItem()), "Must retrieve selected right jar with 65 items");
        h.assertTrue(be.size() == 1 && be.poses().getFirst().scale() == singleScale, "Remaining jar must grow");
        click(h, p, ItemStack.EMPTY, be.poses().getFirst().bounds().getCenter(), Direction.NORTH);
        h.assertTrue(ItemStack.matches(first, p.getMainHandItem()), "1024 contents and metadata must survive round trip");
        h.assertTrue(!h.getBlockState(POS).hasBlockEntity() && h.getLevel().getBlockEntity(h.absolutePos(POS)) == null, "Last removal must destroy BE");
        h.assertBlockPresent(Blocks.STONE, POS.above());
        h.assertBlockPresent(Blocks.STONE, POS.below());
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void serializationAndOneCellBounds(GameTestHelper h) {
        for (boolean pink : new boolean[]{false, true}) for (Direction facing : Direction.Plane.HORIZONTAL) {
            h.setBlock(POS, shelf(pink, facing).setValue(PlacementRules.COUNT, 2));
            ShelfBlockEntity be = (ShelfBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(POS));
            ItemStack first = jar(h, 1024, 0x113355), second = jar(h, 64, 0x557799);
            h.assertTrue(be.insert(first) && be.insert(second), "Insert two jars");
            var saved = be.saveWithoutMetadata(h.getLevel().registryAccess());
            ShelfBlockEntity restored = new ShelfBlockEntity(h.absolutePos(POS), h.getBlockState(POS));
            restored.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(ItemStack.matches(first, restored.item(0)) && ItemStack.matches(second, restored.item(1)), "Registry-aware NBT roundtrip must be lossless");
            for (var pose : restored.poses()) {
                var bounds = PlacementRules.rotate(pose.bounds(), facing);
                h.assertTrue(bounds.minX >= 0 && bounds.minY >= 0 && bounds.minZ >= 0 && bounds.maxX <= 1 && bounds.maxY <= 1 && bounds.maxZ <= 1, "Item must fit its own cell");
            }
            var collision = h.getBlockState(POS).getCollisionShape(h.getLevel(), h.absolutePos(POS)).bounds();
            h.assertTrue(collision.minY == 0 && collision.maxY <= 1, "Lowered collision stays within cell");
            h.assertTrue(collision.maxY > 5.0 / 16, "Collision must include displayed objects, not stale empty-state cache");
            h.assertTrue(h.getBlockState(POS).getTicker(h.getLevel(), HearthHorizonsCompat.SHELF.get()) == null, "Shelf must not tick");
            h.setBlock(POS, Blocks.AIR);
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void rightClickWorksForEveryFacing(GameTestHelper h) {
        FakePlayer p = player(h);
        for (boolean pink : new boolean[]{false, true}) for (Direction facing : Direction.Plane.HORIZONTAL) {
            h.setBlock(POS, shelf(pink, facing));
            ItemStack original = jar(h, 65, 0x123456);
            click(h, p, original.copy(), new Vec3(.5, .3, .8), facing);
            ShelfBlockEntity be = (ShelfBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(POS));
            h.assertTrue(be != null && be.size() == 1, "Insertion must work at " + facing);
            click(h, p, ItemStack.EMPTY, PlacementRules.rotate(be.poses().getFirst().bounds().getCenter(), facing), facing);
            h.assertTrue(ItemStack.matches(original, p.getMainHandItem()), "Targeted retrieval must work at " + facing);
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(POS)) == null, "No leftover BE at " + facing);
        }
        h.succeed();
    }
    @GameTest(template = "stress", timeoutTicks = 2000)
    public static void thousandShelvesHaveNoTickersAndReleaseStorage(GameTestHelper h) {
        long started = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BlockPos pos = new BlockPos(i % 40, 1, i / 40);
            h.setBlock(pos, shelf(false, Direction.NORTH).setValue(PlacementRules.COUNT, 2));
            ShelfBlockEntity be = (ShelfBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
            be.insert(jar(h, 1024, 0x123456)); be.insert(jar(h, 65, 0x654321));
            h.assertTrue(h.getBlockState(pos).getTicker(h.getLevel(), HearthHorizonsCompat.SHELF.get()) == null, "No ticker per occupied shelf");
        }
        for (int i = 0; i < 1000; i++) {
            BlockPos pos = new BlockPos(i % 40, 1, i / 40);
            ShelfBlockEntity be = (ShelfBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
            h.assertTrue(be.size() == 2 && be.item(0).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("Inventory").getList("Items", 10).getCompound(0).getInt("ExtendedCount") == 1024, "Bulk storage must retain counts");
            be.clearForDrop();
            h.setBlock(pos, shelf(false, Direction.NORTH));
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(pos)) == null, "Storage must be released");
        }
        System.out.println("SHELF_BULK_TEST 1000 shelves / 2000 jars setup+check+clear ms=" + (System.nanoTime() - started) / 1_000_000);
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void retrievedJarStillPlacesAsNativeHexerei(GameTestHelper h) {
        h.setBlock(POS, shelf(false, Direction.NORTH));
        FakePlayer p = player(h);
        ItemStack original = jar(h, 1024, 0x227799);
        click(h, p, original.copy(), new Vec3(.5, .3, .8), Direction.NORTH);
        ShelfBlockEntity be = (ShelfBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(POS));
        click(h, p, ItemStack.EMPTY, be.poses().getFirst().bounds().getCenter(), Direction.NORTH);
        BlockPos support = POS.offset(1, 0, 0);
        h.setBlock(support, Blocks.STONE);
        BlockPos absolute = h.absolutePos(support);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(absolute).add(.5, 1, .5), Direction.UP, absolute, false);
        var context = new net.minecraft.world.item.context.BlockPlaceContext(p, InteractionHand.MAIN_HAND, p.getMainHandItem(), hit);
        var result = ((net.joefoxe.hexerei.item.custom.HerbJarItem) p.getMainHandItem().getItem()).place(context);
        h.assertTrue(result.consumesAction(), "Retrieved jar must place through native Hexerei item");
        var placed = h.getLevel().getBlockEntity(absolute.above());
        h.assertTrue(placed instanceof net.joefoxe.hexerei.tileentity.HerbJarTile, "Placement must create actual native jar BE");
        var nativeJar = (net.joefoxe.hexerei.tileentity.HerbJarTile) placed;
        h.assertTrue(nativeJar.itemHandler.getStackInSlot(0).getCount() == 1024, "Native jar must restore 1024 contents");
        h.assertTrue(nativeJar.customName.getString().equals("Jar 1024") && nativeJar.getDyeColor() == 0x227799 && nativeJar.buttonToggled == 1,
            "Native jar must restore name, dye and crow setting");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void destructionDropsWholeJarsOnce(GameTestHelper h) {
        h.setBlock(POS, shelf(false, Direction.NORTH).setValue(PlacementRules.COUNT, 2));
        ShelfBlockEntity be = (ShelfBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(POS));
        ItemStack first = jar(h, 1024, 0x113355), second = jar(h, 65, 0x557799);
        be.insert(first); be.insert(second);
        h.setBlock(POS, Blocks.AIR);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(POS)).inflate(1));
        h.assertTrue(drops.size() == 2, "Exactly two jar drops required");
        h.assertTrue(drops.stream().anyMatch(e -> ItemStack.matches(e.getItem(), first)) && drops.stream().anyMatch(e -> ItemStack.matches(e.getItem(), second)), "Dropped jars must preserve contents");
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(POS)) == null, "Destroyed shelf must have no BE");
        h.succeed();
    }
}
