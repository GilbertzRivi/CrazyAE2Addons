package net.oktawia.crazyae2addons.datavariables.nodes.str;

import net.oktawia.crazyae2addons.datavariables.*;
import net.oktawia.crazyae2addons.entities.MEDataControllerBE;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ReadVariableNode implements IFlowNode {

    private MEDataControllerBE controller;
    private IFlowNode next;

    public ReadVariableNode() {
    }

    public void setController(MEDataControllerBE controller){
        this.controller = controller;
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        AtomicReference<Optional<String>> variables = new AtomicReference<>();
        inputs.values().stream().findFirst().ifPresent(val -> {
           variables.set(controller.variables
                   .values()
                   .stream()
                   .filter(rec -> rec.name().equals(val.getRaw()))
                   .map(MEDataControllerBE.VariableRecord::value)
                   .filter(Objects::nonNull)
                   .findFirst());
        });
        return variables.get().map(s -> Map.of("out", FlowResult.of(next, new StringValue(s)))).orElseGet(Map::of);
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
        return "Looks for a variable in the network storage, and returns its value";
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
        return Map.of();
    }
}
