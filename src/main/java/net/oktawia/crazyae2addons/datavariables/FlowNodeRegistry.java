package net.oktawia.crazyae2addons.datavariables;

import appeng.api.networking.IGrid;
import appeng.me.Grid;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.oktawia.crazyae2addons.datavariables.nodes.bool.*;
import net.oktawia.crazyae2addons.datavariables.nodes.integer.*;
import net.oktawia.crazyae2addons.datavariables.nodes.str.*;
import net.oktawia.crazyae2addons.datavariables.nodes.output.*;
import net.oktawia.crazyae2addons.entities.MEDataControllerBE;
import net.oktawia.crazyae2addons.parts.RedstoneEmitterPart;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class FlowNodeRegistry {

    public record FlowNodeMeta(
            Class<? extends IFlowNode> nodeClass,
            String desc,
            Map<String, String> args,
            Map<String, DataType> expectedInputs,
            int outputPaths
    ) {}

    private static final Map<String, FlowNodeMeta> labelMap = new HashMap<>();
    private static final Map<Class<? extends IFlowNode>, FlowNodeMeta> classMap = new HashMap<>();

    public static void register(
            String label,
            Class<? extends IFlowNode> clazz,
            String desc,
            Map<String, String> args,
            Map<String, DataType> expectedInputs,
            int outputPaths
    ) {
        FlowNodeMeta meta = new FlowNodeMeta(clazz, desc, args, expectedInputs, outputPaths);
        labelMap.put(label, meta);
        classMap.put(clazz, meta);
    }

    public static FlowNodeMeta getMeta(String label) {
        return labelMap.get(label);
    }

    public static Class<? extends IFlowNode> getNodeClass(String label) {
        var meta = labelMap.get(label);
        return meta != null ? meta.nodeClass() : null;
    }

    public static String getDescription(String label) {
        var meta = labelMap.get(label);
        return meta != null ? meta.desc() : null;
    }

    public static Map<String, String> getArgs(String label) {
        var meta = labelMap.get(label);
        return meta != null ? meta.args() : Map.of();
    }

    public static Map<String, DataType> getExpectedInputs(String label) {
        var meta = labelMap.get(label);
        return meta != null ? meta.expectedInputs() : Map.of();
    }

    public static int getOutputPaths(String label) {
        var meta = labelMap.get(label);
        return meta != null ? meta.outputPaths() : 0;
    }

    public static FlowNodeMeta getMeta(Class<? extends IFlowNode> clazz) {
        return classMap.get(clazz);
    }

    public static String getDescription(Class<? extends IFlowNode> clazz) {
        var meta = classMap.get(clazz);
        return meta != null ? meta.desc() : null;
    }

    public static Map<String, String> getArgs(Class<? extends IFlowNode> clazz) {
        var meta = classMap.get(clazz);
        return meta != null ? meta.args() : Map.of();
    }

    public static Map<String, DataType> getExpectedInputs(Class<? extends IFlowNode> clazz) {
        var meta = classMap.get(clazz);
        return meta != null ? meta.expectedInputs() : Map.of();
    }

    public static int getOutputPaths(Class<? extends IFlowNode> clazz) {
        var meta = classMap.get(clazz);
        return meta != null ? meta.outputPaths() : 0;
    }

    public static List<IFlowNode> deserializeNodesFromNBT(CompoundTag tag) {
        List<IFlowNode> result = new ArrayList<>();
        Map<IFlowNode, List<String>> nodeToOutputNames = new HashMap<>();
        Map<String, IFlowNode> nameToNode = new HashMap<>();
        Map<IFlowNode, String> nodeToLabel = new HashMap<>();

        ListTag list = tag.getList("Nodes", Tag.TAG_COMPOUND);

        for (Tag t : list) {
            if (!(t instanceof CompoundTag ct)) continue;

            String label = ct.getString("label");
            String name = ct.getString("name");
            String settingsStr = ct.getString("settings");
            List<String> settings = List.of(settingsStr.split("\\|", -1));

            Class<? extends IFlowNode> clazz = FlowNodeRegistry.getNodeClass(label);
            if (clazz == null) continue;

            Map<String, String> argDefs = FlowNodeRegistry.getArgs(clazz);
            List<String> argKeys = argDefs.keySet().stream()
                    .filter(k -> !k.equalsIgnoreCase("Next") && !k.equalsIgnoreCase("OnTrue") && !k.equalsIgnoreCase("OnFalse"))
                    .toList();

            try {
                List<String> argValues = new ArrayList<>();
                for (int i = 0; i < argKeys.size(); i++) {
                    if (i < settings.size()) {
                        argValues.add(settings.get(i));
                    }
                }

                IFlowNode node = null;

                if (clazz == IntConstNode.class && !argValues.isEmpty()) {
                    int val = Integer.parseInt(argValues.get(0));
                    node = new IntConstNode(val);
                } else if (clazz == BoolConstNode.class && !argValues.isEmpty()) {
                    boolean val = Boolean.parseBoolean(argValues.get(0));
                    node = new BoolConstNode(val);
                } else if (clazz == StringConstNode.class && !argValues.isEmpty()) {
                    String val = argValues.get(0);
                    node = new StringConstNode(val);
                }

                if (node == null) {
                    Constructor<?>[] constructors = clazz.getConstructors();

                    outer:
                    for (Constructor<?> ctor : constructors) {
                        Class<?>[] paramTypes = ctor.getParameterTypes();
                        int expectedArgs = paramTypes.length;

                        if (expectedArgs != argValues.size()) continue;

                        Object[] args = new Object[expectedArgs];
                        for (int i = 0; i < expectedArgs; i++) {
                            String val = argValues.get(i);
                            Class<?> type = paramTypes[i];

                            try {
                                if (type == int.class || type == Integer.class)
                                    args[i] = Integer.parseInt(val);
                                else if (type == boolean.class || type == Boolean.class)
                                    args[i] = Boolean.parseBoolean(val);
                                else if (type == String.class)
                                    args[i] = val;
                                else
                                    continue outer;
                            } catch (Exception e) {
                                continue outer;
                            }
                        }

                        node = (IFlowNode) ctor.newInstance(args);
                        break;
                    }
                }

                if (node == null) {
                    try {
                        node = clazz.getDeclaredConstructor().newInstance();
                    } catch (NoSuchMethodException ignored) {
                        continue;
                    }
                }

                result.add(node);
                nameToNode.put(name, node);
                nodeToLabel.put(node, label);
                nodeToOutputNames.put(node, settings.subList(argKeys.size(), settings.size()));

            } catch (Exception ignored) {
            }
        }

        for (IFlowNode node : result) {
            String label = nodeToLabel.get(node);
            if (label == null) continue;

            Class<? extends IFlowNode> clazz = FlowNodeRegistry.getNodeClass(label);
            if (clazz == null) continue;

            int outputCount = FlowNodeRegistry.getOutputPaths(clazz);
            List<String> outputNames = nodeToOutputNames.getOrDefault(node, List.of());
            outputNames = outputNames.stream().map(name -> name.split("\\^")[0]).toList();
            List<IFlowNode> outputs = new ArrayList<>();
            for (int i = 0; i < outputCount; i++) {
                if (i < outputNames.size()) {
                    IFlowNode target = nameToNode.get(outputNames.get(i));
                    outputs.add(target);
                } else {
                    outputs.add(null);
                }
            }

            node.setOutputNodes(outputs);
        }

        return result;
    }


    public static void init(){
        register("(B) AND",
                BoolAndNode.class,
                BoolAndNode.getDesc(),
                BoolAndNode.getArgs(),
                BoolAndNode.getExpectedInputs(),
                BoolAndNode.getOutputPaths()
        );

        register("(B) Literal value",
                BoolConstNode.class,
                BoolConstNode.getDesc(),
                BoolConstNode.getArgs(),
                BoolConstNode.getExpectedInputs(),
                BoolConstNode.getOutputPaths()
        );

        register("(B) ==",
                BoolEqualsNode.class,
                BoolEqualsNode.getDesc(),
                BoolEqualsNode.getArgs(),
                BoolEqualsNode.getExpectedInputs(),
                BoolEqualsNode.getOutputPaths()
        );

        register("(B) NOR",
                BoolNorNode.class,
                BoolNorNode.getDesc(),
                BoolNorNode.getArgs(),
                BoolNorNode.getExpectedInputs(),
                BoolNorNode.getOutputPaths()
        );

        register("(B) !=",
                BoolNotEqualsNode.class,
                BoolNotEqualsNode.getDesc(),
                BoolNotEqualsNode.getArgs(),
                BoolNotEqualsNode.getExpectedInputs(),
                BoolNotEqualsNode.getOutputPaths()
        );

        register("(B) NOT",
                BoolNotNode.class,
                BoolNotNode.getDesc(),
                BoolNotNode.getArgs(),
                BoolNotNode.getExpectedInputs(),
                BoolNotNode.getOutputPaths()
        );

        register("(B) OR",
                BoolOrNode.class,
                BoolOrNode.getDesc(),
                BoolOrNode.getArgs(),
                BoolOrNode.getExpectedInputs(),
                BoolOrNode.getOutputPaths()
        );

        register("(B) Sleep delay",
                BoolSleepNode.class,
                BoolSleepNode.getDesc(),
                BoolSleepNode.getArgs(),
                BoolSleepNode.getExpectedInputs(),
                BoolSleepNode.getOutputPaths()
        );

        register("(B) Int to Bool",
                IntToBoolNode.class,
                IntToBoolNode.getDesc(),
                IntToBoolNode.getArgs(),
                IntToBoolNode.getExpectedInputs(),
                IntToBoolNode.getOutputPaths()
        );

        register("(I) +",
                IntAddNode.class,
                IntAddNode.getDesc(),
                IntAddNode.getArgs(),
                IntAddNode.getExpectedInputs(),
                IntAddNode.getOutputPaths()
        );

        register("(I) Literal value",
                IntConstNode.class,
                IntConstNode.getDesc(),
                IntConstNode.getArgs(),
                IntConstNode.getExpectedInputs(),
                IntConstNode.getOutputPaths()
        );

        register("(I) /",
                IntDivideNode.class,
                IntDivideNode.getDesc(),
                IntDivideNode.getArgs(),
                IntDivideNode.getExpectedInputs(),
                IntDivideNode.getOutputPaths()
        );

        register("(I) ==",
                IntEqualsNode.class,
                IntEqualsNode.getDesc(),
                IntEqualsNode.getArgs(),
                IntEqualsNode.getExpectedInputs(),
                IntEqualsNode.getOutputPaths()
        );

        register("(I) >",
                IntGreaterThanNode.class,
                IntGreaterThanNode.getDesc(),
                IntGreaterThanNode.getArgs(),
                IntGreaterThanNode.getExpectedInputs(),
                IntGreaterThanNode.getOutputPaths()
        );

        register("(I) >=",
                IntGreaterThanOrEqualNode.class,
                IntGreaterThanOrEqualNode.getDesc(),
                IntGreaterThanOrEqualNode.getArgs(),
                IntGreaterThanOrEqualNode.getExpectedInputs(),
                IntGreaterThanOrEqualNode.getOutputPaths()
        );

        register("(I) <",
                IntLesserThanNode.class,
                IntLesserThanNode.getDesc(),
                IntLesserThanNode.getArgs(),
                IntLesserThanNode.getExpectedInputs(),
                IntLesserThanNode.getOutputPaths()
        );

        register("(I) <=",
                IntLesserThanOrEqualNode.class,
                IntLesserThanOrEqualNode.getDesc(),
                IntLesserThanOrEqualNode.getArgs(),
                IntLesserThanOrEqualNode.getExpectedInputs(),
                IntLesserThanOrEqualNode.getOutputPaths()
        );

        register("(I) Max",
                IntMaxNode.class,
                IntMaxNode.getDesc(),
                IntMaxNode.getArgs(),
                IntMaxNode.getExpectedInputs(),
                IntMaxNode.getOutputPaths()
        );

        register("(I) Min",
                IntMinNode.class,
                IntMinNode.getDesc(),
                IntMinNode.getArgs(),
                IntMinNode.getExpectedInputs(),
                IntMinNode.getOutputPaths()
        );

        register("(I) %",
                IntModuloNode.class,
                IntModuloNode.getDesc(),
                IntModuloNode.getArgs(),
                IntModuloNode.getExpectedInputs(),
                IntModuloNode.getOutputPaths()
        );

        register("(I) *",
                IntMultiplyNode.class,
                IntMultiplyNode.getDesc(),
                IntMultiplyNode.getArgs(),
                IntMultiplyNode.getExpectedInputs(),
                IntMultiplyNode.getOutputPaths()
        );

        register("(I) !=",
                IntNotEqualsNode.class,
                IntNotEqualsNode.getDesc(),
                IntNotEqualsNode.getArgs(),
                IntNotEqualsNode.getExpectedInputs(),
                IntNotEqualsNode.getOutputPaths()
        );

        register("(I) Sleep delay",
                IntSleepNode.class,
                IntSleepNode.getDesc(),
                IntSleepNode.getArgs(),
                IntSleepNode.getExpectedInputs(),
                IntSleepNode.getOutputPaths()
        );

        register("(I) -",
                IntSubtractNode.class,
                IntSubtractNode.getDesc(),
                IntSubtractNode.getArgs(),
                IntSubtractNode.getExpectedInputs(),
                IntSubtractNode.getOutputPaths()
        );

        register("(I) String to Int",
                StringToIntNode.class,
                StringToIntNode.getDesc(),
                StringToIntNode.getArgs(),
                StringToIntNode.getExpectedInputs(),
                StringToIntNode.getOutputPaths()
        );

        register("(O) Redstone emitter",
                SetRedstoneEmitterNode.class,
                SetRedstoneEmitterNode.getDesc(),
                SetRedstoneEmitterNode.getArgs(),
                SetRedstoneEmitterNode.getExpectedInputs(),
                SetRedstoneEmitterNode.getOutputPaths()
        );

        register("(O) Set variable",
                SetVariableNode.class,
                SetVariableNode.getDesc(),
                SetVariableNode.getArgs(),
                SetVariableNode.getExpectedInputs(),
                SetVariableNode.getOutputPaths()
        );

        register("(S) Bool to String",
                BoolToStringNode.class,
                BoolToStringNode.getDesc(),
                BoolToStringNode.getArgs(),
                BoolToStringNode.getExpectedInputs(),
                BoolToStringNode.getOutputPaths()
        );

        register("(S) Entrypoint",
                EntrypointNode.class,
                EntrypointNode.getDesc(),
                EntrypointNode.getArgs(),
                EntrypointNode.getExpectedInputs(),
                EntrypointNode.getOutputPaths()
        );

        register("(S) Int to String",
                IntToStringNode.class,
                IntToStringNode.getDesc(),
                IntToStringNode.getArgs(),
                IntToStringNode.getExpectedInputs(),
                IntToStringNode.getOutputPaths()
        );

        register("(S) Variable reader",
                ReadVariableNode.class,
                ReadVariableNode.getDesc(),
                ReadVariableNode.getArgs(),
                ReadVariableNode.getExpectedInputs(),
                ReadVariableNode.getOutputPaths()
        );

        register("(S) Concat",
                StringConcatNode.class,
                StringConcatNode.getDesc(),
                StringConcatNode.getArgs(),
                StringConcatNode.getExpectedInputs(),
                StringConcatNode.getOutputPaths()
        );

        register("(S) Const value",
                StringConstNode.class,
                StringConstNode.getDesc(),
                StringConstNode.getArgs(),
                StringConstNode.getExpectedInputs(),
                StringConstNode.getOutputPaths()
        );

        register("(S) Contains",
                StringContainsNode.class,
                StringContainsNode.getDesc(),
                StringContainsNode.getArgs(),
                StringContainsNode.getExpectedInputs(),
                StringContainsNode.getOutputPaths()
        );

        register("(S) EndsWith",
                StringEndsWithNode.class,
                StringEndsWithNode.getDesc(),
                StringEndsWithNode.getArgs(),
                StringEndsWithNode.getExpectedInputs(),
                StringEndsWithNode.getOutputPaths()
        );

        register("(S) Length",
                StringLengthNode.class,
                StringLengthNode.getDesc(),
                StringLengthNode.getArgs(),
                StringLengthNode.getExpectedInputs(),
                StringLengthNode.getOutputPaths()
        );

        register("(S) StartsWith",
                StringStartsWithNode.class,
                StringStartsWithNode.getDesc(),
                StringStartsWithNode.getArgs(),
                StringStartsWithNode.getExpectedInputs(),
                StringStartsWithNode.getOutputPaths()
        );

    }

}
