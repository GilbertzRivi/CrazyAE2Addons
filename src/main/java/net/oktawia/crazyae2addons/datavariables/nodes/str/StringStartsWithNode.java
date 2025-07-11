package net.oktawia.crazyae2addons.datavariables.nodes.str;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class StringStartsWithNode implements IFlowNode {

    private IFlowNode onTrue;
    private IFlowNode onFalse;

    public StringStartsWithNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        var str = inputs.get("text");
        var prefix = inputs.get("prefix");

        if (str == null || prefix == null || str.getType() != DataType.STRING || prefix.getType() != DataType.STRING)
            return Map.of();

        boolean result = ((StringValue) str).getRaw().startsWith(((StringValue) prefix).getRaw());

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
        return "Checks if string marked with ^A starts with the one marked as ^B";
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
                "text", DataType.STRING,
                "prefix", DataType.STRING
        );
    }
}
