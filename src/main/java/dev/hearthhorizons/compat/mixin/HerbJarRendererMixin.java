package dev.hearthhorizons.compat.mixin;

import dev.hearthhorizons.compat.client.HerbJarRenderScope;
import net.joefoxe.hexerei.item.custom.HerbJarItemRenderer;
import net.joefoxe.hexerei.tileentity.HerbJarTile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Cache only decoding. Hexerei keeps all of its own model, dye and content rendering. */
@Mixin(value = HerbJarItemRenderer.class, remap = false)
public abstract class HerbJarRendererMixin {
    @Redirect(method = "renderTileStuff", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/CustomData;copyTag()Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag hh$cachedData(CustomData data, ItemStack item, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var snapshot = HerbJarRenderScope.current();
        return snapshot == null || snapshot.owner() != item ? data.copyTag() : snapshot.data();
    }
    @Redirect(method = "renderTileStuff", at = @At(value = "INVOKE", target = "Lnet/joefoxe/hexerei/item/custom/HerbJarItemRenderer;loadBlockEntityFromItem(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/item/ItemStack;)Lnet/joefoxe/hexerei/tileentity/HerbJarTile;"))
    private HerbJarTile hh$cachedDisplay(CompoundTag data, ItemStack item) {
        var snapshot = HerbJarRenderScope.current();
        if (snapshot == null || snapshot.owner() != item) return HerbJarItemRenderer.loadBlockEntityFromItem(data, item);
        if (HerbJarRenderScope.METRICS) HerbJarRenderScope.reusedSnapshots++;
        return snapshot.display();
    }
}
