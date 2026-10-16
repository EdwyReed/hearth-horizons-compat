package dev.hearthhorizons.compat.culinary;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.List;

/** Distinct fluid identities survive Create tanks, pipes, saves and recipe matching.
 * No world block/bucket is registered: soda is a beverage, not an infinite spring.
 */
public final class SodaFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, HearthHorizonsCompat.ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, HearthHorizonsCompat.ID);
    public record Flavour(String name, String russian, int colour, DeferredHolder<FluidType, FluidType> type,
                          DeferredHolder<Fluid, BaseFlowingFluid.Source> fluid) {
        public Item drink() { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("pamhc2foodextended", name + "sodaitem")); }
    }
    private static Flavour flavour(String name, String russian, int colour) {
        DeferredHolder<FluidType, FluidType> type = TYPES.register(name + "_soda", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid.hearth_horizons_compat." + name + "_soda")
            .density(1030).viscosity(1100).canConvertToSource(false)) {});
        // Source and flowing are intentionally the same non-placeable fluid.
        // BaseFlowingFluid supports this for machine-only fluids without a block.
        @SuppressWarnings("unchecked")
        DeferredHolder<Fluid, BaseFlowingFluid.Source>[] holder = new DeferredHolder[1];
        holder[0] = FLUIDS.register(name + "_soda", () -> new BaseFlowingFluid.Source(
            new BaseFlowingFluid.Properties(type, () -> holder[0].get(), () -> holder[0].get())));
        return new Flavour(name, russian, colour, type, holder[0]);
    }
    public static final List<Flavour> ALL = List.of(
        flavour("cherry", "Вишнёвая газировка", 0xDDE42F64),
        flavour("cola", "Кола", 0xDD623B25),
        flavour("ginger", "Имбирная газировка", 0xDDEEB854),
        flavour("grapefruit", "Грейпфрутовая газировка", 0xDDF59483),
        flavour("grape", "Виноградная газировка", 0xDD9247B5),
        flavour("lemonlime", "Лимонно-лаймовая газировка", 0xDDCBED65),
        flavour("orange", "Апельсиновая газировка", 0xDDFF992D),
        flavour("rootbeer", "Рутбир", 0xDD89522B),
        flavour("strawberry", "Клубничная газировка", 0xDDF36E89));
    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }
    public static Flavour find(Fluid fluid) { return ALL.stream().filter(f -> f.fluid().get() == fluid).findFirst().orElse(null); }
    private SodaFluids() {}
}
