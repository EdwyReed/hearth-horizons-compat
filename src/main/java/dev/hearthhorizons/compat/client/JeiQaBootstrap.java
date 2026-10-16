package dev.hearthhorizons.compat.client;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Keep the copied QA world's generator independent of optional world-generation mods. */
@EventBusSubscriber(modid = HearthHorizonsCompat.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class JeiQaBootstrap {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        if (!Boolean.getBoolean("hh.jeiQa") && !Boolean.getBoolean("hh.sodaQa")) return;
        event.enqueueWork(() -> {
            try {
                var directory = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
                if (!directory.endsWith(java.nio.file.Path.of("run", "client"))) throw new IllegalStateException("QA requires the isolated run/client directory");
                var file = directory.resolve("saves/JeiQA/level.dat");
                var backup = file.resolveSibling("level.dat.before-jei-flat.nbt");
                if (!Files.exists(backup)) Files.copy(file, backup);
                var root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                var data = root.getCompound("Data");
                data.putString("LevelName", "JeiQA");
                var worldGen = data.getCompound("WorldGenSettings");
                var dimensions = worldGen.getCompound("dimensions");
                var overworld = new CompoundTag(); overworld.putString("type", "minecraft:overworld");
                var generator = new CompoundTag(); generator.putString("type", "minecraft:flat");
                var settings = new CompoundTag(); settings.putString("biome", "minecraft:plains");
                settings.putBoolean("lakes", false); settings.putBoolean("features", false);
                settings.put("structure_overrides", new ListTag());
                var layers = new ListTag();
                for (var layer : new Object[][] {{"minecraft:bedrock", 1}, {"minecraft:stone", 141}, {"minecraft:grass_block", 1}}) {
                    var value = new CompoundTag(); value.putString("block", (String) layer[0]); value.putInt("height", (int) layer[1]); layers.add(value);
                }
                settings.put("layers", layers); generator.put("settings", settings); overworld.put("generator", generator);
                dimensions.put("minecraft:overworld", overworld); worldGen.put("dimensions", dimensions); data.put("WorldGenSettings", worldGen);
                NbtIo.writeCompressed(root, file);
                System.out.println("JEI_QA_BOOTSTRAP copied world uses isolated vanilla flat generator");
            } catch (Exception error) { throw new RuntimeException("Cannot prepare isolated JEI QA world", error); }
        });
    }
}
