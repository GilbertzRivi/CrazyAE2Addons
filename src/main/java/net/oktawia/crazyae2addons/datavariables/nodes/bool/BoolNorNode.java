package net.oktawia.crazyae2addons.datavariables.nodes.bool;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class BoolNorNode implements IFlowNode {

    private IFlowNode onTrue;
    private IFlowNode onFalse;

    public BoolNorNode() {
    }


    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        var a = inputs.get("a");
        var b = inputs.get("b");

        if (a == null || b == null || a.getType() != DataType.BOOL || b.getType() != DataType.BOOL) {
            return Map.of();
        }

        boolean result = !((Boolean) a.getRaw() || (Boolean) b.getRaw());
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
        return "Compares two boolean values and gives true if only one is true, false otherwise";
    }

    static
    public int getOutputPaths() {
        return 2;
    }

    static
    public List<?> getInputTypes() {
        return List.of(Boolean.class, Boolean.class);
    }

    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
                "a", DataType.BOOL,
                "b", DataType.BOOL
        );
    }
}
