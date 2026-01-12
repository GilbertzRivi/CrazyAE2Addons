package net.oktawia.crazyae2addonslite.parts;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.AbstractLevelEmitterPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.menus.RedstoneEmitterMenu;
import net.oktawia.crazyae2addonslite.menus.RedstoneTerminalMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.List;

public class RedstoneEmitterPart extends AbstractLevelEmitterPart implements MenuProvider {
    @PartModels
    public static final ResourceLocation MODEL_BASE_OFF = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_base_off");
    @PartModels
    public static final ResourceLocation MODEL_BASE_ON = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_base_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_OFF = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_status_off");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_ON = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_status_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_status_has_channel");
    public static final PartModel MODEL_OFF_OFF = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_OFF);
    public static final PartModel MODEL_OFF_ON = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_ON);
    public static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_HAS_CHANNEL);
    public static final PartModel MODEL_ON_OFF = new PartModel(MODEL_BASE_ON, MODEL_STATUS_OFF);
    public static final PartModel MODEL_ON_ON = new PartModel(MODEL_BASE_ON, MODEL_STATUS_ON);
    public static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(MODEL_BASE_ON, MODEL_STATUS_HAS_CHANNEL);

    public String name = randomHexId();

    public RedstoneEmitterPart(IPartItem<?> partItem) {
        super(partItem);
        this.getConfigManager().registerSetting(Settings.REDSTONE_EMITTER, RedstoneMode.LOW_SIGNAL);
    }

    @Override
    protected void configureWatchers() {}

    @Override
    protected boolean hasDirectOutput() {
        return false;
    }

    @Override
    protected boolean getDirectOutput() {
        return false;
    }

    private static String randomHexId() {
        SecureRandom rand = new SecureRandom();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) sb.append(Integer.toHexString(rand.nextInt(16)).toUpperCase());
        return sb.toString();
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        this.name = data.getString("name");
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.putString("name", this.name);
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.REDSTONE_EMITTER_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return this.isLevelEmitterOn() ? MODEL_ON_HAS_CHANNEL : MODEL_OFF_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return this.isLevelEmitterOn() ? MODEL_ON_ON : MODEL_OFF_ON;
        } else {
            return this.isLevelEmitterOn() ? MODEL_ON_OFF : MODEL_OFF_OFF;
        }
    }

    public void setState(boolean state){
        this.setReportingValue(state ? 1 : 0);
        this.updateState();
    }

    public boolean getState(){
        return this.getReportingValue() > 0;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new RedstoneEmitterMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Redstone Emitter");
    }

    public List<RedstoneTerminalMenu.EmitterInfo> getEmitters() {
        var grid = getMainNode().getGrid();
        if (grid == null) return List.of();
        return grid.getActiveMachines(RedstoneEmitterPart.class).stream().filter(part -> part != this).map(part ->
                new RedstoneTerminalMenu.EmitterInfo(
                        part.getBlockEntity().getBlockPos(),
                        part.name,
                        part.isLevelEmitterOn()
                )
        ).toList();
    }
}
