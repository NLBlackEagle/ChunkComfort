package chunkcomfort.statue;

import chunkcomfort.ChunkComfort;
import chunkcomfort.client.StatueClient;
import chunkcomfort.registry.StatueRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * A statue with candle slots. Sneak-right-click a candle spot with a candle to place it,
 * sneak-right-click a candle with an empty hand to take it back out. Each lit slot
 * activates the potions configured for it in Statue Candle Slots.
 *
 * Which slot is clicked is worked out on the client (candle positions come from client
 * resources, see StatueLayouts) and sent to the server in PacketStatueCandle.
 */
@Mod.EventBusSubscriber(modid = ChunkComfort.MODID)
public class BlockStatue extends Block {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    // REASON: candles are an unlisted property (bitmask from the tile entity) instead of one
    // boolean property per slot, so any number of slots doesn't multiply the block states.
    public static final IUnlistedProperty<Integer> CANDLE_MASK = new IUnlistedProperty<Integer>() {
        @Override public String getName() { return "candle_mask"; }
        @Override public boolean isValid(Integer value) { return true; }
        @Override public Class<Integer> getType() { return Integer.class; }
        @Override public String valueToString(Integer value) { return String.valueOf(value); }
    };

    public final StatueType type;

    public BlockStatue(StatueType type) {
        super(Material.ROCK);
        this.type = type;
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.STONE);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    // --- Blockstate: facing is stored in meta, candles come from the tile entity ---

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new IProperty[]{FACING}, new IUnlistedProperty[]{CANDLE_MASK});
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        int mask = te instanceof TileEntityStatue ? ((TileEntityStatue) te).getCandleMask() : 0;
        return ((IExtendedBlockState) state).withProperty(CANDLE_MASK, mask);
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

    // --- Tile entity ---

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityStatue();
    }

    // --- Candle interaction ---

    /** Holding a candle (place, while sneaking) or an empty hand (take out): the hands that interact with candles. */
    public static boolean isCandleInteraction(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        return StatueRegistry.isCandle(held) || held.isEmpty();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        // Both placing and taking out need sneak, a normal right-click does nothing
        if (!player.isSneaking() || !isCandleInteraction(player, hand)) return false;

        // The client knows where the candles are, so it picks the slot and tells the server
        if (world.isRemote) return StatueClient.onStatueClicked(this, pos, state, player, hand);
        return true;
    }

    /**
     * Candles are placed and taken out while sneaking, but a sneaking right-click skips
     * onBlockActivated when either hand holds an item. Let it through for candle interactions on a
     * statue (both sides, so the server doesn't use the candle item on the statue instead, e.g.
     * placing a Rustic candle block).
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!player.isSneaking() || !isCandleInteraction(player, event.getHand())) return;
        if (event.getWorld().getBlockState(event.getPos()).getBlock() instanceof BlockStatue) {
            event.setUseBlock(Event.Result.ALLOW);
        }
    }

    /** Server side: light (insert) or take out a candle, after the client picked the slot. */
    public void handleCandleClick(World world, BlockPos pos, EntityPlayer player, EnumHand hand, int slot, boolean insert) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityStatue)) return;
        TileEntityStatue statue = (TileEntityStatue) te;
        // Every spot takes a candle, not only the ones with an effect
        if (slot < 0 || slot >= StatueType.MAX_SLOTS) return;

        ItemStack held = player.getHeldItem(hand);

        if (insert) {
            if (!player.isSneaking() || !StatueRegistry.isCandle(held) || statue.hasCandle(slot)) return;

            ItemStack candle = held.copy();
            candle.setCount(1);
            statue.setCandle(slot, candle);
            if (!player.capabilities.isCreativeMode) held.shrink(1);
            // Lighting sound only for spots that burn, the decorative ones are just set down
            if (StatueRegistry.isEffectSlot(this, slot)) {
                world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 0.6F, 1.2F);
            } else {
                world.playSound(null, pos, SoundEvents.BLOCK_CLOTH_PLACE, SoundCategory.BLOCKS, 0.6F, 1.2F);
            }
        } else {
            if (!player.isSneaking() || !held.isEmpty() || !statue.hasCandle(slot)) return;

            net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player, statue.removeCandle(slot));
            if (StatueRegistry.isEffectSlot(this, slot)) {
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4F, 1.6F);
            } else {
                world.playSound(null, pos, SoundEvents.BLOCK_CLOTH_BREAK, SoundCategory.BLOCKS, 0.6F, 1.2F);
            }
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityStatue) {
            for (ItemStack candle : ((TileEntityStatue) te).getCandles()) {
                if (!candle.isEmpty()) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), candle);
            }
        }
        super.breakBlock(world, pos, state);
    }

    // --- Candle flames ---

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        StatueClient.spawnCandleFlames(this, state, world, pos, rand);
    }

    // --- Item tooltip: what each candle does ---

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        // Plaque dedication, only for statues that have one in the lang file
        String plaqueKey = getTranslationKey() + ".plaque";
        if (I18n.hasKey(plaqueKey)) {
            tooltip.add(I18n.format("tooltip.chunkcomfort.statue.plaque", I18n.format(plaqueKey)));
        }

        // Candle info only while sneak is held, to keep the tooltip short
        if (!StatueClient.isSneakKeyDown()) {
            tooltip.add(I18n.format("tooltip.chunkcomfort.statue.hold_sneak"));
            return;
        }

        for (int slot : StatueRegistry.getEffectSlots(this)) {
            tooltip.add(I18n.format("tooltip.chunkcomfort.statue.slot", StatueType.roman(slot + 1), StatueClient.describeSlot(this, slot)));
        }
        tooltip.add(I18n.format("tooltip.chunkcomfort.statue.hint"));
    }

    // --- Render / shape: not a full cube ---

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
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
