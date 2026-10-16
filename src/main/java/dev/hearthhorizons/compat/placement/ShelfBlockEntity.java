package dev.hearthhorizons.compat.placement;

import dev.hearthhorizons.compat.HearthHorizonsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** No ticker, GUI or automation capability. Stacks are the sole persistent source of truth. */
public final class ShelfBlockEntity extends BlockEntity {
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int revision;
    private List<PlacementRules.Pose> cachedPoses;
    private final java.util.EnumMap<net.minecraft.core.Direction, net.minecraft.world.phys.shapes.VoxelShape> shapes = new java.util.EnumMap<>(net.minecraft.core.Direction.class);
    public ShelfBlockEntity(BlockPos pos, BlockState state) { super(HearthHorizonsCompat.SHELF.get(), pos, state); }
    public int size() { return items.get(0).isEmpty() ? 0 : items.get(1).isEmpty() ? 1 : 2; }
    public ItemStack item(int slot) { return items.get(slot); }
    public int revision() { return revision; }
    public List<PlacementRules.Pose> poses() {
        if (cachedPoses == null) cachedPoses = PlacementRules.SMALL_SHELF.layout(size(), PlacementRules.bounds(items.get(0)), PlacementRules.bounds(items.get(1)));
        return cachedPoses;
    }
    public net.minecraft.world.phys.shapes.VoxelShape shape(net.minecraft.core.Direction direction) {
        return shapes.computeIfAbsent(direction, d -> PlacementRules.shape(d, poses()));
    }
    public boolean insert(ItemStack source) {
        int count = size();
        if (count == 2 || !PlacementRules.accepts(source)) return false;
        items.set(count, source.copyWithCount(1));
        changed();
        return true;
    }
    public ItemStack remove(int slot) {
        if (slot < 0 || slot >= size()) return ItemStack.EMPTY;
        ItemStack result = items.get(slot);
        if (slot == 0) items.set(0, items.get(1));
        items.set(1, ItemStack.EMPTY);
        changed();
        return result;
    }
    public void clearForDrop() { items.set(0, ItemStack.EMPTY); items.set(1, ItemStack.EMPTY); changed(); }
    private void invalidate() { revision++; cachedPoses = null; shapes.clear(); }
    private void changed() { invalidate(); setChanged(); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Schema", 1);
        ContainerHelper.saveAllItems(tag, items, true, registries);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.set(0, ItemStack.EMPTY); items.set(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        invalidate();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
