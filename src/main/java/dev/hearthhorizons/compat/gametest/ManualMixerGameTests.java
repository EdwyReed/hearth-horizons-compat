package dev.hearthhorizons.compat.gametest;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.mixer.*;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.*;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder(HearthHorizonsCompat.ID)
@PrefixGameTestTemplate(false)
public final class ManualMixerGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static ManualMixerBlockEntity place(GameTestHelper h){h.setBlock(POS,ManualMixer.BLOCK.get());return (ManualMixerBlockEntity)h.getBlockEntity(POS);}
    private static void fill(GameTestHelper h,BasinBlockEntity be,MixingRecipe recipe){
        be.clearContent();
        // Native Basin.clearContent() clears items/filter, not its fluid tanks.
        var fluids=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,be.getBlockPos(),null);
        for(int i=0;i<fluids.getTanks();i++)fluids.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.EXECUTE);
        for(var ingredient:recipe.getIngredients()){
            if(ingredient.getItems().length>0)ItemHandlerHelper.insertItemStacked(be.getInputInventory(),ingredient.getItems()[0].copyWithCount(1),false);
        }
        for(var ingredient:recipe.getFluidIngredients())if(ingredient.getFluids().length>0)fluids.fill(ingredient.getFluids()[0].copy(),IFluidHandler.FluidAction.EXECUTE);
    }
    private static MixingRecipe recipe(GameTestHelper h,String id){return (MixingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow().value();}
    private static int count(ManualMixerBlockEntity be,Item item){int n=0;for(int i=0;i<be.items().getSlots();i++){var s=be.items().getStackInSlot(i);if(s.is(item))n+=s.getCount();}return n;}

    @GameTest(template="empty",timeoutTicks=200)
    public static void everyLoadedColdRecipeUsesNativeBasinMatching(GameTestHelper h){
        var be=place(h);var other=new BlockPos(3,1,1);h.setBlock(other,AllBlocks.BASIN.get());var nativeBasin=(BasinBlockEntity)h.getBlockEntity(other);
        h.runAfterDelay(2,()->{
            int cold=0,matched=0,heated=0;var namespaces=new java.util.TreeSet<String>();
            var recipes=h.getLevel().getRecipeManager().<RecipeInput,MixingRecipe>getAllRecipesFor(AllRecipeTypes.MIXING.getType());
            for(var holder:recipes){
                var r=holder.value();boolean expected=r.getRequiredHeat()==HeatCondition.NONE;
                h.assertTrue(ManualMixerBlockEntity.allowed(r)==expected,"Heat admission changed: "+holder.id());
                if(!expected){heated++;continue;}
                cold++;namespaces.add(holder.id().getNamespace());fill(h,be,r);fill(h,nativeBasin,r);
                boolean nativeMatch=BasinRecipe.match(nativeBasin,r),manualMatch=BasinRecipe.match(be,r);
                h.assertTrue(nativeMatch==manualMatch,"Cold matching differs from native Create basin: "+holder.id());
                if(manualMatch)matched++;
            }
            be.clearContent();nativeBasin.clearContent();
            h.assertTrue(cold>10&&matched>10&&heated>0,"Recipe parity audit did not exercise installed recipes");
            System.out.println("MANUAL_MIXER_QA cold="+cold+" nativeMatch="+matched+" heatedRejected="+heated+" namespaces="+namespaces);
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void contentsAndComponentsSurviveReload(GameTestHelper h){
        var be=place(h);var apple=new ItemStack(Items.APPLE,7);apple.set(DataComponents.CUSTOM_NAME,Component.literal("Mixer persistence"));
        be.getInputInventory().setStackInSlot(0,apple);
        be.fluids().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,750),IFluidHandler.FluidAction.EXECUTE);
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());
        var restored=new ManualMixerBlockEntity(be.getBlockPos(),be.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(restored.getInputInventory().getStackInSlot(0).getCount()==7,"Item count lost");
        h.assertTrue(restored.getInputInventory().getStackInSlot(0).getHoverName().getString().equals("Mixer persistence"),"Item components lost");
        h.assertTrue(restored.fluids().drain(1000,IFluidHandler.FluidAction.SIMULATE).getAmount()==750,"Fluid contents lost");
        h.assertTrue(!restored.rotating(),"Saved machine resumed rotating without a player");
        System.out.println("MANUAL_MIXER_QA persistent item components + 750mB; reload never grants motion");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void crankExhaustionAndStopMatchCreate(GameTestHelper h){
        var be=place(h);var player=h.makeMockPlayer(GameType.SURVIVAL);float before=player.getFoodData().getExhaustionLevel();
        var pos=be.getBlockPos();
        var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.8,pos.getY()+.7,pos.getZ()+.8),net.minecraft.core.Direction.SOUTH,pos,false);
        be.getBlockState().useWithoutItem(h.getLevel(),player,hit);
        h.assertTrue(be.rotating(),"Empty-hand right-click on the crank did not start the mixer");
        float expected=ManualMixerBlockEntity.RPM*AllConfigs.server().kinetics.crankHungerMultiplier.getF();
        h.assertTrue(Math.abs(player.getFoodData().getExhaustionLevel()-before-expected)<.0001,"Exhaustion differs from Create crank setting");
        h.assertTrue(!be.turn(player),"Packet spam bypasses turn interval");
        h.assertTrue(Math.abs(player.getFoodData().getExhaustionLevel()-before-expected)<.0001,"Duplicate turn charged exhaustion");
        h.runAtTickTime(15,()->{h.assertTrue(!be.rotating(),"Unattended mixer did not stop after 10 ticks");h.assertTrue(be.progress()==0,"Empty mixer manufactured recipe progress");System.out.println("MANUAL_MIXER_QA configured exhaustion="+expected+"; cooldown; stop without repeated input");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=240)
    public static void genuineColdMixingPausesAndProducesDough(GameTestHelper h){
        var be=place(h);var r=recipe(h,"hearth_horizons:mixing/egg_dough");fill(h,be,r);
        var player=h.makeMockPlayer(GameType.SURVIVAL);int[] paused={-1};
        for(int t=1;t<40;t+=4)h.runAtTickTime(t,()->be.turn(player));
        h.runAtTickTime(60,()->{paused[0]=be.progress();h.assertTrue(paused[0]>0,"Hand operation never advances cold recipe");});
        h.runAtTickTime(80,()->h.assertTrue(paused[0]==be.progress(),"Progress advanced while nobody turned the crank"));
        for(int t=91;t<=210;t+=4)h.runAtTickTime(t,()->be.turn(player));
        h.runAtTickTime(220,()->{var dough=BuiltInRegistries.ITEM.get(ResourceLocation.parse("farmersdelight:wheat_dough"));h.assertTrue(count(be,dough)==3,"Recipe did not produce exactly 3 dough");h.assertTrue(count(be,Items.EGG)==0,"Egg input not consumed");System.out.println("MANUAL_MIXER_QA native flour+egg recipe, paused work, exact yield=3");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=280)
    public static void sodaFluidRecipeAndBlockedOutputAreTransactional(GameTestHelper h){
        var be=place(h);var r=recipe(h,"hearth_horizons_compat:soda/mixing/cola");fill(h,be,r);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(int t=1;t<240;t+=4)h.runAtTickTime(t,()->be.turn(player));
        h.runAtTickTime(250,()->{
            var soda=dev.hearthhorizons.compat.culinary.SodaFluids.ALL.stream().filter(f->f.name().equals("cola")).findFirst().orElseThrow();
            var result=be.fluids().drain(new FluidStack(soda.fluid().get(),1000),IFluidHandler.FluidAction.SIMULATE);
            h.assertTrue(result.getAmount()==250,"Soda recipe fluid output/volume incorrect");
            h.assertTrue(be.fluids().drain(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),IFluidHandler.FluidAction.SIMULATE).isEmpty(),"Water input not consumed");
            var dough=recipe(h,"hearth_horizons:mixing/egg_dough");fill(h,be,dough);
            for(int i=0;i<be.getOutputInventory().getSlots();i++)be.getOutputInventory().setStackInSlot(i,new ItemStack(Items.COBBLESTONE,64));
            int eggs=count(be,Items.EGG);
            h.assertTrue(!BasinRecipe.apply(be,dough),"Full output accepted another result");
            h.assertTrue(count(be,Items.EGG)==eggs,"Failed transaction consumed ingredients");
            System.out.println("MANUAL_MIXER_QA cola 250mB native fluid ledger; full output refuses consumption");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void breakDropsContentsExactlyOnce(GameTestHelper h){
        var be=place(h);be.getInputInventory().setStackInSlot(0,new ItemStack(Items.APPLE,3));
        h.getLevel().destroyBlock(h.absolutePos(POS),true);
        var box=new net.minecraft.world.phys.AABB(h.absolutePos(POS)).inflate(2);
        int apples=0,blocks=0;for(var e:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box)){if(e.getItem().is(Items.APPLE))apples+=e.getItem().getCount();if(e.getItem().is(ManualMixer.ITEM.get()))blocks+=e.getItem().getCount();}
        h.assertTrue(apples==3&&blocks==1,"Breaking duplicated/lost inventory or mixer: "+apples+" / "+blocks);
        System.out.println("MANUAL_MIXER_QA breaking returns exactly 3 apples and one mixer");h.succeed();
    }
}
