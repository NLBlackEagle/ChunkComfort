package chunkcomfort.hearth;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Holds no data. It only exists so HearthGlowRenderer can draw the redstone at full
 * brightness, since 1.12 block models have no emissive faces.
 */
public class TileEntityHearth extends TileEntity {

    // REASON: the model sticks out half a block on two sides, so widen the culling box
    // or the glow vanishes while the edge of the hearth is still on screen.
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 1, 2));
    }
}
