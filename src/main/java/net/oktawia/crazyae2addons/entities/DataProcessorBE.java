package net.oktawia.crazyae2addons.entities;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.VariableMachine;
import net.oktawia.crazyae2addons.menus.DataProcessorMenu;
import net.oktawia.crazyae2addons.parts.RedstoneEmitterPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.security.SecureRandom;
import java.util.Objects;

public class DataProcessorBE extends AENetworkInvBlockEntity
        implements VariableMachine, MenuProvider, InternalInventoryHost {

    public String identifier = randomHexId();
    public AppEngInternalInventory inv = new AppEngInternalInventory(this, 1, 1);

    private Globals luaGlobals;
    private LuaValue compiledTopLevel;

    private String watchedVar = "";

    public DataProcessorBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.DATA_PROCESSOR_BE.get(), pos, blockState);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(2)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.DATA_PROCESSOR_BLOCK.get()));
    }

    public static String randomHexId() {
        SecureRandom rand = new SecureRandom();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) sb.append(Integer.toHexString(rand.nextInt(16)).toUpperCase());
        return sb.toString();
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("ident")) {
            this.identifier = data.getString("ident");
        }
        if (data.contains("inv")) {
            this.inv.readFromNBT(data, "inv");
        }
        if (data.contains("watchedVar")) {
            this.watchedVar = data.getString("watchedVar");
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        this.onChangeInventory(getInternalInventory(), 0);
        updateRegistration();
    }

    @Override
    public AppEngInternalInventory getInternalInventory() {
        return this.inv;
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putString("ident", this.identifier);
        this.inv.writeToNBT(data, "inv");
        data.putString("watchedVar", this.watchedVar == null ? "" : this.watchedVar);
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        ItemStack stack = inv.getStackInSlot(slot);

        this.compiledTopLevel = null;
        this.luaGlobals = null;

        if (stack.isEmpty()) {
            updateRegistration();
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("lua")) {
            updateRegistration();
            return;
        }

        String luaSource = tag.getString("lua");
        if (luaSource.isEmpty()) {
            updateRegistration();
            return;
        }

        try {
            this.luaGlobals = JsePlatform.standardGlobals();
            bindLuaApi(this.luaGlobals);
            this.compiledTopLevel = this.luaGlobals.load(luaSource, "item_script");
            this.compiledTopLevel.call();

            LuaValue handler = this.luaGlobals.get("onVariable");
            if (handler.isnil() || !handler.isfunction()) {
                CrazyAddons.LOGGER.info("[DataProcessorBE:{}] Script compiled, but onVariable(name, value) not defined.", identifier);
            }

        } catch (Throwable t) {
            this.compiledTopLevel = null;
            this.luaGlobals = null;
            CrazyAddons.LOGGER.warn("[DataProcessorBE] Failed to load/execute Lua: {}", t.getMessage());
        } finally {
            updateRegistration();
        }
    }

    private void updateRegistration() {
        if (getMainNode() == null || getMainNode().getGrid() == null) return;
        var grid = getMainNode().getGrid();
        grid.getMachines(MEDataControllerBE.class).stream().findFirst().ifPresent(db -> {
            db.removeNotification(this.identifier);
            if (this.compiledTopLevel != null && this.watchedVar != null && !this.watchedVar.isBlank()) {
                db.registerNotification(this.identifier, this.watchedVar, this.identifier, this.getClass());
            }
        });
    }

    public void setWatchedVar(String watchedVar) {
        this.watchedVar = watchedVar == null ? "" : watchedVar;
        setChanged();
        updateRegistration();
    }

    public String getWatchedVar() {
        return this.watchedVar;
    }

    private void bindLuaApi(Globals g) {
        g.set("processor_id", LuaValue.valueOf(this.identifier));

        g.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue msg) {
                CrazyAddons.LOGGER.info("[Lua@{}] {}", identifier, msg.tojstring());
                return LuaValue.NIL;
            }
        });

        g.set("setVar", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue name, LuaValue value) {
                String n = name.isnil() ? "" : name.checkjstring();
                String v = value.isnil() ? "" : value.tojstring();
                if (n.isBlank()) return LuaValue.FALSE;

                if (getMainNode() == null || getMainNode().getGrid() == null) return LuaValue.FALSE;
                var grid = getMainNode().getGrid();
                var opt = grid.getMachines(MEDataControllerBE.class).stream().findFirst();
                if (opt.isEmpty()) return LuaValue.FALSE;

                try {
                    opt.get().addVariable(identifier, DataProcessorBE.class, identifier, n, v);
                    return LuaValue.TRUE;
                } catch (Throwable t) {
                    CrazyAddons.LOGGER.warn("[Lua@{}] setVar({}, ...) failed: {}", identifier, n, t.toString());
                    return LuaValue.FALSE;
                }
            }
        });

        g.set("setEmitter", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue name, LuaValue state) {
                String n = name.isnil() ? "" : name.checkjstring();
                boolean v = !state.isnil() && state.toboolean();
                if (n.isBlank()) return LuaValue.FALSE;

                if (getMainNode() == null || getMainNode().getGrid() == null) return LuaValue.FALSE;
                var grid = getMainNode().getGrid();

                try {
                    grid.getActiveMachines(RedstoneEmitterPart.class)
                        .stream().filter(part -> Objects.equals(part.name, n))
                        .findFirst().ifPresent(emitter -> emitter.setState(v));

                    return LuaValue.TRUE;
                } catch (Throwable t) {
                    CrazyAddons.LOGGER.warn("[Lua@{}] setEmitter({}, {}) failed: {}", identifier, name, state, t.toString());
                    return LuaValue.FALSE;
                }
            }
        });

        g.set("toggleEmitter", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue name) {
                String n = name.isnil() ? "" : name.checkjstring();
                if (n.isBlank()) return LuaValue.FALSE;

                if (getMainNode() == null || getMainNode().getGrid() == null) return LuaValue.FALSE;
                var grid = getMainNode().getGrid();

                try {
                    grid.getActiveMachines(RedstoneEmitterPart.class)
                            .stream().filter(part -> Objects.equals(part.name, n))
                            .findFirst().ifPresent(emitter -> emitter.setState(!emitter.getState()));

                    return LuaValue.TRUE;
                } catch (Throwable t) {
                    CrazyAddons.LOGGER.warn("[Lua@{}] toggleEmitter({}) failed: {}", identifier, name, t.toString());
                    return LuaValue.FALSE;
                }
            }
        });
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new DataProcessorMenu(pContainerId, pPlayerInventory, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.DATA_PROCESSOR_MENU.get(), player, locator);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Data Processor");
    }

    @Override
    public String getId() {
        return this.identifier;
    }

    @Override
    public void notifyVariable(String name, String value, MEDataControllerBE db) {
        if (this.watchedVar == null || this.watchedVar.isBlank() || !this.watchedVar.equals(name)) return;
        if (this.compiledTopLevel == null || this.luaGlobals == null) return;

        try {
            LuaValue handler = this.luaGlobals.get("onVariable");
            if (handler != null && handler.isfunction()) {
                handler.call(LuaValue.valueOf(name), LuaValue.valueOf(value));
            } else {
                CrazyAddons.LOGGER.info("[DataProcessorBE:{}] onVariable not defined.", identifier);
            }
        } catch (Throwable t) {
            CrazyAddons.LOGGER.warn("[DataProcessorBE] Error running Lua script for {}: {}", identifier, t.getMessage());
        }
    }
}
