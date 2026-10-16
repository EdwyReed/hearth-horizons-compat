package dev.hearthhorizons.qol.gametest;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import dev.hearthhorizons.qol.culinary.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.neoforged.neoforge.gametest.*;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import vectorwing.farmersdelight.common.item.SkilletItem;

@GameTestHolder(HearthHorizonsQualityOfLife.ID)
@PrefixGameTestTemplate(false)
public final class SkilletGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void fryingIsAcceptedOnlyByRealSkillets(GameTestHelper h) {
        var level=h.getLevel();
        var pos=new BlockPos(1,1,1);
        for(var raw : PreparationItems.DEFINITIONS.stream().filter(d -> !d.recipe().equals("raisin_oatmeal_cookie")).map(PreparationItems.Definition::stack).toList()) {
            h.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING,new SingleRecipeInput(raw),level).isEmpty(),"Frying leaked into campfire recipe type");
            h.setBlock(pos,net.minecraft.world.level.block.Blocks.CAMPFIRE);
            h.assertTrue(((CampfireBlockEntity)h.getBlockEntity(pos)).getCookableRecipe(raw).isEmpty(),"Native campfire accepts frying");
            h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("farmersdelight:skillet")));
            var skillet=(SkilletBlockEntity)h.getBlockEntity(pos);
            var leftover=skillet.addItemToCook(raw.copy(),h.makeMockPlayer(GameType.SURVIVAL));
            h.assertTrue(leftover.isEmpty() && skillet.hasStoredStack(),"Placed FD skillet rejects frying");
            h.assertTrue(SkilletItem.getCookingRecipe(raw,level).isPresent(),"Handheld skillet rejects frying");
            h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        }
        System.out.println("SKILLET_QA native campfire rejects frying; placed and handheld FD skillets accept both preparations");
        h.succeed();
    }
}
