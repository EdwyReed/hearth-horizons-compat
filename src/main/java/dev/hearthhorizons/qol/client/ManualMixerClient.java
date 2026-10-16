package dev.hearthhorizons.qol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import dev.hearthhorizons.qol.mixer.ManualMixer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;

@EventBusSubscriber(modid=HearthHorizonsQualityOfLife.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ManualMixerClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(ManualMixer.TYPE.get(),context->new ManualMixerRenderer());}
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){event.registerReloadListener((ResourceManagerReloadListener)manager->ManualMixerRenderer.reload());}
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent event){
        event.registerItem(new IClientItemExtensions(){
            private final BlockEntityWithoutLevelRenderer renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){ManualMixerRenderer.renderItem(pose,buffers,light,overlay);}
            };
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
        },ManualMixer.ITEM.get());
    }
}
