package net.oktawia.crazyae2addons.client.renderer.message;

import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientHotbarMessage {

    public record Line(Component text, int color) {}

    private static final int MAX_LINES = 3;

    private static final List<Line> LINES = new ArrayList<>();
    @Getter
    private static int ticksLeft = 0;

    private ClientHotbarMessage() {
    }

    public static Line line(Component text, int color) {
        return new Line(text, color);
    }

    public static void show(int durationTicks, Line... lines) {
        LINES.clear();

        if (lines == null || lines.length == 0 || durationTicks <= 0) {
            ticksLeft = 0;
            return;
        }

        for (int i = 0; i < lines.length && i < MAX_LINES; i++) {
            if (lines[i] != null && lines[i].text() != null) {
                LINES.add(lines[i]);
            }
        }

        ticksLeft = LINES.isEmpty() ? 0 : durationTicks;
    }

    public static void show(int durationTicks, int color, Component... lines) {
        LINES.clear();

        if (lines == null || lines.length == 0 || durationTicks <= 0) {
            ticksLeft = 0;
            return;
        }

        for (int i = 0; i < lines.length && i < MAX_LINES; i++) {
            if (lines[i] != null) {
                LINES.add(new Line(lines[i], color));
            }
        }

        ticksLeft = LINES.isEmpty() ? 0 : durationTicks;
    }

    public static void clear() {
        LINES.clear();
        ticksLeft = 0;
    }

    public static void tick() {
        if (ticksLeft > 0) {
            ticksLeft--;
            if (ticksLeft <= 0) {
                clear();
            }
        }
    }

    public static boolean isVisible() {
        return ticksLeft > 0 && !LINES.isEmpty();
    }

    public static List<Line> getLines() {
        return Collections.unmodifiableList(LINES);
    }

}