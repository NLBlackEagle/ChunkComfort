package chunkcomfort.hearth;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * A hearth that glows without burning. It is the default comfort anchor (Fire Blocks list),
 * replacing real fire so the anchor never adds heat for SimpleDifficulty's temperature.
 *
 * The model is 2 blocks wide and half a block tall: it sits on its own block and sticks out
 * half a block to the left and right (model x from -8 to 24 when facing north), so it rotates
 * with placement. The redstone inside glows via TileEntityHearth + HearthGlowRenderer.
 */
public class BlockHearth extends Block {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    // REASON: the box follows the model's overhang so the outline and collision match what
    // you see. Facing north/south it is wide along X, facing east/west wide along Z.
    private static final AxisAlignedBB BOX_X = new AxisAlignedBB(-0.5, 0, 0, 1.5, 0.5, 1);
    private static final AxisAlignedBB BOX_Z = new AxisAlignedBB(0, 0, -0.5, 1, 0.5, 1.5);

    public BlockHearth() {
        super(Material.ROCK);
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.STONE);
        setLightLevel(10F / 15F); // light 10, a bit dimmer than a torch (14)
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    // --- Facing ---

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        // Face the player who placed it
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    // --- Shape: not a full cube, so neighbours are not culled and light passes ---

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(FACING).getAxis() == EnumFacing.Axis.X ? BOX_Z : BOX_X;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    // --- Glow (tile entity only feeds the full-bright renderer) ---

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityHearth();
    }

    // --- Visuals ---

    /**
     * Redstone dust rising from the redstone pieces in the hearth. Purely visual, no heat or fire.
     * The pieces sit in model x -4..20, z 3..13 (facing north), so along the long axis the
     * sparks span -0.25..1.25 and across it 0.2..0.8.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        boolean wideOnX = state.getValue(FACING).getAxis() == EnumFacing.Axis.Z;
        int count = 1 + rand.nextInt(2);
        for (int i = 0; i < count; i++) {
            double along = -0.25 + rand.nextDouble() * 1.5;
            double across = 0.2 + rand.nextDouble() * 0.6;
            double x = pos.getX() + (wideOnX ? along : across);
            double z = pos.getZ() + (wideOnX ? across : along);
            double y = pos.getY() + 0.3 + rand.nextDouble() * 0.15;
            // REDSTONE particles read the speed args as colour; 0,0,0 gives the default redstone red
            world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
