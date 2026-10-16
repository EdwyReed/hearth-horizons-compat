package dev.hearthhorizons.compat.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.hearthhorizons.compat.HearthHorizonsCompat;
import dev.hearthhorizons.compat.mixer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** Original approved mesh, cached on resource reload. Fluid/item surfaces are real basin contents. */
public final class ManualMixerRenderer implements BlockEntityRenderer<ManualMixerBlockEntity> {
    private static final ResourceLocation MESH=ResourceLocation.fromNamespaceAndPath(HearthHorizonsCompat.ID,"models/mesh/manual_mixer.json");
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath(HearthHorizonsCompat.ID,"textures/entity/manual_mixer_white.png");
    private record Part(String id,float[] origin,float[] p,float[] n,int[] colours) {}
    private static List<Part> parts;
    private static final int[] TRIANGLE_QUAD={0,1,2,2},FLUID_QUAD={0,2,1,1};
    public static void reload(){parts=null;}
    @Override public int getViewDistance(){return 32;}
    private static float[] floats(JsonArray array){float[] out=new float[array.size()];for(int i=0;i<out.length;i++)out[i]=array.get(i).getAsFloat();return out;}
    private static List<Part> mesh(){
        if(parts!=null)return parts;
        var out=new ArrayList<Part>();
        try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).openAsReader()){
            var json=JsonParser.parseReader(reader).getAsJsonObject();
            for(var entry:json.getAsJsonArray("parts")){
                var p=entry.getAsJsonObject();var cs=p.getAsJsonArray("colours");int[] c=new int[cs.size()];for(int i=0;i<c.length;i++)c[i]=(int)cs.get(i).getAsLong();
                out.add(new Part(p.get("id").getAsString(),floats(p.getAsJsonArray("origin")),floats(p.getAsJsonArray("positions")),floats(p.getAsJsonArray("normals")),c));
            }
        }catch(Exception e){throw new IllegalStateException("Cannot load manual mixer reference mesh",e);}
        return parts=List.copyOf(out);
    }
    public static float yaw(Direction facing){return switch(facing){case NORTH->180;case EAST->90;case WEST->-90;default->0;};}
    @Override public void render(ManualMixerBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(yaw(be.getBlockState().getValue(ManualMixerBlock.ORIENTATION))));pose.scale(1f/16,1f/16,1f/16);
        renderMesh(pose,buffers,light,overlay,be.angle(partial));
        contents(be,partial,pose,buffers,light,overlay);
        pose.popPose();
    }
    public static void renderItem(PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,0,.5);pose.scale(1f/16,1f/16,1f/16);renderMesh(pose,buffers,light,overlay,0);pose.popPose();
    }
    private static void renderMesh(PoseStack pose,MultiBufferSource buffers,int light,int overlay,float angle){
        var consumer=buffers.getBuffer(RenderType.entitySolid(WHITE));
        for(var part:mesh()){
            pose.pushPose();pose.translate(part.origin[0],part.origin[1],part.origin[2]);
            if(part.id.equals("gear")||part.id.equals("crank"))pose.mulPose(Axis.ZP.rotationDegrees(-angle));
            else if(part.id.equals("whisk"))pose.mulPose(Axis.YP.rotationDegrees(angle*4));
            for(int t=0;t<part.colours.length;t+=3){
                // Minecraft's buffered entity format is quads: duplicate the third triangle vertex.
                for(int j:TRIANGLE_QUAD){int i=t+j,k=i*3;
                    consumer.addVertex(pose.last(),part.p[k],part.p[k+1],part.p[k+2]).setColor(part.colours[i]).setUv(.5f,.5f).setOverlay(overlay).setLight(light).setNormal(pose.last(),part.n[k],part.n[k+1],part.n[k+2]);
                }
            }
            pose.popPose();
        }
    }
    private static void contents(ManualMixerBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var tank=be.fluids();if(tank==null)return;
        int total=0,capacity=0;
        for(int i=0;i<tank.getTanks();i++){total+=tank.getFluidInTank(i).getAmount();capacity+=tank.getTankCapacity(i);}
        float y=2.13f;
        for(int i=0;i<tank.getTanks();i++){
            var stack=tank.getFluidInTank(i);if(stack.isEmpty())continue;
            y+=3.55f*stack.getAmount()/Math.max(1,capacity);
            fluid(pose,buffers,stack,y,light,overlay);
        }
        var items=be.items();if(items==null)return;
        int visible=0;
        for(int slot=0;slot<items.getSlots()&&visible<16;slot++){
            var stack=items.getStackInSlot(slot);if(stack.isEmpty())continue;
            double a=visible*2.399963+(be.rotating()?Math.toRadians(be.angle(partial))*.22:0),radius=visible==0?0:Math.min(1.75,.6+Math.sqrt(visible)*.38);
            // Oversized ingredients sit high in the bowl and visibly break the rim silhouette.
            // Keep liquid fill height independent; this is only the loose-item presentation.
            pose.pushPose();pose.translate(-2.7+Math.cos(a)*radius,Math.max(4.8f,y+.12f),Math.sin(a)*radius);pose.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees(a)));pose.scale(12,12,12);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.GROUND,light,overlay,pose,buffers,be.getLevel(),slot);
            pose.popPose();visible++;
        }
    }
    private static void fluid(PoseStack pose,MultiBufferSource buffers,FluidStack stack,float y,int light,int overlay){
        var ext=IClientFluidTypeExtensions.of(stack.getFluid());
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(stack));
        int colour=ext.getTintColor(stack);int alpha=(colour>>>24)&255;if(alpha==0)alpha=255;
        int rgba=(colour&0xffffff)|(Math.max(170,alpha)<<24);
        var v=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        float r=2.55f+(y-2.03f)/4.02f*1.05f;
        for(int i=0;i<8;i++){
            double a=Math.PI/8+i*Math.PI/4,b=a+Math.PI/4;
            float[][] ps={{-2.7f,y,0},{-2.7f+(float)Math.cos(a)*r,y,(float)Math.sin(a)*r},{-2.7f+(float)Math.cos(b)*r,y,(float)Math.sin(b)*r}};
            for(int j:FLUID_QUAD){var p=ps[j];float u=.5f+(p[0]+2.7f)/(2*r),w=.5f+p[2]/(2*r);
                v.addVertex(pose.last(),p[0],p[1],p[2]).setColor(rgba).setUv(sprite.getU(u),sprite.getV(w)).setOverlay(overlay).setLight(light).setNormal(pose.last(),0,1,0);
            }
        }
    }
}
