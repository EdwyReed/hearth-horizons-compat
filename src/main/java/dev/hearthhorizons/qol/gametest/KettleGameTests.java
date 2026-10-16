package dev.hearthhorizons.qol.gametest;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import net.favouriteless.enchanted.common.blocks.KettleBlock;
import net.favouriteless.enchanted.common.blocks.entity.KettleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HearthHorizonsQualityOfLife.ID)
@PrefixGameTestTemplate(false)
public final class KettleGameTests {
    private static final BlockPos POS = new BlockPos(2, 3, 2);
    private static KettleBlock kettle() {
        return (KettleBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse("enchanted:kettle"));
    }
    private static void clear(GameTestHelper h) {
        h.setBlock(POS, Blocks.AIR);
        for (Direction side : Direction.values()) h.setBlock(POS.relative(side), Blocks.AIR);
    }
    private static BlockState placement(GameTestHelper h, Direction face, Direction playerFacing) {
        var support = h.absolutePos(POS.relative(face.getOpposite()));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(playerFacing.toYRot());
        var context = new BlockPlaceContext(h.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(kettle()),
            new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, support, false));
        h.assertTrue(context.getClickedPos().equals(h.absolutePos(POS)), "Placement fixture targeted wrong block");
        return kettle().getStateForPlacement(context);
    }
    private static KettleBlock.Type expected(Direction face) {
        return face == Direction.UP ? KettleBlock.Type.GROUND
            : face == Direction.DOWN ? KettleBlock.Type.HANGING : KettleBlock.Type.HANGING_BEAM;
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void clickedFaceSelectsAllThreeModelsForPartialSupports(GameTestHelper h) {
        int checked = 0;
        for (var support : new net.minecraft.world.level.block.Block[] {Blocks.STONE, Blocks.COBBLESTONE_WALL,
                Blocks.OAK_FENCE, Blocks.CHAIN, Blocks.IRON_BARS, Blocks.STONE_SLAB, Blocks.OAK_TRAPDOOR}) {
            for (Direction face : Direction.values()) {
                for (Direction playerFacing : Direction.Plane.HORIZONTAL) {
                    clear(h);
                    h.setBlock(POS.relative(face.getOpposite()), support);
                    var state = placement(h, face, playerFacing);
                    h.assertTrue(state.getValue(KettleBlock.TYPE) == expected(face), "Wrong mount for " + support + " / " + face);
                    if (face.getAxis().isHorizontal()) {
                        h.assertTrue(state.getValue(KettleBlock.FACING).getAxis() != face.getAxis(), "Beam misses clicked side support");
                    }
                    checked++;
                }
            }
        }
        System.out.println("KETTLE_QA " + checked + " native placements: single wall/fence/chain/bars/slab/trapdoor/full block, six clicked faces, four player facings");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void clickedMountWinsWhenAllSupportsExistAndSurvivesNeighborChanges(GameTestHelper h) {
        for (Direction clickedFace : Direction.values()) {
            clear(h);
            for (Direction side : Direction.values()) h.setBlock(POS.relative(side), Blocks.STONE);
            var state = placement(h, clickedFace, Direction.NORTH);
            h.assertTrue(state.getValue(KettleBlock.TYPE) == expected(clickedFace), "Other supports overrode clicked mounting face");
            h.setBlock(POS, state);
            // Replace every support with a partial shape, retaining the player's chosen mount.
            for (Direction side : Direction.values()) {
                h.setBlock(POS.relative(side), Blocks.OAK_FENCE);
                h.assertTrue(h.getBlockState(POS).getValue(KettleBlock.TYPE) == expected(clickedFace), "Neighbor change overrode a still-supported mount");
            }
        }
        System.out.println("KETTLE_QA clicked floor/side/ceiling wins with all supports present; unrelated neighbor updates preserve choice");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void supportChangesReattachWithoutLosingContentsAndAirStaysOrdinary(GameTestHelper h) {
        clear(h);
        var level = h.getLevel();
        h.setBlock(POS, kettle().getKettleState(level, h.absolutePos(POS), Direction.NORTH));
        h.assertTrue(h.getBlockState(POS).getValue(KettleBlock.TYPE) == KettleBlock.Type.GROUND, "Unsupported air kettle should be ordinary");
        var entity = (KettleBlockEntity) h.getBlockEntity(POS);
        entity.setFluidAmount(1000);
        entity.setResult(new ItemStack(Items.APPLE, 2));
        var before = entity.saveWithoutMetadata(level.registryAccess());
        BlockPos[] neighbors = {POS.east(), POS.above(), POS.below(), POS.east(), POS.below(), POS.above()};
        var changes = new net.minecraft.world.level.block.Block[] {Blocks.OAK_FENCE, Blocks.CHAIN, Blocks.STONE, Blocks.AIR, Blocks.AIR, Blocks.AIR};
        var types = new KettleBlock.Type[] {KettleBlock.Type.HANGING_BEAM, KettleBlock.Type.HANGING_BEAM,
            KettleBlock.Type.HANGING_BEAM, KettleBlock.Type.GROUND, KettleBlock.Type.HANGING, KettleBlock.Type.GROUND};
        for (int i = 0; i < neighbors.length; i++) {
            h.setBlock(neighbors[i], changes[i]);
            h.assertTrue(h.getBlockState(POS).getValue(KettleBlock.TYPE) == types[i], "Mount failed to follow support removal/addition at step " + i);
            h.assertTrue(h.getBlockEntity(POS) == entity, "Model change replaced kettle entity");
            h.assertTrue(before.equals(entity.saveWithoutMetadata(level.registryAccess())), "Model change lost kettle water/result/state");
        }
        h.setBlock(POS.above(), Blocks.WATER);
        h.setBlock(POS.east(), Blocks.FIRE);
        h.assertTrue(h.getBlockState(POS).getValue(KettleBlock.TYPE) == KettleBlock.Type.GROUND, "Fluid/fire must not become a physical support");
        System.out.println("KETTLE_QA ordinary in air; side/ceiling/floor transitions preserve native block entity and contents; water/fire ignored");
        h.succeed();
    }
}
