package dev.hearthhorizons.qol.culinary;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class SodaParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES=DeferredRegister.create(Registries.PARTICLE_TYPE,HearthHorizonsQualityOfLife.ID);
    public static final DeferredHolder<ParticleType<?>,ParticleType<ColorParticleOption>> BUBBLE=TYPES.register("soda_bubble",() -> new ParticleType<ColorParticleOption>(false) {
        @Override public MapCodec<ColorParticleOption> codec() { return ColorParticleOption.codec(this); }
        @Override public StreamCodec<? super ByteBuf,ColorParticleOption> streamCodec() { return ColorParticleOption.streamCodec(this); }
    });
    public static void register(IEventBus bus) { dev.hearthhorizons.qol.migration.LegacyIds.register(bus, TYPES); }
    public static ColorParticleOption option(int colour) { return ColorParticleOption.create(BUBBLE.get(),colour | 0xFF000000); }
    private SodaParticles() {}
}
