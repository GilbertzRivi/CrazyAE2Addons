package net.oktawia.crazyae2addons.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GadgetCostPreviewClient {

    private static final int RED = 0xFF4040;
    private static final long TOOLTIP_TTL_MS = 175L;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        var mc = Minecraft.getInstance();
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) return;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof PortableSpatialStorage)) {
            held = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!(held.getItem() instanceof PortableSpatialStorage)) return;
        }

        HitResult hr = mc.hitResult;
        if (!(hr instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;

        BlockPos lookAt = bhr.getBlockPos();
        Direction face = bhr.getDirection();

        int energy = readEnergyFromNBT(held);

        if (held.hasTag() && held.getTag().contains("selA")) {
            int[] a = held.getTag().getIntArray("selA");
            if (a.length == 3) {
                BlockPos cornerA = new BlockPos(a[0], a[1], a[2]);
                BlockPos cornerB = lookAt;
                BlockPos origin = lookAt;

                int cost = PortableSpatialStorage.computeCutCostFE(level, cornerA, cornerB, origin);
                showCostWithTTL("Cut: %,d FE", cost, energy);
                return;
            }
        }

        if (held.hasTag() && held.getTag().getBoolean("code")) {
            BlockPos originWorld = lookAt.relative(face);
            Direction pasteFacing = player.getDirection();
            PortableSpatialStorage.Basis basis = PortableSpatialStorage.Basis.forFacing(pasteFacing);

            int[] posArr = held.getTag().getIntArray("preview_positions");
            double req = 0.0;
            for (int i = 0; i + 2 < posArr.length; i += 3) {
                BlockPos local = new BlockPos(posArr[i], posArr[i + 1], posArr[i + 2]);
                BlockPos world = PortableSpatialStorage.localToWorld(local, originWorld, basis);
                req += PortableSpatialStorage.calcStepCostFE(originWorld, world);
            }
            int cost = (int) Math.ceil(req);
            showCostWithTTL("Paste: %,d FE", cost, energy);
        }
    }

    private static void showCostWithTTL(String fmt, int cost, int energy) {
        String msg = String.format(fmt, cost);
        Integer color = (cost > energy) ? RED : null;
        PreviewTooltipRenderer.set(msg, color, TOOLTIP_TTL_MS);
    }

    private static int readEnergyFromNBT(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        return stack.getTag().getInt("internalCurrentPower") * 2;
    }
}
