package net.oktawia.crazyae2addons.datavariables.nodes.str;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class StringEndsWithNode implements IFlowNode {

    private IFlowNode onTrue;
    private IFlowNode onFalse;

    public StringEndsWithNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        var str = inputs.get("text");
        var suffix = inputs.get("suffix");

        if (str == null || suffix == null || str.getType() != DataType.STRING || suffix.getType() != DataType.STRING)
            return Map.of();

        boolean result = ((StringValue) str).getRaw().endsWith(((StringValue) suffix).getRaw());

        return Map.of(
            result ? "true" : "false",
            FlowResult.of(result ? onTrue : onFalse, new BoolValue(result))
        );
    }

    @Override
    public void setOutputNodes(List<IFlowNode> outputs) {
        if (!outputs.isEmpty()) this.onTrue = outputs.get(0);
        if (outputs.size() > 1) this.onFalse = outputs.get(1);
    }

    static
    public Map<String, String> getArgs() {
        return Map.of(
                "onTrue", "Name of the node that should be called on true",
                "onFalse", "Name of the node that should be called on false"
        );
    }

    static
    public String getDesc() {
        return "Checks if string marked with ^A ends with the one marked as ^B";
    }

    static
    public int getOutputPaths() {
        return 2;
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
