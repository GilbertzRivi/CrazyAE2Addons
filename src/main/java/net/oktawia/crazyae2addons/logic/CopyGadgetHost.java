package net.oktawia.crazyae2addons.logic;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CopyGadgetHost extends ItemMenuHost {
    public CopyGadgetHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
        super(player, slot, itemStack);
    }

    public String getProgram() {
        ItemStack stack = getItemStack();
        if (!stack.hasTag() || !stack.getTag().getBoolean("code")) {
            return "";
        }
        String programId = stack.getTag().getString("program_id");
        if (programId.isEmpty()) {
            return "";
        }
        MinecraftServer server = getPlayer().getServer();
        if (server == null) {
            return "";
        }
        return BuilderPatternHost.loadProgramFromFile(stack, server);
    }

    public void setProgram(String newCode) {
        ItemStack stack = getItemStack();
        var tag = stack.getOrCreateTag();

        if (!tag.contains("program_id") || tag.getString("program_id").isEmpty()) {
            tag.putString("program_id", UUID.randomUUID().toString());
        }
        String programId = tag.getString("program_id");

        tag.putBoolean("code", true);

        MinecraftServer server = getPlayer().getServer();
        if (server == null) return;

        try {
            Path file = server.getWorldPath(new LevelResource("serverdata"))
                    .resolve("autobuilder")
                    .resolve(programId);
            Files.createDirectories(file.getParent());
            Files.writeString(file, newCode, UTF_8);
        } catch (Exception e) {
            LogUtils.getLogger().warn("Failed to save program for gadget: " + e);
        }
    }
}
