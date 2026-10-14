package chunkcomfort.statue;

import chunkcomfort.registry.StatueRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

/**
 * Holds the candles placed in a statue. Each filled slot activates the potions
 * configured for that slot in Statue Candle Slots.
 */
public class TileEntityStatue extends TileEntity {

    // The actual candle item per slot, so taking it out gives back the same candle
    private final NonNullList<ItemStack> candles = NonNullList.withSize(StatueType.MAX_SLOTS, ItemStack.EMPTY);

    public boolean hasCandle(int slot) {
        return slot >= 0 && slot < candles.size() && !candles.get(slot).isEmpty();
    }

    /** Bit i set = slot i has a candle. Drives the candle rendering. */
    public int getCandleMask() {
        int mask = 0;
        for (int i = 0; i < candles.size(); i++) {
            if (!candles.get(i).isEmpty()) mask |= 1 << i;
        }
        return mask;
    }

    public void setCandle(int slot, ItemStack candle) {
        candles.set(slot, candle);
        markChangedAndSync();
    }

    public ItemStack removeCandle(int slot) {
        ItemStack removed = candles.get(slot);
        candles.set(slot, ItemStack.EMPTY);
        markChangedAndSync();
        return removed;
    }

    public NonNullList<ItemStack> getCandles() {
        return candles;
    }

    /** Adds every slot with a candle (as StatueRegistry.slotKey) to the given set. */
    public void collectLitSlots(Set<Integer> out) {
        Block block = getBlockType();
        for (int slot = 0; slot < candles.size(); slot++) {
            if (hasCandle(slot)) out.add(StatueRegistry.slotKey(block, slot));
        }
    }

    private void markChangedAndSync() {
        markDirty();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        for (int i = 0; i < candles.size(); i++) candles.set(i, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, candles);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, candles, true);
        return compound;
    }

    // --- Client sync, so the candles render ---

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
        if (world != null) world.markBlockRangeForRenderUpdate(pos, pos);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
