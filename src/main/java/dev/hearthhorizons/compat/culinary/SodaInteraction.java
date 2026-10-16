package dev.hearthhorizons.compat.culinary;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@EventBusSubscriber(modid = HearthHorizonsCompat.ID)
public final class SodaInteraction {
    public static final int SERVING = 250;
    @SubscribeEvent public static void fillBottle(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var player = event.getEntity();
        if (!event.getItemStack().is(Items.GLASS_BOTTLE) || player.isShiftKeyDown() || !player.mayBuild()) return;
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, event.getPos(), event.getFace());
        if (handler == null) handler = level.getCapability(Capabilities.FluidHandler.BLOCK, event.getPos(), null);
        if (handler == null) return;
        for (var flavour : SodaFluids.ALL) {
            var request = new FluidStack(flavour.fluid().get(), SERVING);
            var available = handler.drain(request, IFluidHandler.FluidAction.SIMULATE);
            if (available.getAmount() < SERVING || !FluidStack.isSameFluidSameComponents(request, available)) continue;
            if (!level.isClientSide) {
                var drained = handler.drain(request, IFluidHandler.FluidAction.EXECUTE);
                if (drained.getAmount() != SERVING) throw new IllegalStateException("Fluid handler changed within one server interaction");
                player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(event.getItemStack(), player, new ItemStack(flavour.drink())));
                level.playSound(null, event.getPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7F, 1.25F);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }
    }
    @SubscribeEvent public static void fizzAfterDrinking(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        var flavour = SodaFluids.ALL.stream().filter(f -> event.getItem().is(f.drink())).findFirst().orElse(null);
        if (flavour == null) return;
        var entity = event.getEntity();
        // Cosmetic only: no random harmful effects or balance-changing potion buffs.
        level.sendParticles(SodaParticles.option(flavour.colour()), entity.getX(), entity.getEyeY() - 0.15, entity.getZ(), 12, 0.15, 0.1, 0.15, 0.02);
        level.playSound(null, entity.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    @EventBusSubscriber(modid = HearthHorizonsCompat.ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Components {
        @SubscribeEvent public static void containers(ModifyDefaultComponentsEvent event) {
            for (var flavour : SodaFluids.ALL) {
                var item = flavour.drink();
                var food = item.components().get(DataComponents.FOOD);
                if (food == null) throw new IllegalStateException("Missing Pam soda food: " + flavour.name());
                event.modify(item, builder -> builder.set(DataComponents.FOOD, new FoodProperties(
                    food.nutrition(), food.saturation(), food.canAlwaysEat(), food.eatSeconds(),
                    java.util.Optional.of(new ItemStack(Items.GLASS_BOTTLE)), food.effects())));
            }
        }
    }
    private SodaInteraction() {}
}
