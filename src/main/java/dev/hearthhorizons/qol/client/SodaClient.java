package dev.hearthhorizons.qol.client;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import dev.hearthhorizons.qol.culinary.SodaFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = HearthHorizonsQualityOfLife.ID, value = Dist.CLIENT)
public final class SodaClient {
    private static final List<BlockPos> cups = new ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel previousLevel;
    private static long previousTick = -1;
    public static long spawnedBubbles;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level != previousLevel) { cups.clear(); previousLevel = mc.level; previousTick = -1; }
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        long time = mc.level.getGameTime();
        if (time == previousTick) return;
        previousTick = time;
        // Bounded nearby scan once per two seconds. Never request unloaded chunks.
        if (time % 40 == 0) {
            cups.clear();
            for (var pos : BlockPos.betweenClosed(mc.player.blockPosition().offset(-8,-8,-8), mc.player.blockPosition().offset(8,3,8))) {
                if (!mc.level.hasChunkAt(pos)) continue;
                var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock());
                if (id.getNamespace().equals("create") && id.getPath().equals("basin")) cups.add(pos.immutable());
            }
        }
        if (time % 4 != 0) return;
        for (var pos : cups) {
            if (pos.distToCenterSqr(mc.player.position()) > 144 || !mc.level.hasChunkAt(pos)) continue;
            var handler = mc.level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            if (handler == null) continue;
            int amount = 0, capacity = 0;
            SodaFluids.Flavour soda = null;
            for (int i=0; i<handler.getTanks(); i++) {
                var fluid = handler.getFluidInTank(i);
                if (SodaFluids.find(fluid.getFluid()) == null || fluid.isEmpty()) continue;
                soda = SodaFluids.find(fluid.getFluid()); amount += fluid.getAmount(); capacity += handler.getTankCapacity(i);
            }
            if (soda == null || capacity == 0) continue;
            double x = pos.getX() + .2 + mc.level.random.nextDouble() * .6;
            double z = pos.getZ() + .2 + mc.level.random.nextDouble() * .6;
            double y = pos.getY() + .18 + Math.min(.52, .52 * amount / capacity);
            // Pop particles survive outside water and use vanilla's shader-compatible
            // particle pipeline, avoiding water-only bubble particles that instantly die.
            mc.level.addParticle(dev.hearthhorizons.qol.culinary.SodaParticles.option(soda.colour()), x, y + .04, z, 0, .005, 0);
            spawnedBubbles++;
        }
    }
    @EventBusSubscriber(modid = HearthHorizonsQualityOfLife.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Extensions {
        @SubscribeEvent public static void particles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(dev.hearthhorizons.qol.culinary.SodaParticles.BUBBLE.get(),SodaBubbleParticle.Provider::new);
        }
        @SubscribeEvent public static void fluids(RegisterClientExtensionsEvent event) {
            for (var flavour : SodaFluids.ALL) event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return ResourceLocation.withDefaultNamespace("block/water_still"); }
                @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.withDefaultNamespace("block/water_flow"); }
                @Override public int getTintColor() { return flavour.colour(); }
            }, flavour.type().get());
        }
    }
    private SodaClient() {}
}
