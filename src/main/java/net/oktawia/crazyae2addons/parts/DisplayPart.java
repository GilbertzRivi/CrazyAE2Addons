package net.oktawia.crazyae2addons.parts;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.items.parts.PartModels;
import appeng.menu.ISubMenu;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.automation.PlaneModels;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayTokenResolver;
import net.oktawia.crazyae2addons.logic.display.SampleRing;
import net.oktawia.crazyae2addons.menus.DisplayMenu;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DisplayPart extends AEBasePart implements MenuProvider, ISubMenuHost, IGridTickable {

    private static final PlaneModels MODELS = new PlaneModels(
            "part/display_mon_off",
            "part/display_mon_on"
    );

    public static final List<DisplayPart> CLIENT_INSTANCES = new CopyOnWriteArrayList<>();

    public byte   spin   = 0;
    public String textValue = "";
    public boolean mode   = true;
    public boolean margin = false;
    public boolean center = false;

    public HashMap<String, String> resolvedTokens = new HashMap<>();
    public final Map<String, SampleRing> rateHistory = new HashMap<>();
    /** How many sampling rounds to skip after the grid (re)connects. Prevents false deltas from ME storage initializing. */
    public transient int gridWarmupRemaining = 2;
    @Nullable public volatile String pendingInsert = null;
    public volatile int pendingInsertCursor = -1;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public DisplayPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode()
                .setIdlePowerUsage(1)
                .addService(IGridTickable.class, this);
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeUtf(textValue != null ? textValue : "");
        data.writeByte(spin);
        data.writeByte((mode ? 1 : 0) | (margin ? 2 : 0) | (center ? 4 : 0));
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean wasRegistered = CLIENT_INSTANCES.contains(this);
        boolean oldMode = this.mode;
        boolean oldPowered = this.isPowered();
        Direction oldSide = this.getSide();

        boolean changed = super.readFromStream(data);

        textValue = data.readUtf();
        spin = data.readByte();

        int flags = data.readByte();
        mode   = (flags & 1) != 0;
        margin = (flags & 2) != 0;
        center = (flags & 4) != 0;

        if (!wasRegistered) {
            CLIENT_INSTANCES.add(this);
        }

        boolean topologyChanged =
                !wasRegistered
                        || oldMode != this.mode
                        || oldPowered != this.isPowered()
                        || oldSide != this.getSide();

        if (topologyChanged) {
            DisplayGrid.invalidateClientCache();
        }

        return changed || topologyChanged;
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        boolean removed = CLIENT_INSTANCES.remove(this);
        if (removed) {
            DisplayGrid.invalidateClientCache();
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        DisplayTokenResolver.recomputeVariablesAndNotify(this);
        return TickRateModulation.IDLE;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(0, 0, 15.5, 16, 16, 16);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(isPowered(), isActive());
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!player.level().isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.DISPLAY_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void onPlacement(Player player) {
        super.onPlacement(player);
        byte rotation = (byte) (Mth.floor(player.getYRot() * 4f / 360f + 2.5) & 3);
        if (getSide() == Direction.UP || getSide() == Direction.DOWN) {
            this.spin = rotation;
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        tag.putString("textvalue", textValue);
        tag.putByte("spin", spin);
        tag.putBoolean("mode", mode);
        tag.putBoolean("margin", margin);
        tag.putBoolean("center", center);
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        if (tag.contains("textvalue")) textValue = tag.getString("textvalue");
        if (tag.contains("spin"))      spin      = tag.getByte("spin");
        if (tag.contains("mode"))      mode      = tag.getBoolean("mode");
        if (tag.contains("margin"))    margin    = tag.getBoolean("margin");
        if (tag.contains("center"))    center    = tag.getBoolean("center");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DisplayMenu(id, inv, this);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(CrazyMenuRegistrar.DISPLAY_MENU.get(), player, subMenu.getLocator());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(CrazyItemRegistrar.DISPLAY_MONITOR_PART.get());
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }
}
