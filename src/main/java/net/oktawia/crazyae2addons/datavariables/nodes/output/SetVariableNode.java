package net.oktawia.crazyae2addons.datavariables.nodes.output;

import net.oktawia.crazyae2addons.datavariables.*;
import net.oktawia.crazyae2addons.entities.MEDataControllerBE;

import java.util.List;
import java.util.Map;

public class SetVariableNode implements IFlowNode {

    private MEDataControllerBE controller;
    private String id;

    public SetVariableNode() {
    }

    public void setController(MEDataControllerBE controller){
        this.controller = controller;
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        DataValue<?> value = inputs.get("in");
        DataValue<?> name = inputs.get("name");
        if (value == null) return Map.of();

        controller.addVariable(this.id, this.getClass(), this.id, (String) name.getRaw(), (String) value.getRaw());

        return Map.of();
    }

    @Override
    public void setOutputNodes(List<IFlowNode> outputs) {

    }

    static
    public Map<String, String> getArgs() {
        return Map.of();
    }

    static
    public String getDesc() {
        return "Sets new variable with named same as ^Name and sets its value to ^In";
    }

    static
    public int getOutputPaths() {
        return 0;
    }

    static
    public List<?> getInputTypes() {
        return List.of(String.class, String.class);
    }

    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
                "in", DataType.STRING,
                "name", DataType.STRING
        );
    }

    public void setId(String identifier) {
        this.id = identifier;
    }
}
