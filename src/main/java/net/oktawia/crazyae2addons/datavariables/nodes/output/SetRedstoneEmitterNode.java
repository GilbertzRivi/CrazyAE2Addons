package net.oktawia.crazyae2addons.datavariables.nodes.output;

import net.oktawia.crazyae2addons.datavariables.*;
import net.oktawia.crazyae2addons.parts.RedstoneEmitterPart;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SetRedstoneEmitterNode implements IFlowNode {

    private List<RedstoneEmitterPart> emitterRegistry;

    public SetRedstoneEmitterNode() {
    }

    public void setEmitters(List<RedstoneEmitterPart> emitterRegistry){
        this.emitterRegistry = emitterRegistry;
    }

    @Override
    public Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs) {
        var nameVal = inputs.get("name");
        var stateVal = inputs.get("state");

        if (nameVal == null || stateVal == null) return Map.of();
        if (nameVal.getType() != DataType.STRING || stateVal.getType() != DataType.BOOL) return Map.of();

        String emitterName = (String) nameVal.getRaw();
        boolean state = (Boolean) stateVal.getRaw();

        emitterRegistry
                .stream()
                .filter(emitter -> Objects.equals(emitter.name, emitterName))
                .findAny().ifPresent(part -> part.setState(state));

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
        return "Finds redstone emitter with the name == String argument, and sets its output to Boolean argument";
    }

    static
    public int getOutputPaths() {
        return 0;
    }

    static
    public List<?> getInputTypes() {
        return List.of(String.class, Boolean.class);
    }

    static
    public Map<String, DataType> getExpectedInputs() {
        return Map.of(
                "name", DataType.STRING,
                "state", DataType.BOOL
        );
    }
}
