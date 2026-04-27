package net.oktawia.crazyae2addons.logic.structuretool;

import appeng.api.storage.ISubMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.util.TemplateUtil;

import java.util.List;
import java.util.UUID;

public class PortableSpatialClonerHost extends StructureToolHost implements ISubMenuHost {

    public PortableSpatialClonerHost(Player player, int inventorySlot, ItemStack stack) {
        super(player, inventorySlot, stack);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.open(
                CrazyMenuRegistrar.PORTABLE_SPATIAL_CLONER_MENU.get(),
                player,
                MenuLocators.forHand(player, resolveHand(player))
        );
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyItemRegistrar.PORTABLE_SPATIAL_CLONER.get().getDefaultInstance();
    }

    @Override
    public boolean hasStoredStructure() {
        return StructureToolStackState.hasStructure(getItemStack());
    }

    @Override
    public byte[] getStructureBytes() {
        MinecraftServer server = getPlayer().getServer();

        if (server == null) {
            return null;
        }

        try {
            CompoundTag tag = ClonerStructureLibraryStore.loadSelectedOrMigrateLegacy(
                    server,
                    getPlayer().getUUID(),
                    getItemStack()
            );

            if (tag != null) {
                TemplateUtil.copyPreviewTransformState(tag, getItemStack().getOrCreateTag());
            }

            return tag == null ? null : TemplateUtil.compressNbt(tag);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void setStructureBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        MinecraftServer server = getPlayer().getServer();

        if (server == null) {
            return;
        }

        try {
            CompoundTag tag = TemplateUtil.decompressNbt(bytes);
            UUID owner = getPlayer().getUUID();

            String selectedId = StructureToolStackState.getStructureId(getItemStack());
            UUID selectedOwner = StructureToolStackState.getClonerLibraryOwner(getItemStack());

            ClonerStructureLibraryStore.Entry entry = null;

            if (!selectedId.isBlank()
                    && owner.equals(selectedOwner)
                    && ClonerStructureLibraryStore.exists(server, owner, selectedId)) {
                entry = ClonerStructureLibraryStore.saveExisting(
                        server,
                        owner,
                        selectedId,
                        tag
                );
            }

            if (entry == null) {
                entry = ClonerStructureLibraryStore.saveNew(
                        server,
                        owner,
                        tag,
                        null
                );
            }

            StructureToolStackState.setSelectedClonerLibraryEntry(
                    getItemStack(),
                    owner,
                    entry.id()
            );

            TemplateUtil.copyPreviewTransformState(tag, getItemStack().getOrCreateTag());

            if (getPlayer() instanceof ServerPlayer serverPlayer) {
                StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, tag);
            }
        } catch (Exception ignored) {
        }
    }

    public List<ClonerStructureLibraryStore.Entry> listLibraryEntries() {
        MinecraftServer server = getPlayer().getServer();

        if (server == null) {
            return List.of();
        }

        try {
            return ClonerStructureLibraryStore.list(server, getPlayer().getUUID());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public String getSelectedLibraryEntryId() {
        return StructureToolStackState.getStructureId(getItemStack());
    }

    public void selectEmpty() {
        StructureToolStackState.clearSelectedClonerLibraryEntry(getItemStack());

        TemplateUtil.setTemplateOffset(getItemStack().getOrCreateTag(), BlockPos.ZERO);
        TemplateUtil.setEnergyOrigin(getItemStack().getOrCreateTag(), BlockPos.ZERO);

        if (getPlayer() instanceof ServerPlayer serverPlayer) {
            StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
        }
    }

    public boolean selectLibraryEntry(String id) {
        if (id == null || id.isBlank()) {
            selectEmpty();
            return true;
        }

        MinecraftServer server = getPlayer().getServer();

        if (server == null) {
            return false;
        }

        UUID owner = getPlayer().getUUID();

        try {
            CompoundTag tag = ClonerStructureLibraryStore.load(server, owner, id);

            if (tag == null) {
                return false;
            }

            StructureToolStackState.setSelectedClonerLibraryEntry(getItemStack(), owner, id);
            TemplateUtil.copyPreviewTransformState(tag, getItemStack().getOrCreateTag());

            if (getPlayer() instanceof ServerPlayer serverPlayer) {
                StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, tag);
            }

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean renameLibraryEntry(String id, String name) {
        MinecraftServer server = getPlayer().getServer();

        if (server == null) {
            return false;
        }

        try {
            return ClonerStructureLibraryStore.rename(
                    server,
                    getPlayer().getUUID(),
                    id,
                    name
            );
        } catch (Exception ignored) {
            return false;
        }
    }

    private InteractionHand resolveHand(Player player) {
        ItemStack hostStack = getItemStack();

        if (player.getMainHandItem() == hostStack) {
            return InteractionHand.MAIN_HAND;
        }

        if (player.getOffhandItem() == hostStack) {
            return InteractionHand.OFF_HAND;
        }

        if (ItemStack.isSameItemSameTags(player.getMainHandItem(), hostStack)) {
            return InteractionHand.MAIN_HAND;
        }

        if (ItemStack.isSameItemSameTags(player.getOffhandItem(), hostStack)) {
            return InteractionHand.OFF_HAND;
        }

        return InteractionHand.MAIN_HAND;
    }
}