package net.oktawia.crazyae2addons.logic.builder;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.components.BuilderPatternData;
import net.oktawia.crazyae2addons.defs.regs.CrazyDataComponents;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.BuilderPatternItem;
import net.oktawia.crazyae2addons.misc.ProgramExpander;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class BuilderPatternHost extends ItemMenuHost<BuilderPatternItem> implements ISubMenuHost {

    private boolean code;
    private int delay = 0;

    public BuilderPatternHost(BuilderPatternItem item, Player player, ItemMenuHostLocator locator) {
        super(item, player, locator);
        var data = getItemStack().get(CrazyDataComponents.BUILDER_PATTERN_DATA.get());
        if (data != null) {
            this.code = data.hasCode();
            this.delay = data.delay();
        }
    }

    public String getProgram() {
        return loadProgramFromFile(this.getItemStack(), getPlayer().getServer());
    }

    public int getDelay() { return this.delay; }

    public void setProgram(String program) {
        ProgramExpander.Result result = ProgramExpander.expand(program);
        this.code = result.success;

        var existing = getItemStack().getOrDefault(CrazyDataComponents.BUILDER_PATTERN_DATA.get(), BuilderPatternData.DEFAULT);
        String id = existing.hasCode() ? existing.programId() : UUID.randomUUID().toString();

        if (result.success) {
            saveProgramToFile(id, program, getPlayer().getServer());
        }
        getItemStack().set(CrazyDataComponents.BUILDER_PATTERN_DATA.get(),
                new BuilderPatternData(result.success ? id : "", existing.delay(), existing.srcFacing()));
    }

    public void setDelay(int delay) {
        this.delay = delay;
        var existing = getItemStack().getOrDefault(CrazyDataComponents.BUILDER_PATTERN_DATA.get(), BuilderPatternData.DEFAULT);
        getItemStack().set(CrazyDataComponents.BUILDER_PATTERN_DATA.get(),
                new BuilderPatternData(existing.programId(), delay, existing.srcFacing()));
    }

    public static String loadProgramFromFile(ItemStack stack, MinecraftServer server) {
        try {
            if (server == null || stack == null || stack.isEmpty()) return "";
            var data = stack.get(CrazyDataComponents.BUILDER_PATTERN_DATA.get());
            if (data == null || !data.hasCode()) return "";
            String id = data.programId();
            Path file = server.getWorldPath(new LevelResource("serverdata"))
                    .resolve("autobuilder").resolve(id);
            if (!Files.exists(file)) return "";
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (Exception e) {
            CrazyAddons.LOGGER.debug("failed to read builder pattern file", e);
            return "";
        }
    }

    public static void saveProgramToFile(String id, String code, MinecraftServer server) {
        Path file = server.getWorldPath(new LevelResource("serverdata"))
                .resolve("autobuilder").resolve(id);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, code, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogUtils.getLogger().info(e.toString());
        }
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.open(CrazyMenuRegistrar.BUILDER_PATTERN_MENU.get(), player, MenuLocators.forHand(player, player.getUsedItemHand()), true);
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyItemRegistrar.BUILDER_PATTERN.toStack();
    }
}
