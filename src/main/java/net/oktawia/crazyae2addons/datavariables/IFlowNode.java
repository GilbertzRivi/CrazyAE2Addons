package net.oktawia.crazyae2addons.datavariables;

import java.util.List;
import java.util.Map;

public interface IFlowNode {
    Map<String, FlowResult> execute(String where, Map<String, DataValue<?>> inputs);
    void setOutputNodes(List<IFlowNode> outputs);
}

