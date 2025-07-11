package net.oktawia.crazyae2addons.datavariables;

import java.util.List;

public record FlowResult(List<IFlowNode> nextNodes, DataValue<?> value) {
    public static FlowResult of(IFlowNode next, DataValue<?> value) {
        return new FlowResult(List.of(next), value);
    }

    public static FlowResult ofMany(List<IFlowNode> next, DataValue<?> value) {
        return new FlowResult(next, value);
    }
}