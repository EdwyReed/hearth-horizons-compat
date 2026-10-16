package dev.hearthhorizons.compat.gametest;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.culinary.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

@GameTestHolder(HearthHorizonsCompat.ID)
@PrefixGameTestTemplate(false)
public final class SodaGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nineFlavoursAndRealBasinBottleLedger(GameTestHelper h) {
        var server = h.getLevel().getServer();
        for (var flavour : SodaFluids.ALL) {
            String name = flavour.name();
            h.assertTrue(server.getRecipeManager().byKey(ResourceLocation.parse("pamhc2foodextended:" + name + "sodaitem")).isEmpty(), "Manual soda bypass: " + name);
            var mixing = (ProcessingRecipe<?, ?>) server.getRecipeManager().byKey(ResourceLocation.parse("hearth_horizons_compat:soda/mixing/" + name)).orElseThrow().value();
            var filling = (ProcessingRecipe<?, ?>) server.getRecipeManager().byKey(ResourceLocation.parse("hearth_horizons_compat:soda/filling/" + name)).orElseThrow().value();
            var emptying = (ProcessingRecipe<?, ?>) server.getRecipeManager().byKey(ResourceLocation.parse("hearth_horizons_compat:soda/emptying/" + name)).orElseThrow().value();
            h.assertTrue(mixing.getFluidResults().size() == 1 && mixing.getFluidResults().getFirst().is(flavour.fluid().get()) && mixing.getFluidResults().getFirst().getAmount() == 250, "Mixer output volume/flavour");
            h.assertTrue(filling.getResultItem(server.registryAccess()).is(flavour.drink()), "Wrong bottled flavour");
            h.assertTrue(emptying.getFluidResults().getFirst().is(flavour.fluid().get()) && emptying.getFluidResults().getFirst().getAmount() == 250, "Emptying changes volume/flavour");
            var food = new ItemStack(flavour.drink()).get(DataComponents.FOOD);
            h.assertTrue(food.usingConvertsTo().orElseThrow().is(Items.GLASS_BOTTLE), "Drinking loses vessel");
            var drinker = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            drinker.getFoodData().setFoodLevel(0);
            var consumed = new ItemStack(flavour.drink()).finishUsingItem(h.getLevel(),drinker);
            h.assertTrue(consumed.is(Items.GLASS_BOTTLE) && consumed.getCount()==1,"Native drinking did not return exactly one bottle");
            h.assertTrue(drinker.getInventory().countItem(Items.GLASS_BOTTLE)==0,"Native drinking duplicated its bottle into inventory");
            var pos = new BlockPos(1,1,1);
            h.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:basin")));
            var worldPos = h.absolutePos(pos);
            var tank = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, worldPos, null);
            h.assertTrue(tank != null, "Basin has no fluid capability");
            tank.fill(new FluidStack(flavour.fluid().get(), 500), IFluidHandler.FluidAction.EXECUTE);
            var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
            var hit = new BlockHitResult(Vec3.atCenterOf(worldPos), Direction.UP, worldPos, false);
            var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, worldPos, hit);
            SodaInteraction.fillBottle(event);
            h.assertTrue(event.isCanceled(), "Custom bottle interaction did not claim soda");
            h.assertTrue(player.getMainHandItem().getCount() == 1 && player.getMainHandItem().is(Items.GLASS_BOTTLE), "Wrong bottle decrement");
            h.assertTrue(player.getInventory().countItem(flavour.drink()) == 1, "Wrong drink/flavour count");
            var remaining = tank.drain(new FluidStack(flavour.fluid().get(), 1000), IFluidHandler.FluidAction.SIMULATE);
            h.assertTrue(remaining.getAmount() == 250, "Filling consumed wrong volume");
            h.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        }
        System.out.println("SODA_QA all nine flavours decoded; no manual crafting; 250mB mix/fill/empty; native Create basin right-click consumes one bottle and 250mB");
        h.succeed();
    }
}
