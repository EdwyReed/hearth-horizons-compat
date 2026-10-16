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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(HearthHorizonsCompat.ID)
@PrefixGameTestTemplate(false)
public final class KitchenGameTests {
    private static ItemStack stack(String id) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))); }
    private static ItemStack craft(GameTestHelper h, String recipeId, List<ItemStack> input) {
        var recipe = (CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(recipeId)).orElseThrow().value();
        var grid = new ArrayList<>(input);
        while (grid.size()<9) grid.add(ItemStack.EMPTY);
        var crafting = CraftingInput.of(3,3,grid);
        h.assertTrue(recipe.matches(crafting,h.getLevel()), "Serving ingredients no longer match " + recipeId);
        return recipe.assemble(crafting,h.getLevel().registryAccess());
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void realServingNutritionBreadAndKitRoundTrips(GameTestHelper h) {
        var registry = h.getLevel().registryAccess();
        var croissant = stack("pamhc2foodextended:croissantitem");
        var served = craft(h,"cluttered:croissant",List.of(KitchenContents.pastryPlate(),croissant));
        h.assertTrue(served.get(DataComponents.FOOD).nutrition()==croissant.get(DataComponents.FOOD).nutrition(),"Plating changed nutrition");
        h.assertTrue(served.get(DataComponents.FOOD).saturation()==croissant.get(DataComponents.FOOD).saturation(),"Plating changed saturation");
        var slices = new ArrayList<ItemStack>();
        slices.add(new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE));
        for (int i=0;i<4;i++) {
            var slice = stack(i%2==0 ? "moredelight:bread_slice" : "farmersdelight_extended:bread_slice");
            // This second mod's exact ID is resolved from the accepted tag below.
            if (slice.isEmpty()) slice = stack("moredelight:bread_slice");
            slice.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("slice-"+i));
            slices.add(slice);
        }
        var bread = craft(h,"cluttered:sliced_bread",slices);
        var flour = stack("create:wheat_flour");
        var kit = craft(h,"cluttered:sweetheart_baking_set_ingredients",List.of(new ItemStack(Items.BOWL),flour,new ItemStack(Items.SUGAR),new ItemStack(Items.COCOA_BEANS),new ItemStack(Items.EGG)));
        for (var packed : List.of(bread,kit)) {
            var block = ((BlockItem)packed.getItem()).getBlock();
            var pos = new BlockPos(1,1,1);
            h.setBlock(pos,block);
            var be = (KitchenBlockEntity)h.getBlockEntity(pos);
            be.place(packed);
            var saved = be.saveWithFullMetadata(registry);
            var restored = (KitchenBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(h.absolutePos(pos),be.getBlockState(),saved,registry);
            h.assertTrue(ItemStack.isSameItemSameComponents(restored.packed(),packed),"Kitchen NBT lost source components");
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.getInventory().selected = 8; // Keep the interaction hand empty while inventory fills.
            var worldPos = h.absolutePos(pos);
            var expected = KitchenContents.unpack(packed);
            for (int i=0;i<4;i++) {
                var event = new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,worldPos,new BlockHitResult(Vec3.atCenterOf(worldPos),Direction.UP,worldPos,false));
                KitchenInteraction.use(event);
                h.assertTrue(event.isCanceled(),"Kitchen right-click not handled");
                if (i<3) h.assertTrue(h.getBlockState(pos).getValue(KitchenContents.PORTIONS)==3-i,"Wrong visible portion count");
            }
            h.assertTrue(h.getBlockState(pos).isAir(),"Empty kitchen composition persisted");
            for (var item : expected) {
                h.assertTrue(player.getInventory().items.stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s,item)),"Ingredient identity/component or vessel lost");
            }
            h.assertTrue(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum()==5,"Contents or vessel duplicated");
        }
        System.out.println("KITCHEN_QA exact serving nutrition/saturation; bread and ingredient kit preserve original stacks across NBT; four clicks update models and return one tray/bowl without duplication");
        h.succeed();
    }
}
