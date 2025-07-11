package net.oktawia.crazyae2addons.datavariables.nodes.str;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class StringConstNode implements IFlowNode {

    private final String value;
    private IFlowNode next;

    public StringConstNode(String value) {
        this.value = value;
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        return Map.of(
            "out", FlowResult.of(next, new StringValue(value))
        );
    }

    @Override
    public void setOutputNodes(List<IFlowNode> outputs) {
        if (!outputs.isEmpty()) this.next = outputs.get(0);
    }

    static
    public Map<String, String> getArgs() {
        return Map.of(
                "Value", "String value this node will represent",
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
        return List.of(String.class);
    }
    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
        );
    }
}
