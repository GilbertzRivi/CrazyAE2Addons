package net.oktawia.crazyae2addons.menus.item;

import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.builder.BuilderPatternHost;

public class BuilderPatternSubMenu extends AEBaseMenu implements ISubMenu {

    public static final String ACTION_GENERATE = "actiongenerate";
    public static final String ACTION_W = "actionwidth";
    public static final String ACTION_H = "actionheight";
    public static final String ACTION_D = "actiondepth";
    public static final String ACTION_R = "actionright";
    public static final String ACTION_U = "actionup";
    public static final String ACTION_F = "actionforward";
    public static final String ACTION_C = "actioncondition";
    public static final String ACTION_T = "actiontype";
    public static final String ACTION_PLACE_BLOCK = "actionplaceblock";
    public static final String ACTION_CHECK_BLOCK = "actioncheckblock";

    private final BuilderPatternHost host;

    @GuiSync(21)
    public int width = 1;
    @GuiSync(22)
    public int height = 1;
    @GuiSync(23)
    public int depth = 1;

    @GuiSync(11)
    public boolean right = true;
    @GuiSync(12)
    public boolean up = true;
    @GuiSync(13)
    public boolean forward = true;

    @GuiSync(1)
    public int condition = 0;
    @GuiSync(2)
    public int actionType = 0;

    @GuiSync(24)
    public String placeBlock = "minecraft:stone";

    @GuiSync(25)
    public String checkBlock = "minecraft:air";

    public BuilderPatternSubMenu(int id, Inventory playerInventory, BuilderPatternHost host) {
        super(CrazyMenuRegistrar.BUILDER_PATTERN_SUBMENU.get(), id, playerInventory, host);
        this.host = host;
        registerClientAction(ACTION_GENERATE, this::generate);
        registerClientAction(ACTION_W, Integer.class, this::setWidth);
        registerClientAction(ACTION_H, Integer.class, this::setHeight);
        registerClientAction(ACTION_D, Integer.class, this::setDepth);
        registerClientAction(ACTION_R, Boolean.class, this::setRight);
        registerClientAction(ACTION_U, Boolean.class, this::setUp);
        registerClientAction(ACTION_F, Boolean.class, this::setForward);
        registerClientAction(ACTION_C, Integer.class, this::setCondition);
        registerClientAction(ACTION_T, Integer.class, this::setActionType);
        registerClientAction(ACTION_PLACE_BLOCK, String.class, this::setPlaceBlock);
        registerClientAction(ACTION_CHECK_BLOCK, String.class, this::setCheckBlock);
    }

    @Override
    public BuilderPatternHost getHost() {
        return host;
    }

    public void setWidth(int width) {
        this.width = width;
        if (isClientSide()) {
            sendClientAction(ACTION_W, width);
        }
    }

    public void setHeight(int heigh) {
        this.height = heigh;
        if (isClientSide()) {
            sendClientAction(ACTION_H, heigh);
        }
    }

    public void setDepth(int depth) {
        this.depth = depth;
        if (isClientSide()) {
            sendClientAction(ACTION_D, depth);
        }
    }

    public void setRight(boolean right) {
        this.right = right;
        if (isClientSide()) {
            sendClientAction(ACTION_R, right);
        }
    }

    public void setUp(boolean up) {
        this.up = up;
        if (isClientSide()) {
            sendClientAction(ACTION_U, up);
        }
    }

    public void setForward(boolean forward) {
        this.forward = forward;
        if (isClientSide()) {
            sendClientAction(ACTION_F, forward);
        }
    }

    public void setActionType(int type) {
        this.actionType = type;
        if (isClientSide()) {
            sendClientAction(ACTION_T, type);
        }
    }

    public void setCondition(int condition) {
        this.condition = condition;
        if (isClientSide()) {
            sendClientAction(ACTION_C, condition);
        }
    }

    public void setPlaceBlock(String placeBlock) {
        this.placeBlock = placeBlock;
        if (isClientSide()) {
            sendClientAction(ACTION_PLACE_BLOCK, placeBlock);
        }
    }

    public void setCheckBlock(String checkBlock) {
        this.checkBlock = checkBlock;
        if (isClientSide()) {
            sendClientAction(ACTION_CHECK_BLOCK, checkBlock);
        }
    }

    public void generate() {
        if (isClientSide()) {
            sendClientAction(ACTION_GENERATE);
            return;
        }

        String instr;
        String blockMap;

        if (actionType == 0) {
            instr = switch (condition) {
                case 1  -> "P(0)==(1)";
                case 2  -> "P(0)!=(1)";
                default -> "P(0)";
            };
            blockMap = condition == 0
                    ? "0(" + placeBlock + ")"
                    : "0(" + placeBlock + "),1(" + checkBlock + ")";
        } else {
            instr = switch (condition) {
                case 1  -> "X==(1)";
                case 2  -> "X!=(1)";
                default -> "X";
            };
            blockMap = condition == 0
                    ? "0(minecraft:air)"
                    : "0(minecraft:air),1(" + checkBlock + ")";
        }

        String dirW  = right   ? "R" : "L";
        String antiW = right   ? "L" : "R";
        String dirH  = up      ? "U" : "D";
        String antiH = up      ? "D" : "U";
        String dirD  = forward ? "F" : "B";

        String row   = width + "{" + instr + dirW + "}" + width + "{" + antiW + "}";
        String slice = height + "{" + row + dirH + "}" + height + "{" + antiH + "}";

        String program;
        if (depth == 1 && height == 1) {
            program = "H " + width + "{" + instr + dirW + "}";
        } else if (depth == 1) {
            program = "H " + height + "{" + row + dirH + "}";
        } else {
            program = "H " + depth + "{" + slice + dirD + "}";
        }

        String result = blockMap + "\n||\n" + program;

        host.setProgram(result);
        host.returnToMainMenu(getPlayer(), this);
    }
}