package net.oktawia.crazyae2addons.datavariables.nodes.bool;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class BoolConstNode implements IFlowNode {

    private final boolean value;
    private IFlowNode next;

    public BoolConstNode(boolean value) {
        this.value = value;
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        return Map.of(
            "out", FlowResult.of(next, new BoolValue(value))
        );
    }

    @Override
    public void setOutputNodes(List<IFlowNode> outputs) {
        if (!outputs.isEmpty()) this.next = outputs.get(0);
    }


    static public Map<String, String> getArgs() {
        return Map.of(
                "Value", "Boolean value this node will represent",
                "Next", "Name of the node that should be called next"
        );
    }

    static
    public String getDesc() {
        return "Simply a value";
    }

    static
    public int getOutputPaths() {
        return 1;
    }

    static
    public List<?> getInputTypes() {
        return List.of();
    }

    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of();
    }
}
