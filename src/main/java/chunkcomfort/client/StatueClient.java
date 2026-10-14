package chunkcomfort.client;

import chunkcomfort.ChunkComfort;
import chunkcomfort.network.NetworkHandler;
import chunkcomfort.network.PacketStatueCandle;
import chunkcomfort.player.PlayerComfortManager;
import chunkcomfort.registry.StatueRegistry;
import chunkcomfort.statue.BlockStatue;
import chunkcomfort.statue.StatueType;
import chunkcomfort.statue.TileEntityStatue;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Client side of the statues: picking the candle slot the player aims at, the slot
 * highlight, the action bar hint and the candle flames.
 */
@Mod.EventBusSubscriber(modid = ChunkComfort.MODID, value = Side.CLIENT)
public class StatueClient {

    private static int lastHintSlot = -2;
    private static BlockPos lastHintPos = null;
    private static boolean lastHintSneaking = false;

    // The ghost candle and action bar hint only show when the player is this close to the statue (blocks)
    private static final double HINT_RANGE = 2.0;

    /** Call in preInit (client): load candle layouts now and on every resource reload. */
    public static void init() {
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(new StatueLayouts());
    }

    /** Number of usable slots: every candle position in the layout, effect or not. */
    private static int usableSlots(BlockStatue block) {
        return Math.min(StatueLayouts.get(block.type).size(), StatueType.MAX_SLOTS);
    }

    /**
     * The slot the player is looking at (ray traced against each candle box), or -1.
     * filled = true only considers lit slots, false only empty slots.
     */
    public static int getAimedSlot(BlockStatue block, BlockPos pos, IBlockState state, EntityPlayer player, boolean filled) {
        TileEntity te = player.world.getTileEntity(pos);
        if (!(te instanceof TileEntityStatue)) return -1;
        TileEntityStatue statue = (TileEntityStatue) te;

        double reach = Minecraft.getMinecraft().playerController.getBlockReachDistance();
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d end = eyes.add(player.getLook(1.0F).scale(reach));
        EnumFacing facing = state.getValue(BlockStatue.FACING);
        List<StatueLayouts.Slot> layout = StatueLayouts.get(block.type);

        int best = -1;
        double bestDist = Double.MAX_VALUE;

        for (int slot = 0; slot < usableSlots(block); slot++) {
            if (statue.hasCandle(slot) != filled) continue;

            StatueLayouts.Slot candle = layout.get(slot);
            AxisAlignedBB box = StatueType.candleBox(StatueType.rotate(candle.pos, facing), candle.height).offset(pos);
            RayTraceResult hit = box.calculateIntercept(eyes, end);
            if (hit == null) continue;

            double dist = hit.hitVec.squareDistanceTo(eyes);
            if (dist < bestDist) {
                bestDist = dist;
                best = slot;
            }
        }
        return best;
    }

    /** Sneak-right-click on a statue with a candle (insert) or an empty hand (remove). */
    public static boolean onStatueClicked(BlockStatue block, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand) {
        boolean insert = StatueRegistry.isCandle(player.getHeldItem(hand));
        if (!player.isSneaking()) return false;
        int slot = getAimedSlot(block, pos, state, player, !insert);
        if (slot < 0) return false;

        NetworkHandler.INSTANCE.sendToServer(new PacketStatueCandle(pos, slot, insert, hand));
        return true;
    }

    public static void spawnCandleFlames(BlockStatue block, IBlockState state, World world, BlockPos pos, Random rand) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityStatue)) return;
        TileEntityStatue statue = (TileEntityStatue) te;
        EnumFacing facing = state.getValue(BlockStatue.FACING);
        List<StatueLayouts.Slot> layout = StatueLayouts.get(block.type);

        for (int slot = 0; slot < layout.size(); slot++) {
            // Only candles in a spot with an effect burn; the rest are decoration
            if (!statue.hasCandle(slot) || !StatueRegistry.isEffectSlot(block, slot)) continue;

            StatueLayouts.Slot candle = layout.get(slot);
            Vec3d p = StatueType.rotate(candle.pos, facing);
            double x = pos.getX() + p.x;
            double y = pos.getY() + p.y + (candle.height + 1) / 16.0;
            double z = pos.getZ() + p.z;

            spawnSmall(EnumParticleTypes.FLAME, x, y, z);
            if (rand.nextInt(4) == 0) spawnSmall(EnumParticleTypes.SMOKE_NORMAL, x, y + 0.03, z);
        }
    }

    // Candle flames are tiny next to torches, so their particles are drawn smaller
    private static final float FLAME_SCALE = 0.5F;

    private static void spawnSmall(EnumParticleTypes type, double x, double y, double z) {
        // Null when the particle setting skips it
        Particle particle = Minecraft.getMinecraft().effectRenderer.spawnEffectParticle(type.getParticleID(), x, y, z, 0, 0, 0);
        if (particle != null) particle.multipleParticleScaleBy(FLAME_SCALE);
    }

    // --- Aiming helpers: what the player is currently pointing at ---

    private static class Aim {
        BlockStatue block;
        BlockPos pos;
        IBlockState state;
        int slot;
        boolean insert;
    }

    private static Aim getAim() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        RayTraceResult target = mc.objectMouseOver;
        if (player == null || target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return null;

        IBlockState state = player.world.getBlockState(target.getBlockPos());
        if (!(state.getBlock() instanceof BlockStatue)) return null;

        BlockPos pos = target.getBlockPos();
        if (player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > HINT_RANGE * HINT_RANGE) return null;

        Aim aim = new Aim();
        aim.block = (BlockStatue) state.getBlock();
        aim.pos = target.getBlockPos();
        aim.state = state;

        // Main hand first, then the off hand: the same order right-click tries the hands in.
        // An empty main hand only aims at candles, so a candle in the off hand can still aim at empty spots.
        for (EnumHand hand : EnumHand.values()) {
            if (!BlockStatue.isCandleInteraction(player, hand)) continue;
            aim.insert = StatueRegistry.isCandle(player.getHeldItem(hand));
            aim.slot = getAimedSlot(aim.block, aim.pos, state, player, !aim.insert);
            if (aim.slot >= 0) return aim;
        }
        return null;
    }

    // --- Slot highlight: a see-through ghost candle at the aimed spot instead of the block outline ---

    // Ghost candle opacity (0-255)
    private static final int GHOST_ALPHA = 128;

    @SubscribeEvent
    public static void onDrawHighlight(DrawBlockHighlightEvent event) {
        Aim aim = getAim();
        if (aim == null || aim.slot < 0) return;

        EntityPlayer player = event.getPlayer();
        float partial = event.getPartialTicks();
        double dx = player.lastTickPosX + (player.posX - player.lastTickPosX) * partial;
        double dy = player.lastTickPosY + (player.posY - player.lastTickPosY) * partial;
        double dz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partial;

        // Same placement as StatueBakedModel, but relative to the camera
        EnumFacing facing = aim.state.getValue(BlockStatue.FACING);
        StatueLayouts.Slot candle = StatueLayouts.get(aim.block.type).get(aim.slot);
        Vec3d p = StatueType.rotate(candle.pos, facing);
        float ox = (float) (aim.pos.getX() - dx + p.x - 0.5);
        float oy = (float) (aim.pos.getY() - dy + p.y);
        float oz = (float) (aim.pos.getZ() - dz + p.z - 0.5);

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        // Pulled towards the camera so it draws cleanly over a candle that is already there
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-1.0F, -10.0F);
        // Full brightness, so the ghost is visible in the dark too
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
        for (BakedQuad quad : ModModels.getCandleQuads()) {
            buffer.addVertexData(StatueBakedModel.place(quad, ox, oy, oz, (float) candle.height).getVertexData());
            // Keep the usual per-side shading so the ghost still reads as a 3D candle
            int shade = (int) (255 * LightUtil.diffuseLight(quad.getFace()));
            buffer.putColor4(GHOST_ALPHA << 24 | shade << 16 | shade << 8 | shade);
        }
        tessellator.draw();

        GlStateManager.doPolygonOffset(0.0F, 0.0F);
        GlStateManager.disablePolygonOffset();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();

        event.setCanceled(true);
    }

    // --- Statue tooltip ---

    /**
     * Whether the sneak key is held, for the statue tooltip (candle info only shows while sneaking).
     * REASON: KeyBinding.isKeyDown() isn't updated while a GUI is open, so read the key directly.
     */
    public static boolean isSneakKeyDown() {
        int key = Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode();
        if (key < 0) return Mouse.isButtonDown(key + 100); // bound to a mouse button
        return key > 0 && Keyboard.isKeyDown(key);
    }

    // --- Action bar hint: what the aimed candle does ---

    /**
     * What a candle spot does, for the action bar and the statue tooltip. Effects the player's
     * comfort hasn't unlocked yet are obfuscated, with the Comfort tier they unlock at.
     */
    public static String describeSlot(Block block, int slot) {
        if (!StatueRegistry.isEffectSlot(block, slot)) return I18n.format("tooltip.chunkcomfort.statue.decorative");

        List<String> parts = new ArrayList<>();
        for (StatueRegistry.SlotEffect effect : StatueRegistry.getSlotEffects(block, slot)) {
            if (effect.potion == null) {
                parts.add(I18n.format("tooltip.chunkcomfort.statue.notfound"));
                continue;
            }

            String name = I18n.format(effect.potion.getName());
            String level = effect.isNegation() ? "" : " " + StatueType.roman(effect.amplifier + 1);

            if (ClientComfortStatus.comfort < effect.comfort) {
                // Name the Comfort tier it unlocks at; points only if it's above every tier
                int tier = PlayerComfortManager.getTierForComfort(effect.comfort);
                String unlock = tier >= 0 ? I18n.format("tooltip.chunkcomfort.statue.unlock.tier", StatueType.roman(tier + 1))
                        : I18n.format("tooltip.chunkcomfort.statue.unlock.points", effect.comfort);
                parts.add(I18n.format(effect.isNegation() ? "tooltip.chunkcomfort.statue.locked.negates"
                        : "tooltip.chunkcomfort.statue.locked.activates", name, level, unlock));
            } else {
                parts.add(effect.isNegation() ? I18n.format("tooltip.chunkcomfort.statue.negates", name)
                        : I18n.format("tooltip.chunkcomfort.statue.activates", name + level));
            }
        }
        return String.join("§7, ", parts);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.isGamePaused()) return;

        Aim aim = getAim();
        if (aim == null || aim.slot < 0) {
            // Clear our own hint right away instead of letting it fade out
            if (lastHintSlot >= 0) mc.ingameGUI.setOverlayMessage("", false);
            lastHintSlot = -2;
            lastHintPos = null;
            return;
        }

        // Only refresh when the aimed slot or sneaking changes, or the message is about to fade
        boolean sneaking = mc.player.isSneaking();
        if (aim.slot == lastHintSlot && aim.pos.equals(lastHintPos) && sneaking == lastHintSneaking
                && mc.player.ticksExisted % 40 != 0) return;
        lastHintSlot = aim.slot;
        lastHintPos = aim.pos;
        lastHintSneaking = sneaking;

        // Candle info only while sneaking, otherwise just the sneak prompt
        String hint = sneaking
                ? I18n.format("tooltip.chunkcomfort.statue.aim", StatueType.roman(aim.slot + 1), describeSlot(aim.block, aim.slot))
                : I18n.format("tooltip.chunkcomfort.statue.sneak");
        mc.ingameGUI.setOverlayMessage(new TextComponentString(hint), false);
    }
}
