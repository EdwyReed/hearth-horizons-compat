package dev.hearthhorizons.qol.culinary;

import dev.hearthhorizons.qol.HearthHorizonsQualityOfLife;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No ticker or GUI. The placed object is the original component-bearing item.
 * Breaking returns that item, never a fresh full platter plus loose contents.
 */
public final class KitchenBlockEntity extends BlockEntity {
    private ItemStack packed = ItemStack.EMPTY;
    public KitchenBlockEntity(BlockPos pos, BlockState state) { super(HearthHorizonsQualityOfLife.KITCHEN.get(), pos, state); }
    public ItemStack packed() { return packed.isEmpty() ? new ItemStack(getBlockState().getBlock()) : packed.copy(); }
    public boolean hasSnapshot() { return !packed.isEmpty() && packed.has(DataComponents.CONTAINER); }
    public void place(ItemStack stack) { packed = stack.copyWithCount(1); changed(); }
    public int portions() { return (int)KitchenContents.unpack(packed).stream().filter(s -> !KitchenContents.vessel(s)).count(); }
    public ItemStack take() {
        var contents = KitchenContents.unpack(packed);
        for (int i = contents.size()-1; i>=0; i--) {
            if (KitchenContents.vessel(contents.get(i))) continue;
            var result = contents.remove(i);
            packed.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            changed();
            return result;
        }
        return ItemStack.EMPTY;
    }
    public java.util.List<ItemStack> vessels() { return KitchenContents.unpack(packed).stream().filter(KitchenContents::vessel).toList(); }
    private void changed() {
        setChanged();
        if (level != null) {
            var state = getBlockState();
            if (state.hasProperty(KitchenContents.PORTIONS) && hasSnapshot()) {
                state = state.setValue(KitchenContents.PORTIONS, Math.min(4, portions()));
                level.setBlock(worldPosition, state, 3);
            }
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!packed.isEmpty()) tag.put("Packed", packed.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        packed = ItemStack.parseOptional(registries, tag.getCompound("Packed"));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
