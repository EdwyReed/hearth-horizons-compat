package dev.hearthhorizons.compat.client;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.placement.PlacementRules;
import dev.hearthhorizons.compat.placement.ShelfBlockEntity;
import net.joefoxe.hexerei.items.JarHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Explicitly opt-in QA, exclusively in this project's separate development world. */
@EventBusSubscriber(modid = HearthHorizonsCompat.ID, value = Dist.CLIENT)
public final class ClientQa {
    private static int ticks;
    private static boolean prepared;
    private static String lastScreen = "";
    private static ItemStack savedSingle = ItemStack.EMPTY;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("hh.shelfQa")) return;
        Minecraft mc = Minecraft.getInstance();
        String screen = mc.screen == null ? "world" : mc.screen.getClass().getName();
        if (!screen.equals(lastScreen)) { System.out.println("SHELF_QA_SCREEN " + screen); lastScreen = screen; }
        if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding) { onboarding.onClose(); return; }
        if (mc.level == null || mc.player == null || mc.screen != null || mc.getSingleplayerServer() == null) return;
        ticks++;
        if (!prepared) {
            prepared = true;
            var uuid = mc.player.getUUID();
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                var level = server.overworld();
                level.setDayTime(6000);
                for (int x = 0; x < 15; x++) for (int y = 78; y <= 85; y++) {
                    level.setBlock(new BlockPos(x, y, 3), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);
                    for (int z = -6; z < 3; z++) level.setBlock(new BlockPos(x, y, z), y == 78 ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
                for (var drop : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(0, 78, -6, 15, 86, 3))) drop.discard();
                for (int count = 0; count <= 2; count++) {
                    BlockPos pos = new BlockPos(4 + count * 3, 81, 2);
                    var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("cluttered:small_shelf" + (count == 2 ? "_pink" : "")));
                    level.setBlock(pos, block.defaultBlockState().setValue(PlacementRules.COUNT, count), 3);
                    if (count > 0 && level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) {
                        for (int slot = 0; slot < count; slot++) {
                            ItemStack jar = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("hexerei:herb_jar")));
                            JarHandler inv = new JarHandler(1, 1024);
                            inv.setStackInSlot(0, new ItemStack(slot == 0 ? Items.DANDELION : Items.POPPY, 512));
                            CompoundTag data = new CompoundTag();
                            data.put("Inventory", inv.serializeNBT(level.registryAccess()));
                            jar.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                            jar.set(DataComponents.DYED_COLOR, new DyedItemColor(slot == 0 ? 0x718c4a : 0x9b5997, false));
                            shelf.insert(jar);
                        }
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    }
                }
                var player = server.getPlayerList().getPlayer(uuid);
                if (player != null) { player.setGameMode(GameType.SPECTATOR); player.teleportTo(7.5, 80.6, -1.5); player.setYRot(0); player.setXRot(5); }
            });
        }
        if (ticks == 60) {
            for (int count = 0; count <= 2; count++) {
                BlockPos pos = new BlockPos(4 + count * 3, 81, 2);
                var be = mc.level.getBlockEntity(pos);
                if (count == 0 ? be != null : !(be instanceof ShelfBlockEntity shelf) || shelf.size() != count)
                    throw new IllegalStateException("Client shelf sync failed at " + pos);
            }
            System.out.println("SHELF_QA_CLIENT_SYNC initial 0/1/2 passed");
        }
        if (ticks == 80) mc.getSingleplayerServer().execute(() -> {
            var level = mc.getSingleplayerServer().overworld();
            BlockPos pos = new BlockPos(7, 81, 2);
            ShelfBlockEntity be = (ShelfBlockEntity) level.getBlockEntity(pos);
            savedSingle = be.remove(0);
            level.setBlock(pos, level.getBlockState(pos).setValue(PlacementRules.COUNT, 0), 3);
        });
        if (ticks == 120) {
            BlockPos pos = new BlockPos(7, 81, 2);
            if (mc.level.getBlockState(pos).hasBlockEntity() || mc.level.getBlockEntity(pos) != null)
                throw new IllegalStateException("Client retained empty shelf BlockEntity");
            System.out.println("SHELF_QA_CLIENT_SYNC last-removal cleanup passed");
            mc.getSingleplayerServer().execute(() -> {
                var level = mc.getSingleplayerServer().overworld();
                level.setBlock(pos, level.getBlockState(pos).setValue(PlacementRules.COUNT, 1), 3);
                ((ShelfBlockEntity) level.getBlockEntity(pos)).insert(savedSingle);
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            });
        }
        if (ticks == 100) { mc.player.setYRot(0); mc.player.setXRot(5); }
        if (ticks == 200) Screenshot.grab(mc.gameDirectory, "shelf-qa.png", mc.getMainRenderTarget(), message -> System.out.println("SHELF_QA_SCREENSHOT " + message.getString()));
        if (ticks == 240) {
            System.out.println("SHELF_QA_CACHED_DECODES " + HerbJarRenderScope.decodedSnapshots + " CACHE_HITS " + HerbJarRenderScope.reusedSnapshots);
            if (HerbJarRenderScope.reusedSnapshots < 100) throw new IllegalStateException("Native rendering did not reuse decoded jar snapshots");
            mc.stop();
        }
    }
}
