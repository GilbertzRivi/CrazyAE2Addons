package net.oktawia.crazyae2addons.interfaces;

import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

public interface PenroseValidator {
    Map<String, List<Block>> getSymbols();
    List<List<String>> getLayers();
    int getOriginX();
    int getOriginY();
    int getOriginZ();
}