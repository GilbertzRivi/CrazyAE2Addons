package net.oktawia.crazyae2addons.compat.CC;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.entities.PenroseControllerBE;
import net.oktawia.crazyae2addons.entities.PenroseFrameBE;

import java.util.HashMap;
import java.util.Map;

public final class PenroseFramePeripheral implements GenericPeripheral {

    @Override
    public String id() {
        return new ResourceLocation(CrazyAddons.MODID, "penrose_frame").toString();
    }

    private static PenroseControllerBE ctrl(PenroseFrameBE frame) {
        return frame != null ? frame.controller : null;
    }

    private static String s(long v) { return Long.toString(v); }

    @LuaFunction(mainThread = true)
    public final boolean hasController(PenroseFrameBE frame) {
        return ctrl(frame) != null;
    }

    @LuaFunction(mainThread = true)
    public final boolean isActive(PenroseFrameBE frame) {
        var c = ctrl(frame);
        return c != null && c.isBlackHoleActive();
    }

    @LuaFunction(mainThread = true)
    public final String getStoredEnergy(PenroseFrameBE frame) {
        var c = ctrl(frame);
        return c == null ? "0" : s(c.getStoredEnergy());
    }

    @LuaFunction(mainThread = true)
    public final String getStoredEnergyInDisk(PenroseFrameBE frame) {
        var c = ctrl(frame);
        return c == null ? "0" : s(c.getStoredEnergyInDisk());
    }

    @LuaFunction(mainThread = true)
    public final double getHeat(PenroseFrameBE frame) {
        var c = ctrl(frame);
        return c == null ? 0.0 : c.getHeat();
    }

    @LuaFunction(mainThread = true)
    public final String getMass(PenroseFrameBE frame) {
        var c = ctrl(frame);
        return c == null ? "0" : s(c.getBlackHoleMass());
    }

    @LuaFunction(mainThread = true)
    public final String getLastGeneratedFePerTickGross(PenroseFrameBE frame) {
        var c = ctrl(frame);
        return c == null ? "0" : s(c.getLastGeneratedFePerTickGross());
    }

    @LuaFunction(mainThread = true)
    public final String getLastConsumedFePerTick(PenroseFrameBE frame) {
        var c = ctrl(frame);
        return c == null ? "0" : s(c.getLastConsumedFePerTick());
    }

    @LuaFunction(mainThread = true)
    public final String getLastSecondMassDelta(PenroseFrameBE frame) {
        var c = ctrl(frame);
        return c == null ? "0" : s(c.getLastSecondMassDelta());
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getStatus(PenroseFrameBE frame) {
        var c = ctrl(frame);
        Map<String, Object> out = new HashMap<>();
        out.put("attached", c != null);

        if (c == null) return out;

        out.put("active", c.isBlackHoleActive());
        out.put("storedEnergy", s(c.getStoredEnergy()));
        out.put("storedEnergyInDisk", s(c.getStoredEnergyInDisk()));
        out.put("heat", c.getHeat());
        out.put("mass", s(c.getBlackHoleMass()));
        out.put("lastGeneratedFePerTickGross", s(c.getLastGeneratedFePerTickGross()));
        out.put("lastConsumedFePerTick", s(c.getLastConsumedFePerTick()));
        out.put("lastSecondMassDelta", s(c.getLastSecondMassDelta()));
        return out;
    }
}
