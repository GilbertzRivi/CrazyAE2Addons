package net.oktawia.crazyae2addons.parts;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.automation.UpgradeablePart;
import appeng.parts.p2p.P2PModels;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.EntityTickerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

import static java.lang.Math.pow;


public class EntityTickerPart extends UpgradeablePart implements IGridTickable, MenuProvider, IUpgradeableObject {
    public static final int actionsPerSec = 20;
    public EntityTickerMenu menu;

    private static final P2PModels MODELS = new P2PModels(
            new ResourceLocation(CrazyAddons.MODID, "part/entity_ticker_part_item"));

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public EntityTickerPart(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(1)
                .addService(IGridTickable.class,this);
    }

    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public boolean onPartActivate(Player p, InteractionHand hand, Vec3 pos) {
        if (!p.getCommandSenderWorld().isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.ENTITY_TICKER_MENU.get(), p, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(5, 5, 12, 11, 11, 13);
        bch.addBox(3, 3, 13, 13, 13, 14);
        bch.addBox(2, 2, 14, 14, 14, 16);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode iGridNode) {
        return new TickingRequest(20/actionsPerSec, 20/actionsPerSec, false, true);
    }

    @Override
    public void upgradesChanged() {
        if (this.menu != null){
            this.menu.sendUpgradeNum(this.getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD));
        }
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode iGridNode, int ticksSinceLastCall) {
        BlockEntity target = getLevel().getBlockEntity(getBlockEntity().getBlockPos().relative(getSide()));
        if (target != null && isActive()){
            tickBlockEntity(target);
        }
        return TickRateModulation.IDLE;
    }

    private <T extends BlockEntity> void tickBlockEntity(@NotNull T blockEntity) {
        if (!CrazyConfig.COMMON.enableEntityTicker.get()) return;
        String id = ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock()).toString();
        if (CrazyConfig.COMMON.EntityTickerBlackList.get().contains(id)) return;
        if (this.getGridNode() == null || this.getMainNode() == null || this.getMainNode().getGrid() == null) return;
        int powerDraw = (int) (CrazyConfig.COMMON.EntityTickerCost.get() * pow(4, getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD))) / 2; // convert to FE
        double extractedPower = getMainNode().getGrid().getEnergyService().extractAEPower(powerDraw, Actionable.MODULATE, PowerMultiplier.CONFIG);
        if (extractedPower < powerDraw){
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        BlockEntityTicker<T> blockEntityTicker = this.getLevel().getBlockState(pos).getTicker(this.getLevel(),
                (BlockEntityType<T>) blockEntity.getType());
        if (blockEntityTicker == null) return;
        int speed = (int) pow(2, (getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD) + 1));
        if (blockEntity.getLevel() == null){
            return;
        }
        for (int i = 0; i < speed - 1; i++) {
            blockEntityTicker.tick(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(),
                    blockEntity);
        }
    }

    @Override
    public boolean hasCustomName() {
        return super.hasCustomName();
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EntityTickerMenu(containerId, playerInventory, this);
    }

    @Override
    protected int getUpgradeSlots() {
        return 8;
    }
}