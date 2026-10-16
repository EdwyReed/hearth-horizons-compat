package dev.hearthhorizons.compat.client;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.culinary.SodaFluids;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Dev-only, opt-in native render test. Never loaded by the distributed mod. */
@EventBusSubscriber(modid=HearthHorizonsCompat.ID,value=Dist.CLIENT)
public final class SodaQa {
    private static boolean prepared;
    private static int frames;
    private static long nextFrameTime;
    private static String shader="vanilla";
    // Advance only after a real frame. Tick catch-up in a background window can
    // otherwise capture a stale framebuffer from a different scene.
    @SubscribeEvent public static void tick(RenderFrameEvent.Post event) {
        if(!Boolean.getBoolean("hh.sodaQa")) return;
        var mc=Minecraft.getInstance();
        if(mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen screen) { screen.onClose(); return; }
        if(mc.level==null || mc.player==null || mc.screen!=null || mc.getSingleplayerServer()==null) return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(java.nio.file.Path.of("run","client"))) throw new IllegalStateException("Soda QA outside isolated directory");
        long now=System.nanoTime();
        if(now<nextFrameTime) return;
        nextFrameTime=now+50_000_000L;
        frames++;
        if(!prepared) {
            prepared=true;
            mc.options.hideGui=true;
            var uuid=mc.player.getUUID(); var server=mc.getSingleplayerServer();
            server.execute(() -> {
                var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(0,6000,false,false);
                for(int x=17;x<=31;x++) for(int z=7;z<=20;z++) for(int y=80;y<=88;y++)
                    level.setBlock(new BlockPos(x,y,z),y==80?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                for(int i=0;i<SodaFluids.ALL.size();i++) {
                    var pos=new BlockPos(20+(i%3)*3,81,10+(i/3)*3);
                    level.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:basin")).defaultBlockState(),3);
                    var tank=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,null);
                    int filled=tank.fill(new FluidStack(SodaFluids.ALL.get(i).fluid().get(),750),IFluidHandler.FluidAction.EXECUTE);
                    if(filled!=750) throw new IllegalStateException("QA tank fill failed");
                }
                var player=server.getPlayerList().getPlayer(uuid);
                player.setGameMode(GameType.SPECTATOR); player.connection.teleport(23.5,84.1,8.2,0,43);
            });
        }
        if(frames==80) {
            mc.player.setYRot(0);mc.player.setXRot(43);
            if(Boolean.getBoolean("hh.shaderQa")) {
                try {
                    Class<?> iris=Class.forName("net.irisshaders.iris.Iris");
                    shader=(String)iris.getMethod("getCurrentPackName").invoke(null);
                    if(!(Boolean)iris.getMethod("isPackInUseQuick").invoke(null)) throw new IllegalStateException("Shader pack disabled");
                    var manager=iris.getMethod("getPipelineManager").invoke(null);
                    System.out.println("SODA_QA_SHADER active="+shader+" pipelineManager="+manager.getClass().getName());
                } catch(Exception ex) { throw new RuntimeException("Soda shader QA did not load Iris pack",ex); }
            }
            for(int i=0;i<SodaFluids.ALL.size();i++) {
                var pos=new BlockPos(20+(i%3)*3,81,10+(i/3)*3);
                var tank=mc.level.getCapability(Capabilities.FluidHandler.BLOCK,pos,null);
                if(tank==null) throw new IllegalStateException("Client fluid capability absent");
                boolean found=false;
                for(int t=0;t<tank.getTanks();t++) if(tank.getFluidInTank(t).is(SodaFluids.ALL.get(i).fluid().get()) && tank.getFluidInTank(t).getAmount()==750) found=true;
                if(!found) throw new IllegalStateException("Client soda flavour/volume sync failed: "+i);
            }
            System.out.println("SODA_QA_SYNC all nine 750mB fluids present on client");
        }
        if(frames==180 || frames==200) {
            Screenshot.grab(mc.gameDirectory,"soda-"+shader.replaceAll("[^A-Za-z0-9.-]","_")+"-"+frames+".png",mc.getMainRenderTarget(),message -> System.out.println("SODA_QA_SCREENSHOT "+message.getString()));
        }
        if(frames==210) {
            mc.getSingleplayerServer().execute(() -> {
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                player.connection.teleport(23.5,88,13.5,0,90);
            });
        }
        if(frames==230) { mc.player.setYRot(0);mc.player.setXRot(90); }
        if(frames==250) {
            System.out.println("SODA_QA_CAMERA player="+mc.player.position()+" camera="+mc.gameRenderer.getMainCamera().getPosition());
            for(int i=0;i<SodaFluids.ALL.size();i++) {
                var pos=new BlockPos(20+(i%3)*3,81,10+(i/3)*3);
                var tank=mc.level.getCapability(Capabilities.FluidHandler.BLOCK,pos,null);
                if(tank==null) throw new IllegalStateException("Soda fixture vanished before top capture: "+i+" / "+mc.level.getBlockState(pos));
            }
            Screenshot.grab(mc.gameDirectory,"soda-"+shader.replaceAll("[^A-Za-z0-9.-]","_")+"-top.png",mc.getMainRenderTarget(),message -> System.out.println("SODA_QA_SCREENSHOT "+message.getString()));
        }
        if(frames==280) {
            if(SodaClient.spawnedBubbles<30) throw new IllegalStateException("No native visible soda bubble particles: "+SodaClient.spawnedBubbles);
            System.out.println("SODA_QA_COMPLETE shader="+shader+" clientFlavours=9 spawnedBubbles="+SodaClient.spawnedBubbles);
            if(Boolean.getBoolean("hh.kitchenQa")) mc.getSingleplayerServer().execute(() -> {
                var level=mc.getSingleplayerServer().overworld();
                for(int x=17;x<=31;x++) for(int z=7;z<=20;z++) level.setBlock(new BlockPos(x,81,z),Blocks.AIR.defaultBlockState(),3);
                for(int row=0;row<2;row++) for(int portion=0;portion<=4;portion++) {
                    String id=row==0?"cluttered:sliced_bread":"cluttered:sweetheart_baking_set_ingredients";
                    var item=new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
                    var contents=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
                    contents.add(new net.minecraft.world.item.ItemStack(row==0?net.minecraft.world.item.Items.HEAVY_WEIGHTED_PRESSURE_PLATE:net.minecraft.world.item.Items.BOWL));
                    String[] ingredients=row==0?new String[]{"moredelight:bread_slice","moredelight:bread_slice","moredelight:bread_slice","moredelight:bread_slice"}:new String[]{"create:wheat_flour","minecraft:sugar","minecraft:cocoa_beans","minecraft:egg"};
                    for(int i=0;i<portion;i++) contents.add(new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(ingredients[i]))));
                    dev.hearthhorizons.compat.culinary.KitchenContents.capture(item,contents);
                    var pos=new BlockPos(19+portion*2,81,11+row*3);
                    level.setBlock(pos,((net.minecraft.world.item.BlockItem)item.getItem()).getBlock().defaultBlockState(),3);
                    ((dev.hearthhorizons.compat.culinary.KitchenBlockEntity)level.getBlockEntity(pos)).place(item);
                }
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                player.connection.teleport(23.5,86,12.5,0,90);
            });
        }
        if(frames==310 && Boolean.getBoolean("hh.kitchenQa")) { mc.player.setYRot(0);mc.player.setXRot(90); }
        if(frames==340 && Boolean.getBoolean("hh.kitchenQa")) {
            Screenshot.grab(mc.gameDirectory,"kitchen-"+shader.replaceAll("[^A-Za-z0-9.-]","_")+"-portions.png",mc.getMainRenderTarget(),message -> System.out.println("KITCHEN_QA_SCREENSHOT "+message.getString()));
        }
        if(frames==360) {
            mc.stop();
        }
    }
}
