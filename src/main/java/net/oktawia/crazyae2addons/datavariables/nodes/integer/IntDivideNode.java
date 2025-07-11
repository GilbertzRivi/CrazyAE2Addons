package net.oktawia.crazyae2addons.datavariables.nodes.integer;

import net.oktawia.crazyae2addons.datavariables.*;

import java.util.List;
import java.util.Map;

public class IntDivideNode implements IFlowNode {

    private IFlowNode next;

    public IntDivideNode() {
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        DataValue<?> a = inputs.get("a");
        DataValue<?> b = inputs.get("b");

        if (a == null || b == null || a.getType() != DataType.INT || b.getType() != DataType.INT)
            return Map.of();

        int result = ((IntValue) a).getRaw() / ((IntValue) b).getRaw();
        return Map.of("out", FlowResult.of(next, new IntValue(result)));
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
        return "Divides INT marked with \"^A\", by and INT marked by \"^B\", (truncates reminder)";
    }

    static
    public int getOutputPaths() {
        return 1;
    }

    static
    public List<?> getInputTypes() {
        return List.of(Integer.class, Integer.class);
    }

    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
                "a", DataType.INT,
                "b", DataType.INT
        );
    }
}
