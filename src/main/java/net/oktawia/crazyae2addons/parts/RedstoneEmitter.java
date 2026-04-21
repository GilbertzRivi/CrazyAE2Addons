package net.oktawia.crazyae2addons.parts;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AppEng;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.AbstractLevelEmitterPart;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.part.RedstoneEmitterMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;

public class RedstoneEmitter extends AbstractLevelEmitterPart implements MenuProvider {

    private static final String NBT_NAME = "name";

    public static final PartModel MODEL_OFF_OFF = new PartModel(
            AppEng.makeId("part/level_emitter_base_off"),
            AppEng.makeId("part/level_emitter_status_off")
    );

    public static final PartModel MODEL_OFF_ON = new PartModel(
            AppEng.makeId("part/level_emitter_base_off"),
            AppEng.makeId("part/level_emitter_status_on")
    );

    public static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(
            AppEng.makeId("part/level_emitter_base_off"),
            AppEng.makeId("part/level_emitter_status_has_channel")
    );

    public static final PartModel MODEL_ON_OFF = new PartModel(
            AppEng.makeId("part/level_emitter_base_on"),
            AppEng.makeId("part/level_emitter_status_off")
    );

    public static final PartModel MODEL_ON_ON = new PartModel(
            AppEng.makeId("part/level_emitter_base_on"),
            AppEng.makeId("part/level_emitter_status_on")
    );

    public static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(
            AppEng.makeId("part/level_emitter_base_on"),
            AppEng.makeId("part/level_emitter_status_has_channel")
    );

    @Setter
    private String name = randomHexId();

    public RedstoneEmitter(IPartItem<?> partItem) {
        super(partItem);
        getConfigManager().registerSetting(Settings.REDSTONE_EMITTER, RedstoneMode.LOW_SIGNAL);
    }

    public String getNameId() {
        return name;
    }

    public void setNameId(String name) {
        this.name = name == null ? "" : name;
        markForSave();
    }

    @Override
    protected void configureWatchers() {
    }

    @Override
    protected boolean hasDirectOutput() {
        return false;
    }

    @Override
    protected boolean getDirectOutput() {
        return false;
    }

    @Override
    protected int getUpgradeSlots() {
        return 0;
    }

    private static String randomHexId() {
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder(4);

        for (int i = 0; i < 4; i++) {
            builder.append(Integer.toHexString(random.nextInt(16)).toUpperCase());
        }

        return builder.toString();
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        this.name = data.contains(NBT_NAME) ? data.getString(NBT_NAME) : randomHexId();
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.putString(NBT_NAME, this.name);
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
        if (isActive() && isPowered()) {
            return isLevelEmitterOn() ? MODEL_ON_HAS_CHANNEL : MODEL_OFF_HAS_CHANNEL;
        }
        if (isPowered()) {
            return isLevelEmitterOn() ? MODEL_ON_ON : MODEL_OFF_ON;
        }
        return isLevelEmitterOn() ? MODEL_ON_OFF : MODEL_OFF_OFF;
    }

    public void setState(boolean state) {
        setReportingValue(state ? 1 : 0);
        updateState();
        markForSaveAndUpdate();
    }

    public boolean getState() {
        return getReportingValue() > 0;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RedstoneEmitterMenu(containerId, playerInventory, this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Redstone Emitter");
    }

    private void markForSave() {
        if (getHost() != null) {
            getHost().markForSave();
        }
    }

    private void markForSaveAndUpdate() {
        if (getHost() != null) {
            getHost().markForSave();
            getHost().markForUpdate();
        }
    }
}