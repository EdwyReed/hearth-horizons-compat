package dev.hearthhorizons.compat.gametest;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.culinary.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;
import net.satisfy.farm_and_charm.core.block.entity.StoveBlockEntity;
import net.satisfy.farm_and_charm.core.recipe.StoveRecipe;

@GameTestHolder(HearthHorizonsCompat.ID)
@PrefixGameTestTemplate(false)
public final class OvenServingGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeOvenPreservesMuffinNutritionAndPlatedEating(GameTestHelper h) throws Exception {
        var level=h.getLevel();var pos=new BlockPos(1,1,1);
        h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("farm_and_charm:stove")));
        var stove=(StoveBlockEntity)h.getBlockEntity(pos);
        var recipe=(StoveRecipe)level.getRecipeManager().byKey(ResourceLocation.parse("cluttered:blueberry_muffin")).orElseThrow().value();
        int expectedNutrition=0;float expectedSaturation=0;
        for(int i=0;i<recipe.getIngredients().size();i++) {
            var ingredient=recipe.getIngredients().get(i).getItems()[0].copyWithCount(1);
            var food=ingredient.get(DataComponents.FOOD);
            if(food!=null) { expectedNutrition+=food.nutrition();expectedSaturation+=food.saturation(); }
            stove.setItem(i+1,ingredient);
        }
        var craft=StoveBlockEntity.class.getDeclaredMethod("craft",StoveRecipe.class,RegistryAccess.class);
        craft.setAccessible(true);craft.invoke(stove,recipe,level.registryAccess());
        var muffin=stove.getItem(stove.getOutputSlot()).copy();
        h.assertTrue(KitchenContents.isPastry(muffin) && muffin.has(DataComponents.CONTAINER),"Native oven bypassed serving snapshot");
        var food=muffin.get(DataComponents.FOOD);
        h.assertTrue(food!=null && food.nutrition()==expectedNutrition && food.saturation()==expectedSaturation,"Native oven lost actual ingredient nutrition");
        stove.clearContent();h.setBlock(pos,Blocks.AIR);
        h.setBlock(pos,((BlockItem)muffin.getItem()).getBlock());
        ((KitchenBlockEntity)h.getBlockEntity(pos)).place(muffin);
        var drops=Block.getDrops(h.getBlockState(pos),level,h.absolutePos(pos),h.getBlockEntity(pos));
        h.assertTrue(drops.size()==1 && ItemStack.isSameItemSameComponents(drops.getFirst(),muffin),"Breaking served food lost components or duplicated food/plate");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);player.getFoodData().setSaturation(0);
        var worldPos=h.absolutePos(pos);
        KitchenInteraction.use(new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,worldPos,new BlockHitResult(Vec3.atCenterOf(worldPos),Direction.UP,worldPos,false)));
        h.assertTrue(player.getFoodData().getFoodLevel()==Math.min(20,expectedNutrition),"Placed muffin nutrition differs from item");
        h.assertTrue(h.getBlockState(pos).getBlock()==((BlockItem)KitchenContents.pastryPlate().getItem()).getBlock(),"Eating did not return exactly one placed empty plate");
        h.assertTrue(level.getBlockEntity(h.absolutePos(pos))==null,"Empty plate retained kitchen contents");
        System.out.println("OVEN_SERVING_QA native Farm & Charm craft captures muffin ingredients; item/placed nutrition agrees; break roundtrip returns exactly one packed serving; eating leaves one empty plate");
        h.succeed();
    }
}
