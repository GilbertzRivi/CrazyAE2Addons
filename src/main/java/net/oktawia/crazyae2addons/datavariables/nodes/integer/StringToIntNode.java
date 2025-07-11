package net.oktawia.crazyae2addons.datavariables.nodes.integer;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class StringToIntNode implements IFlowNode {

    private IFlowNode next;

    public StringToIntNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        var input = inputs.get("in");

        if (input == null || input.getType() != DataType.STRING) return Map.of();

        try {
            double parsed = Double.parseDouble((String) input.getRaw());
            int result = (int) parsed;
            return Map.of("out", FlowResult.of(next, new IntValue(result)));
        } catch (Exception ignored) {
            return Map.of();
        }
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
        return "Takes String as input, emits equivalent INT if the conversion was successful";
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
                "in", DataType.STRING
        );
    }
}