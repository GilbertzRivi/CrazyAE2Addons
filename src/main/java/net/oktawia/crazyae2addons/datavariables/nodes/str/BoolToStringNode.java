package net.oktawia.crazyae2addons.datavariables.nodes.str;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class BoolToStringNode implements IFlowNode {

    private IFlowNode next;

    public BoolToStringNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        DataValue<?> input = inputs.get("in");
        if (input == null || input.getType() != DataType.BOOL) return Map.of();

        String str = String.valueOf(((BoolValue) input).getRaw());
        return Map.of("out", FlowResult.of(next, new StringValue(str)));
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
        return "Takes in a Boolean, outputs a String";
    }

    static
    public int getOutputPaths() {
        return 1;
    }

    static
    public List<?> getInputTypes() {
        return List.of(Boolean.class);
    }
    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
                "in", DataType.BOOL
        );
    }

}
