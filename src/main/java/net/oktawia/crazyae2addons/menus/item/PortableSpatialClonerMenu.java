package net.oktawia.crazyae2addons.menus.item;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftAmountMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerStructureLibraryStore;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolHost;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.structures.SyncClonerRequirementStatusPacket;
import net.oktawia.crazyae2addons.network.packets.structures.SyncClonerLibraryPacket;
import net.oktawia.crazyae2addons.util.StructureToolKeys;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PortableSpatialClonerMenu extends AbstractPortableStructureToolMenu {

    private static final String ACTION_CRAFT_REQUEST = "craft_request";

    private int requirementSyncTick = 0;

    public PortableSpatialClonerMenu(int id, Inventory playerInventory, StructureToolHost host) {
        super(CrazyMenuRegistrar.PORTABLE_SPATIAL_CLONER_MENU.get(), id, playerInventory, host);

        registerClientAction(ACTION_CRAFT_REQUEST, String.class, this::craftRequest);

        if (!isClientSide()) {
            syncRequirementsToClient();
            syncLibraryToClient();
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (!isClientSide()) {
            requirementSyncTick++;
            if (requirementSyncTick >= 20) {
                requirementSyncTick = 0;
                syncRequirementsToClient();
            }
        }
    }

    private void syncLibraryToClient() {
        if (!(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        try {
            NetworkHandler.sendToPlayer(
                    serverPlayer,
                    SyncClonerLibraryPacket.fromStoreEntries(
                            ClonerStructureLibraryStore.list(serverPlayer.server, serverPlayer.getUUID()),
                            StructureToolStackState.getStructureId(host.getItemStack())
                    )
            );
        } catch (Exception ignored) {
            NetworkHandler.sendToPlayer(
                    serverPlayer,
                    SyncClonerLibraryPacket.fromStoreEntries(
                            List.of(),
                            StructureToolStackState.getStructureId(host.getItemStack())
                    )
            );
        }
    }

    public void craftRequest(String format) {
        if (isClientSide()) {
            sendClientAction(ACTION_CRAFT_REQUEST, format);
        } else {
            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(format.split("\\|")[0]));
            if (item != null) {
                CraftAmountMenu.open(
                        (ServerPlayer) getPlayer(),
                        MenuLocators.forHand(getPlayer(), getPlayer().swingingArm),
                        AEItemKey.of(item),
                        Integer.parseInt(format.split("\\|")[1])
                );
            }
        }
    }

    private void syncRequirementsToClient() {
        if (!(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NetworkHandler.sendToPlayer(
                serverPlayer,
                new SyncClonerRequirementStatusPacket(this.containerId, buildRequirementEntries())
        );
    }

    private List<SyncClonerRequirementStatusPacket.Entry> buildRequirementEntries() {
        if (!host.hasStoredStructure()) {
            return List.of();
        }

        byte[] bytes = host.getStructureBytes();
        if (bytes == null || bytes.length == 0) {
            return List.of();
        }

        CompoundTag structureTag;
        try {
            structureTag = TemplateUtil.decompressNbt(bytes);
        } catch (Exception ignored) {
            return List.of();
        }

        if (!structureTag.contains(StructureToolKeys.CLONE_METADATA_KEY, Tag.TAG_COMPOUND)) {
            return List.of();
        }

        CompoundTag metadata = structureTag.getCompound(StructureToolKeys.CLONE_METADATA_KEY);
        if (!metadata.contains(StructureToolKeys.CLONE_REQUIREMENTS_KEY, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag requirements = metadata.getList(
                StructureToolKeys.CLONE_REQUIREMENTS_KEY,
                Tag.TAG_COMPOUND
        );

        IGrid grid = resolveGrid();
        MEStorage meStorage = null;
        IActionSource actionSource = null;

        if (grid != null) {
            try {
                meStorage = grid.getStorageService().getInventory();
                actionSource = IActionSource.ofPlayer(getPlayer());
            } catch (Exception ignored) {
                meStorage = null;
                actionSource = null;
            }
        }

        List<SyncClonerRequirementStatusPacket.Entry> out = new ArrayList<>();

        for (int i = 0; i < requirements.size(); i++) {
            CompoundTag row = requirements.getCompound(i);

            if (!row.contains(StructureToolKeys.CLONE_KEY_STACK, Tag.TAG_COMPOUND)) {
                continue;
            }

            ItemStack stack = ItemStack.of(row.getCompound(StructureToolKeys.CLONE_KEY_STACK));
            if (stack.isEmpty()) {
                continue;
            }

            stack = stack.copy();
            stack.setCount(1);

            long required = Math.max(1L, row.getLong(StructureToolKeys.CLONE_KEY_COUNT));
            long available = countPlayerInventory(stack);

            AEItemKey key = AEItemKey.of(stack);
            boolean craftable = false;

            if (key != null && meStorage != null && actionSource != null) {
                try {
                    available += meStorage.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
                } catch (Exception ignored) {
                }

                try {
                    craftable = grid != null && grid.getCraftingService().isCraftable(key);
                } catch (Exception ignored) {
                    craftable = false;
                }
            }

            out.add(new SyncClonerRequirementStatusPacket.Entry(
                    stack,
                    available,
                    required,
                    craftable
            ));
        }

        return out;
    }

    private long countPlayerInventory(ItemStack targetStack) {
        AEItemKey targetKey = AEItemKey.of(targetStack);
        if (targetKey == null) {
            return 0L;
        }

        long total = 0L;
        var inventory = getPlayer().getInventory();

        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty() && targetKey.equals(AEItemKey.of(stack))) {
                total += stack.getCount();
            }
        }

        for (ItemStack stack : inventory.offhand) {
            if (!stack.isEmpty() && targetKey.equals(AEItemKey.of(stack))) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private @Nullable IGrid resolveGrid() {
        ItemStack stack = host.getItemStack();
        if (!(stack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem)) {
            return null;
        }

        try {
            return wirelessTerminalItem.getLinkedGrid(stack, getPlayer().level(), getPlayer());
        } catch (Exception ignored) {
            return null;
        }
    }
}