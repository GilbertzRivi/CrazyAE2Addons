package net.oktawia.crazyae2addons.datavariables.nodes.str;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class StringConcatNode implements IFlowNode {

    private IFlowNode next;

    public StringConcatNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        DataValue<?> a = inputs.get("a");
        DataValue<?> b = inputs.get("b");

        if (a == null || b == null || a.getType() != DataType.STRING || b.getType() != DataType.STRING) return Map.of();

        String out = ((StringValue) a).getRaw() + ((StringValue) b).getRaw();
        return Map.of("out", FlowResult.of(next, new StringValue(out)));
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
        return "Appends string marked with ^B at the end of the string marked with ^A";
    }

    static
    public int getOutputPaths() {
        return 1;
    }

    static
    public List<?> getInputTypes() {
        return List.of(String.class, String.class);
    }

    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
                "a", DataType.STRING,
                "b", DataType.STRING
        );
    }
}
