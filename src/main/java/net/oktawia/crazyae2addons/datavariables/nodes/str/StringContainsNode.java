package net.oktawia.crazyae2addons.datavariables.nodes.str;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class StringContainsNode implements IFlowNode {

    private IFlowNode onTrue;
    private IFlowNode onFalse;

    public StringContainsNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        var haystack = inputs.get("text");
        var needle = inputs.get("contains");

        if (haystack == null || needle == null ||
            haystack.getType() != DataType.STRING || needle.getType() != DataType.STRING) {
            return Map.of();
        }

        String value = ((StringValue) haystack).getRaw();
        String check = ((StringValue) needle).getRaw();
        boolean result = value.contains(check);

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
        return "Checks if string marked with ^B is present in the one marked as ^A";
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
                "contains", DataType.STRING
        );
    }
}