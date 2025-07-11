package net.oktawia.crazyae2addons.datavariables.nodes.bool;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class IntToBoolNode implements IFlowNode {

    private IFlowNode onTrue;
    private IFlowNode onFalse;

    public IntToBoolNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        DataValue<?> input = inputs.get("in");

        if (input == null || input.getType() != DataType.INT) return Map.of();

        int value = (Integer) input.getRaw();
        boolean result = value != 0;

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
        return "Takes INT as input, emits BOOL, true if input != 0, false otherwise";
    }

    static
    public int getOutputPaths() {
        return 2;
    }

    static
    public List<?> getInputTypes() {
        return List.of(Integer.class);
    }

    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
                "in", DataType.INT
        );
    }
}
