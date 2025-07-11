package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.datavariables.DataType;
import net.oktawia.crazyae2addons.datavariables.FlowNodeRegistry;
import net.oktawia.crazyae2addons.datavariables.IFlowNode;
import net.oktawia.crazyae2addons.menus.DataflowPatternMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.NodeWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DataflowPatternScreen<C extends DataflowPatternMenu> extends AEBaseScreen<C> {

    public List<String> nodes = new ArrayList<>();
    private AETextField searchBar;
    private List<String> filteredNodes = new ArrayList<>();
    private final int maxVisibleEntries = 12;
    private int scrollOffset = 0;
    private final int dropdownWidth = 121;
    private final int entryHeight = 12;
    private List<NodeWidget> nodeWidgets = new ArrayList<>();
    private AETextField activeLabelEditor = null;
    public NodeWidget editingNode = null;
    private final List<ConfigField> nodeConfigFields = new ArrayList<>();
    private int scrollOffsetY = 0;
    private final int columns = 4;
    private final int nodeWidth = 50;
    private final int nodeHeight = 60;
    private final int nodePaddingX = 10;
    private final int nodePaddingY = 10;
    private final int nodeSpacingX = 60;
    private final int nodeSpacingY = 70;
    private record ConfigField(AETextField field, int x, int y, int w, int h) {}


    String[] boolNodes = {
            "Literal value", "AND", "==", "!=",
            "NOT", "OR", "XOR", "Sleep delay", "Int to Bool"
    };
    String[] intNodes = {
            "Literal value", "+", "-", "*", "/",
            "%", "==", "!=",
            ">", ">=", "<",
            "<=", "Max", "Min", "Sleep delay", "String to Int"
    };
    String[] stringNodes = {
            "Bool to String", "Int to String", "Entrypoint", "Variable reader", "Const value",
            "Concat", "Replace", "Substring", "Contains",
            "StartsWith", "EndsWith", "Length", "To lower case", "To upper case", "Sleep delay"
    };
    String[] outputNodes = {
            "Redstone emitter", "Set variable"
    };
    private boolean initialized = false;
    private NodeWidget activeWidget;


    public DataflowPatternScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        for (String s : boolNodes) nodes.add("(B) " + s);
        for (String s : intNodes) nodes.add("(I) " + s);
        for (String s : stringNodes) nodes.add("(S) " + s);
        for (String s : outputNodes) nodes.add("(O) " + s);
        filteredNodes.addAll(nodes);
        setupGui();
    }

    @Override
    protected void init() {
        super.init();
        if (!this.initialized){
            searchBar.setValue("(S) Entrypoint");
            addNode();
            searchBar.setValue("");
            initialized = true;
            getMenu().sendData();
        }
    }

    public void setupGui() {
        searchBar = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        searchBar.setBordered(false);
        searchBar.setResponder(this::onSearchChanged);
        this.widgets.add("search", searchBar);
        IconButton addNodeBtn = new IconButton(Icon.ENTER, b -> this.addNode());
        addNodeBtn.setTooltip(Tooltip.create(Component.literal("Add selected node")));
        this.widgets.add("addnode", addNodeBtn);
    }

    private void onSearchChanged(String input) {
        filteredNodes = nodes.stream()
                .filter(s -> s.toLowerCase().contains(input.toLowerCase()))
                .toList();
        scrollOffset = 0;
    }

    private int rainbowColor(int index) {
        float hue = (index * 0.02f) % 1.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.8f, 0.8f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public void openEditorFor(NodeWidget widget) {
        closeEditor();

        this.editingNode = widget;
        this.activeLabelEditor = null;
        this.nodeConfigFields.clear();

        int editorX = this.leftPos + 65;
        int editorY = this.topPos + 50;

        this.activeLabelEditor = new AETextField(style, font, editorX, editorY, 120, 16);
        this.activeLabelEditor.setBordered(false);
        this.activeLabelEditor.setTooltip(Tooltip.create(Component.literal("Name of this node")));
        this.activeLabelEditor.setPlaceholder(Component.literal("Name"));
        this.activeLabelEditor.setValue(widget.name);
        this.activeWidget = widget;
        editorY += 24;

        Class<? extends IFlowNode> clazz = widget.nodeClass;

        Map<String, String> args = FlowNodeRegistry.getArgs(clazz);
        Map<String, DataType> expectedInputs = FlowNodeRegistry.getExpectedInputs(clazz);

        // Najpierw zwykłe argumenty
        for (Map.Entry<String, String> entry : args.entrySet()) {
            String key = entry.getKey();
            if (expectedInputs.containsKey(key)) continue;

            boolean isOutputRef = key.equalsIgnoreCase("Next") || key.equalsIgnoreCase("OnTrue") || key.equalsIgnoreCase("OnFalse");
            if (isOutputRef) continue;

            AETextField tf = new AETextField(style, font, editorX, editorY, 120, 16);
            tf.setBordered(false);
            tf.setPlaceholder(Component.literal(key));
            tf.setTooltip(Tooltip.create(Component.literal("Arg: " + entry.getValue())));

            nodeConfigFields.add(new ConfigField(tf, editorX, editorY, 120, 16));
            editorY += 22;
        }

        // Potem referencje do outputów (Next, OnTrue, OnFalse)
        for (Map.Entry<String, String> entry : args.entrySet()) {
            String key = entry.getKey();
            if (expectedInputs.containsKey(key)) continue;

            boolean isOutputRef = key.equalsIgnoreCase("Next") || key.equalsIgnoreCase("OnTrue") || key.equalsIgnoreCase("OnFalse");
            if (!isOutputRef) continue;

            AETextField tf = new AETextField(style, font, editorX, editorY, 120, 16);
            tf.setBordered(false);
            tf.setPlaceholder(Component.literal(key));
            tf.setTooltip(Tooltip.create(Component.literal("Node ID for: " + key)));

            nodeConfigFields.add(new ConfigField(tf, editorX, editorY, 120, 16));
            editorY += 22;
        }

        // Na końcu — tylko do podglądu expected inputs
        for (Map.Entry<String, DataType> inputEntry : expectedInputs.entrySet()) {
            String inputName = inputEntry.getKey();
            DataType inputType = inputEntry.getValue();

            AETextField tf = new AETextField(style, font, editorX, editorY, 120, 16);
            tf.setBordered(false);
            tf.setEditable(false);
            tf.setPlaceholder(Component.literal(inputName + " : " + inputType.name()));
            tf.setTooltip(Tooltip.create(Component.literal("Expected input")));

            nodeConfigFields.add(new ConfigField(tf, editorX, editorY, 120, 16));
            editorY += 20;
        }

        // Ustawienie wartości
        String[] values = widget.settings.split("\\|", -1);
        for (int i = 0; i < nodeConfigFields.size(); i++) {
            if (i < values.length) {
                nodeConfigFields.get(i).field().setValue(values[i]);
            } else {
                nodeConfigFields.get(i).field().setValue("");
            }
        }
    }


    public void closeEditor() {
        if (activeLabelEditor == null) return;
        this.activeWidget.name = this.activeLabelEditor.getValue();
        this.activeWidget.settings = this.nodeConfigFields.stream()
                .map(cf -> cf.field().getValue())
                .collect(Collectors.joining("|"));

        this.editingNode = null;
        this.activeLabelEditor = null;
        this.nodeConfigFields.clear();
        this.activeWidget = null;
        getMenu().saveData(serializeAllNodes());
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.fill(getGuiLeft() + 5, getGuiTop() + 20, getGuiLeft() + 246, getGuiTop() + 160, 0xFF000000);
        guiGraphics.fill(getGuiLeft() + 7, getGuiTop() + 22, getGuiLeft() + 244, getGuiTop() + 158, 0xFF666666);

        int clipX1 = getGuiLeft() + 7;
        int clipY1 = getGuiTop() + 22;
        int clipX2 = getGuiLeft() + 244;
        int clipY2 = getGuiTop() + 158;

        guiGraphics.enableScissor(clipX1, clipY1, clipX2, clipY2);
        renderNodeWidgets(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.disableScissor();


        if (searchBar.isFocused()) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();

            int x = searchBar.getX();
            int y = searchBar.getY() + 14;

            int shown = Math.min(maxVisibleEntries, filteredNodes.size() - scrollOffset);

            for (int i = 0; i < shown; i++) {
                int index = i + scrollOffset;
                if (index >= filteredNodes.size()) break;

                String entry = filteredNodes.get(index);
                int entryY = y + i * entryHeight;

                guiGraphics.fill(x, entryY, x + dropdownWidth, entryY + entryHeight, 0xFF202020);
                guiGraphics.drawString(font, entry, x + 4, entryY + 2, 0xFFFFFF);
            }

            pose.popPose();
        }

        if (editingNode != null) {

            int lx = activeLabelEditor.getX();
            int ly = activeLabelEditor.getY();
            int lw = activeLabelEditor.getWidth();
            int lh = activeLabelEditor.getHeight();
            guiGraphics.fill(lx - 10, ly - 10, lx + lw + 16, ly + lh + 8, 0xFF242424);
            activeLabelEditor.render(guiGraphics, mouseX, mouseY, partialTicks);

            for (ConfigField cf : nodeConfigFields) {
                guiGraphics.fill(cf.x() - 8, cf.y() - 6, cf.x() + cf.w() + 8, cf.y() + cf.h() + 6, 0xFF242424);
                cf.field().render(guiGraphics, mouseX, mouseY, partialTicks);
            }

        }
    }

    public java.awt.Rectangle getEditorBounds() {
        if (editingNode == null || activeLabelEditor == null) return null;

        int x1 = activeLabelEditor.getX() - 5;
        int y1 = activeLabelEditor.getY() - 10;
        int width = 120 + 10;
        int height = 24 + (nodeConfigFields.size() * 22) + 8;

        return new java.awt.Rectangle(x1, y1, width, height);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editingNode != null) {
            boolean insideEditor = false;
            java.awt.Rectangle editorBounds = getEditorBounds();
            if (editorBounds != null) {
                insideEditor = editorBounds.contains(mouseX, mouseY);
            }

            boolean clickedOnFields = false;

            if (activeLabelEditor != null && activeLabelEditor.mouseClicked(mouseX, mouseY, button)) {
                clickedOnFields = true;
                activeLabelEditor.setFocused(true);
            }

            for (ConfigField config : nodeConfigFields) {
                AETextField tf = config.field();
                if (tf.mouseClicked(mouseX, mouseY, button)) {
                    clickedOnFields = true;
                    tf.setFocused(true);
                }
            }

            if (!insideEditor && !clickedOnFields) {
                closeEditor();
                return true;
            }
        }

        if (searchBar.isFocused()) {
            int x = searchBar.getX();
            int y = searchBar.getY() + 14;

            for (int i = 0; i < Math.min(maxVisibleEntries, filteredNodes.size() - scrollOffset); i++) {
                int index = i + scrollOffset;
                int entryY = y + i * entryHeight;

                if (mouseX >= x && mouseX <= x + dropdownWidth && mouseY >= entryY && mouseY <= entryY + entryHeight) {
                    searchBar.setValue(filteredNodes.get(index));
                    return true;
                }
            }
        }

        searchBar.setFocused(false);
        if (activeLabelEditor != null) activeLabelEditor.setFocused(false);
        for (ConfigField config : nodeConfigFields) config.field().setFocused(false);

        if (searchBar.mouseClicked(mouseX, mouseY, button)) {
            searchBar.setFocused(true);
            return true;
        }

        if (button == 1 && mouseX >= searchBar.getX() && mouseX <= searchBar.getX() + searchBar.getWidth()
                && mouseY >= searchBar.getY() && mouseY <= searchBar.getY() + searchBar.getHeight()) {
            searchBar.setValue("");
            return true;
        }

        if (activeLabelEditor != null && activeLabelEditor.mouseClicked(mouseX, mouseY, button)) {
            activeLabelEditor.setFocused(true);
            return true;
        }

        for (ConfigField config : nodeConfigFields) {
            AETextField tf = config.field();
            if (tf.mouseClicked(mouseX, mouseY, button)) {
                tf.setFocused(true);
                return true;
            }
        }

        for (NodeWidget widget : nodeWidgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }



    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (searchBar.isFocused() && !filteredNodes.isEmpty()) {
            int maxOffset = Math.max(0, filteredNodes.size() - maxVisibleEntries);
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.signum(delta), maxOffset));
            return true;
        }

        scrollOffsetY = Math.max(0, scrollOffsetY + (int) ((delta * -1) * (nodeSpacingY / 7f)));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchBar.charTyped(chr, modifiers)) return true;

        if (editingNode != null && activeLabelEditor != null && activeLabelEditor.charTyped(chr, modifiers)) {
            return true;
        }

        if (editingNode != null) {
            for (ConfigField config : nodeConfigFields) {
                AETextField tf = config.field();
                if (tf.charTyped(chr, modifiers)) return true;
            }
        }

        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBar.keyPressed(keyCode, scanCode, modifiers)) return true;

        if (editingNode != null && activeLabelEditor != null && activeLabelEditor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (editingNode != null) {
            for (ConfigField config : nodeConfigFields) {
                AETextField tf = config.field();
                if (tf.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    private void renderNodeWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < nodeWidgets.size(); i++) {
            NodeWidget widget = nodeWidgets.get(i);

            int col = i % columns;
            int row = i / columns;

            int x = leftPos + nodePaddingX + col * nodeSpacingX;
            int y = topPos + nodePaddingY + row * nodeSpacingY - scrollOffsetY;

            widget.setX(x);
            widget.setY(y + 15);
            widget.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }

    public void addNode() {
        if (!nodes.contains(searchBar.getValue())) return;

        int index = nodeWidgets.size();
        int col = index % columns;
        int row = index / columns;

        int x = leftPos + nodePaddingX + col * nodeSpacingX;
        int y = topPos + nodePaddingY + row * nodeSpacingY;

        Class<? extends IFlowNode> clazz = FlowNodeRegistry.getNodeClass(searchBar.getValue());
        int color = rainbowColor(nodeWidgets.size());
        NodeWidget widget = new NodeWidget(x, y, nodeWidth, nodeHeight, searchBar.getValue(), clazz, color, index);
        this.nodeWidgets.add(widget);
    }

    public void removeNodeWidget(NodeWidget widget) {
        this.removeWidget(widget);
        nodeWidgets.remove(widget);

        if (widget == editingNode) {
            closeEditor();
        }
    }

    public CompoundTag serializeAllNodes() {
        ListTag list = new ListTag();

        for (NodeWidget node : nodeWidgets) {
            CompoundTag tag = new CompoundTag();
            tag.putString("label", node.label);
            tag.putString("name", node.name);
            tag.putString("settings", node.settings);
            tag.putInt("color", node.color);
            tag.putInt("index", node.index);
            tag.putInt("x", node.getX());
            tag.putInt("y", node.getY());
            list.add(tag);
        }

        CompoundTag result = new CompoundTag();
        result.put("Nodes", list);
        return result;
    }

    public void loadFromTag(CompoundTag tag){
        ListTag list = tag.getList("Nodes", Tag.TAG_COMPOUND);
        for (var t : list){
            if (t instanceof CompoundTag CT){
                var label = CT.getString("label");
                var name = CT.getString("name");
                var settings = CT.getString("settings");
                var color = CT.getInt("color");
                var index = CT.getInt("index");
                var x = CT.getInt("x");
                var y = CT.getInt("y");

                if (label.equals("(S) Entrypoint")){
                    var node = nodeWidgets.get(0);
                    node.name = name;
                    node.label = label;
                    node.settings = settings;
                    node.color = color;
                    node.index = index;
                    node.setX(x);
                    node.setY(y);
                } else {
                    Class<? extends IFlowNode> clazz = FlowNodeRegistry.getNodeClass(label);
                    NodeWidget widget = new NodeWidget(x, y, nodeWidth, nodeHeight, label, clazz, color, index);
                    widget.settings = settings;
                    widget.name = name;
                    this.nodeWidgets.add(widget);
                }
            }
        }
    }

}