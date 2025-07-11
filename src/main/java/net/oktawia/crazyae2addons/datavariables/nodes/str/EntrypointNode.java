package net.oktawia.crazyae2addons.datavariables.nodes.str;

import net.oktawia.crazyae2addons.datavariables.*;
import net.oktawia.crazyae2addons.entities.MEDataControllerBE;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class EntrypointNode implements IFlowNode {

    private String value;
    private final String valueName;
    private IFlowNode next;

    public EntrypointNode(String valueName) {
        this.valueName = valueName;
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {

        return Map.of("out", FlowResult.of(next, new StringValue(value)));
    }

    @Override
    public void setOutputNodes(List<IFlowNode> outputs) {
        if (!outputs.isEmpty()) this.next = outputs.get(0);
    }

    public void setValue(String value){
        this.value = value;
    }

    public String getValueName(){
        return this.valueName;
    }

    static
    public Map<String, String> getArgs() {
        return Map.of(
                "Next", "Name of the node that should be called next",
                "Value", "New value that starts the data flow"
        );
    }

    static
    public String getDesc() {
        return "Starts the data flow";
    }

    static
    public int getOutputPaths() {
        return 1;
    }

    static
    public List<?> getInputTypes() {
        return List.of();
    }

    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
        );
    }
}
