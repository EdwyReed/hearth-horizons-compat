package dev.hearthhorizons.qol.culinary;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = HearthHorizonsQualityOfLife.ID)
public final class KitchenInteraction {
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().isEmpty() || event.getEntity().isShiftKeyDown() || !event.getEntity().mayBuild()) return;
        var level = event.getLevel(); var pos = event.getPos(); var state = level.getBlockState(pos);
        if (!(level.getBlockEntity(pos) instanceof KitchenBlockEntity contents) || !contents.hasSnapshot()) return;
        var player = event.getEntity();
        if (KitchenContents.portions(state.getBlock())) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide) return;
            var item = contents.take();
            if (!item.isEmpty()) { if (!player.addItem(item)) player.drop(item,false); }
            if (contents.portions() == 0) {
                for (var vessel : contents.vessels()) if (!player.addItem(vessel)) player.drop(vessel,false);
                level.removeBlock(pos,false);
            }
            return;
        }
        var food = contents.packed().get(DataComponents.FOOD);
        if (food == null) return;
        event.setCanceled(true); event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide || !player.canEat(false)) return;
        // FoodData.eat(int,float) expects a modifier, not absolute saturation.
        player.getFoodData().eat(food.nutrition(), food.nutrition() == 0 ? 0 : food.saturation() / (food.nutrition() * 2F));
        for (var effect : food.effects()) if (level.random.nextFloat() < effect.probability()) player.addEffect(effect.effect());
        level.gameEvent(player, GameEvent.EAT, pos);
        level.playSound(null,pos,SoundEvents.GENERIC_EAT,SoundSource.PLAYERS,1,1);
        Block plate = ((net.minecraft.world.item.BlockItem)KitchenContents.pastryPlate().getItem()).getBlock();
        var next = plate.defaultBlockState();
        if (next.hasProperty(HorizontalDirectionalBlock.FACING)) next = next.setValue(HorizontalDirectionalBlock.FACING,state.getValue(HorizontalDirectionalBlock.FACING));
        level.setBlock(pos,next,3);
    }
    private KitchenInteraction() {}
}
