package net.oktawia.crazyae2addons.datavariables.nodes.str;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class StringLengthNode implements IFlowNode {

    private IFlowNode next;

    public StringLengthNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        var in = inputs.get("in");
        if (in == null || in.getType() != DataType.STRING) return Map.of();

        int len = ((StringValue) in).getRaw().length();
        return Map.of("out", FlowResult.of(next, new IntValue(len)));
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
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
                "in", DataType.STRING
        );
    }

    static
    public String getDesc() {
        return "Returns the length of given String";
    }

    static
    public int getOutputPaths() {
        return 1;
    }

    static
    public List<?> getInputTypes() {
        return List.of(String.class);
    }
}
