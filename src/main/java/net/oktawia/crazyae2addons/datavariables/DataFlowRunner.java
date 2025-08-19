package net.oktawia.crazyae2addons.datavariables;

import appeng.api.networking.IGrid;
import com.mojang.logging.LogUtils;
import net.oktawia.crazyae2addons.datavariables.nodes.output.SetRedstoneEmitterNode;
import net.oktawia.crazyae2addons.datavariables.nodes.output.SetVariableNode;
import net.oktawia.crazyae2addons.datavariables.nodes.str.EntrypointNode;
import net.oktawia.crazyae2addons.datavariables.nodes.str.ReadVariableNode;
import net.oktawia.crazyae2addons.entities.MEDataControllerBE;
import net.oktawia.crazyae2addons.parts.RedstoneEmitterPart;
import org.slf4j.Logger;

import java.util.*;

public class DataFlowRunner {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<IFlowNode> allNodes;
    private final Map<IFlowNode, Map<String, DataValue<?>>> inputBuffers = new HashMap<>();
    private final Set<IFlowNode> executed = new HashSet<>();

    public DataFlowRunner(List<IFlowNode> allNodes) {
        this.allNodes = allNodes;
    }

    public void run(String startValue, String identifier, IGrid grid) {
        List<MEDataControllerBE> dataControllers = grid != null ? grid.getMachines(MEDataControllerBE.class).stream().toList() : List.of();
        MEDataControllerBE controller = dataControllers.isEmpty() ? null : dataControllers.get(0);

        List<RedstoneEmitterPart> emitters = grid != null ? grid.getMachines(RedstoneEmitterPart.class).stream().toList() : List.of();

        for (IFlowNode node : allNodes) {
            try {
                if (node instanceof EntrypointNode ep) {
                    ep.setValue(startValue);
                } else if (node instanceof SetVariableNode sv) {
                    sv.setId(identifier);
                    if (controller != null) sv.setController(controller);
                } else if (node instanceof SetRedstoneEmitterNode re) {
                    if (!emitters.isEmpty()) re.setEmitters(emitters);
                } else if (node instanceof ReadVariableNode rv) {
                    if (controller != null) rv.setController(controller);
                }
            } catch (Exception ignored) {}
        }

        run();
    }

    private void run() {
        for (IFlowNode node : allNodes) {
            Map<String, DataType> expectedInputs = FlowNodeRegistry.getExpectedInputs(node.getClass());
            if (expectedInputs.isEmpty()) {
                tryExecute(node, "start", Map.of());
            }
        }
    }

    public void receiveInput(IFlowNode node, String inputName, DataValue<?> value) {
        if (node == null || inputName == null || value == null) return;

        Map<String, DataType> expected = FlowNodeRegistry.getExpectedInputs(node.getClass());
        DataType expectedType = expected.get(inputName);

        DataValue<?> toStore = value;

        if (expectedType != null && value.getType() != expectedType) {
            if (TypeConverters.canConvert(value.getType(), expectedType)) {
                Optional<DataValue<?>> converted = TypeConverters.convert(value, expectedType);
                if (converted.isPresent()) {
                    toStore = converted.get();
                } else {
                    LOGGER.warn("Nie udało się skonwertować wartości typu {} do {} dla wejścia '{}' w nodzie {}. Wartość zostaje odrzucona.",
                            value.getType(), expectedType, inputName, node.getClass().getSimpleName());
                    return; // nie dostarczaj błędnej wartości
                }
            } else {
                LOGGER.warn("Brak możliwości konwersji wartości typu {} do {} dla wejścia '{}' w nodzie {}. Wartość zostaje odrzucona.",
                        value.getType(), expectedType, inputName, node.getClass().getSimpleName());
                return;
            }
        }

        inputBuffers
                .computeIfAbsent(node, n -> new HashMap<>())
                .put(inputName, toStore);

        tryExecute(node, inputName, inputBuffers.get(node));
    }

    private void tryExecute(IFlowNode node, String lastInput, Map<String, DataValue<?>> currentInputs) {
        if (node == null || executed.contains(node)) return;

        Map<String, DataType> expected = FlowNodeRegistry.getExpectedInputs(node.getClass());

        if (expected.isEmpty()) {
            try {
                Map<String, FlowResult> results = node.execute(lastInput, Map.of());
                executed.add(node);
                dispatchResults(results);
            } catch (Exception e) {
                LOGGER.error("Błąd podczas wykonywania noda: " + node.getClass().getSimpleName(), e);
            }
            return;
        }

        if (currentInputs == null) return;

        boolean ready = expected.keySet().stream().allMatch(currentInputs::containsKey);
        if (!ready) return;

        inputBuffers.remove(node);
        executed.add(node);

        try {
            Map<String, FlowResult> results = node.execute(lastInput, currentInputs);
            dispatchResults(results);
        } catch (Exception e) {
            LOGGER.error("Błąd w execute() dla: " + node.getClass().getSimpleName(), e);
        }
    }

    private void dispatchResults(Map<String, FlowResult> results) {
        if (results == null) return;

        for (Map.Entry<String, FlowResult> entry : results.entrySet()) {
            String pathName = entry.getKey();
            FlowResult res = entry.getValue();
            if (res == null || res.nextNodes() == null) continue;

            String inputOverride = null;
            int caretIdx = pathName.indexOf('^');
            if (caretIdx != -1 && caretIdx < pathName.length() - 1) {
                inputOverride = pathName.substring(caretIdx + 1);
            }

            for (IFlowNode next : res.nextNodes()) {
                if (next == null || res.value() == null) continue;

                String inputName = (inputOverride != null)
                        ? inputOverride
                        : findNextAvailableInput(next, res.value());

                if (inputName != null) {
                    receiveInput(next, inputName, res.value());
                }
            }
        }
    }

    private String findNextAvailableInput(IFlowNode node, DataValue<?> value) {
        if (node == null || value == null || value.getType() == null) return null;

        Map<String, DataType> expected = FlowNodeRegistry.getExpectedInputs(node.getClass());
        Map<String, DataValue<?>> current = inputBuffers.getOrDefault(node, Map.of());

        for (Map.Entry<String, DataType> entry : expected.entrySet()) {
            String inputName = entry.getKey();
            DataType expectedType = entry.getValue();

            if (current.containsKey(inputName)) continue;

            if (expectedType == value.getType() || TypeConverters.canConvert(value.getType(), expectedType)) {
                return inputName;
            }
        }

        return null;
    }
}
