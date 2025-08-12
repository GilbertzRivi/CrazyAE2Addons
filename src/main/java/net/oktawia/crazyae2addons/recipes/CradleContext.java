package net.oktawia.crazyae2addons.recipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CradleContext implements Container {
    private final Level level;
    private final BlockPos origin;
    private final Direction facing;

    public CradleContext(Level level, BlockPos origin, Direction facing) {
        this.level = level;
        this.origin = origin;
        this.facing = facing;
    }

    public Level level() { return level; }
    public BlockPos origin() { return origin; }
    public Direction facing() { return facing; }

    @Override public int getContainerSize() { return 0; }
    @Override public boolean isEmpty() { return true; }
    @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) {}
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() {}
}
