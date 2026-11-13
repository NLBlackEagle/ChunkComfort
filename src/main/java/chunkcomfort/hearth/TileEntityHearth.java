package chunkcomfort.hearth;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Feeds HearthGlowRenderer (1.12 block models have no emissive faces) and remembers when the
 * hearth was placed. The oldest hearth in an area owns the comfort zone; a newer hearth within
 * its radius does not make a zone of its own (see AreaComfortCalculator.isActiveAnchor).
 */
public class TileEntityHearth extends TileEntity {

    /** World time when placed. 0 for hearths placed before this was tracked, so they count as oldest. */
    public long placedTime = 0;

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        placedTime = compound.getLong("placedTime");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setLong("placedTime", placedTime);
        return compound;
    }

    // REASON: the model sticks out half a block on two sides, so widen the culling box
    // or the glow vanishes while the edge of the hearth is still on screen.
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 1, 2));
    }
}
