package net.oktawia.crazyae2addons.datavariables.nodes.integer;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class IntSleepNode implements IFlowNode {

    private IFlowNode next;

    public IntSleepNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        DataValue<?> duration = inputs.get("duration");
        DataValue<?> payload = inputs.get("in");

        if (duration == null || duration.getType() != DataType.INT || payload == null)
            return Map.of();

        int ticks = ((IntValue) duration).getRaw();
        if (ticks <= 0) return Map.of("out", FlowResult.of(next, payload));

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return Map.of();

        long delayMillis = ticks * 50L;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> {
                    if (next != null) {
                        new DataFlowRunner(java.util.List.of(next))
                                .receiveInput(next, "in", payload);
                    }
                });
            }
        }, delayMillis);

        return Map.of();
    }

    @Override
    public void setOutputNodes(List<IFlowNode> outputs) {
        if (!outputs.isEmpty()) this.next = outputs.get(0);
    }

    static
    public Map<String, String> getArgs() {
        return Map.of(
                "Next", "Name of the node that should be called next"
        );
    }

    static
    public String getDesc() {
        return "Waits for the amount of ticks specified by the INT input marked ^A, and then emits INT input marked ^B";
    }

    static
    public int getOutputPaths() {
        return 1;
    }

    static
    public List<?> getInputTypes() {
        return List.of(Integer.class, Boolean.class);
    }

    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
                "duration", DataType.INT,
                "in", DataType.INT
        );
    }
}
