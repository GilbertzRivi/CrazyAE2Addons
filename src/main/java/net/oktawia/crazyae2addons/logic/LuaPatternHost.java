package net.oktawia.crazyae2addons.logic;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LuaPatternHost extends ItemMenuHost {
    public LuaPatternHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
        super(player, slot, itemStack);
    }

    public String getLua() {
        var stack = getItemStack();
        if (stack == null || stack.isEmpty()) return "";
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains("lua")) ? tag.getString("lua") : "";
    }

    public void setLua(@NotNull String src) {
        var stack = getItemStack();
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("lua", src);
        stack.setTag(tag);
    }

    public String getProgram() { return getLua(); }
    public void setProgram(String program) { setLua(program == null ? "" : program); }
}
