package net.oktawia.crazyae2addons.menus;

import appeng.api.stacks.AEItemKey;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftAmountMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.BuilderPatternItem;
import net.oktawia.crazyae2addons.items.PortableAutobuilder;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.BuilderPatternHost;
import net.oktawia.crazyae2addons.logic.CopyGadgetHost;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.SendLongStringToClientPacket;
import net.oktawia.crazyae2addons.network.SendLongStringToServerPacket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PortableAutobuilderMenu extends AEBaseMenu {
    public static final String SEND_DATA = "SendData";
    public static final String REQUEST_DATA = "requestData";
    public static final String FLIP_H = "flipH";
    public static final String FLIP_V = "flipV";
    public static final String ROTATE = "rotateCW";
    public static final String ACTION_UPDATE = "actionUpdate";
    public static final String CRAFT = "craft";
    public static final String CLEAR = "clear";

    public String program = "";

    public CopyGadgetHost host;

    @GuiSync(239)
    public String name = "";

    @GuiSync(240) //
    public String requirements = "";

    public PortableAutobuilderMenu(int id, Inventory playerInventory, CopyGadgetHost host) {
        super(CrazyMenuRegistrar.COPY_GADGET_MENU.get(), id, playerInventory, host);
        setupUpgrades(host.getUpgrades());
        registerClientAction(SEND_DATA, String.class, this::updateData);
        registerClientAction(REQUEST_DATA, this::requestData);
        registerClientAction(FLIP_H, this::flipH);
        registerClientAction(FLIP_V, this::flipV);
        registerClientAction(ROTATE, Integer.class, this::rotateCW);
        registerClientAction(ACTION_UPDATE, this::updateRequirements);
        registerClientAction(CRAFT, String.class, this::craftRequest);
        registerClientAction(CLEAR, this::clearStructure);

        this.host = host;
        this.name = host.getItemStack().getDisplayName().getString()
                .substring(1, host.getItemStack().getDisplayName().getString().length() - 1);

        this.createPlayerInventorySlots(playerInventory);

        if (!isClientSide()) {
            this.program = host.getProgram();
        }
        requestData();
        updateRequirements();
    }

    public void updateRequirements() {
        if (isClientSide()) {
            sendClientAction(ACTION_UPDATE);
        } else {
            var player = getPlayer();
            Level level = player.level();
            this.requirements = PortableAutobuilder.getRequirementsString(host.getItemStack(), level, player);
        }
    }

    public void requestData() {
        if (isClientSide()) {
            sendClientAction(REQUEST_DATA);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            ServerPlayer sp = (ServerPlayer) getPlayer();

            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SendLongStringToClientPacket("__RESET__")
            );

            byte[] bytes = program.getBytes(StandardCharsets.UTF_8);
            int maxSize = 1000 * 1000;
            int total = (int) Math.ceil((double) bytes.length / maxSize);

            for (int i = 0; i < total; i++) {
                int start = i * maxSize;
                int end = Math.min(bytes.length, (i + 1) * maxSize);
                byte[] part = Arrays.copyOfRange(bytes, start, end);
                String partString = new String(part, StandardCharsets.UTF_8);

                NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> sp),
                        new SendLongStringToClientPacket(partString)
                );
            }
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SendLongStringToClientPacket("__END__")
            );
        }
    }

    public void flipH() {
        if (isClientSide()) {
            sendClientAction(FLIP_H);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            ItemStack s = host.getItemStack();

            BuilderPatternItem.applyFlipHorizontalToItem(s, getPlayer().getServer(), getPlayer());

            String full = BuilderPatternHost.loadProgramFromFile(s, getPlayer().getServer());

            this.host.setProgram(full);
            this.program = full;

            PortableSpatialStorage.rebuildPreviewFromCode(s, getPlayer().getServer(), full);

            requestData();
        }
    }

    public void flipV() {
        if (isClientSide()) {
            sendClientAction(FLIP_V);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            ItemStack s = host.getItemStack();

            BuilderPatternItem.applyFlipVerticalToItem(s, getPlayer().getServer(), getPlayer());

            String full = BuilderPatternHost.loadProgramFromFile(s, getPlayer().getServer());

            this.host.setProgram(full);
            this.program = full;

            PortableSpatialStorage.rebuildPreviewFromCode(s, getPlayer().getServer(), full);

            requestData();
        }
    }


    public void rotateCW(Integer times) {
        int t = times == null ? 1 : times;
        if (isClientSide()) {
            sendClientAction(ROTATE, t);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            ItemStack s = host.getItemStack();

            BuilderPatternItem.applyRotateCWToItem(s, getPlayer().getServer(), t, getPlayer());

            String full = BuilderPatternHost.loadProgramFromFile(s, getPlayer().getServer());

            this.host.setProgram(full);
            this.program = full;

            PortableSpatialStorage.rebuildPreviewFromCode(s, getPlayer().getServer(), full);

            requestData();
        }
    }


    public void updateData(String program) {
        this.program = program;
        if (isClientSide()) {
            NetworkHandler.INSTANCE.sendToServer(new SendLongStringToServerPacket(this.program));
        } else {
            this.host.setProgram(program);
            this.program = host.getProgram();
            updateRequirements();
            requestData();
        }
    }

    public void craftRequest(String format) {
        if (isClientSide()){
            sendClientAction(CRAFT, format);
        } else {
            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(format.split("\\|")[0]));
            if (item != null){
                CraftAmountMenu.open(
                        (ServerPlayer) getPlayer(),
                        MenuLocators.forHand(getPlayer(), getPlayer().swingingArm),
                        AEItemKey.of(item),
                        Integer.parseInt(format.split("\\|")[1])
                );
            }
        }
    }

    public void clearStructure() {
        if (isClientSide()){
            sendClientAction(CLEAR);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            ItemStack s = host.getItemStack();
            this.host.setProgram("");
            this.program = "";
            PortableSpatialStorage.rebuildPreviewFromCode(s, getPlayer().getServer(), "");
            updateRequirements();
            requestData();
        }
    }
}
