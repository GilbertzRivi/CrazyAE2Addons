package net.oktawia.crazyae2addons.client.misc;

import com.lowdragmc.lowdraglib.gui.widget.codeeditor.language.ILanguageDefinition;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.language.LanguageDefinition;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.language.StyleManager;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.language.TokenType;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Set;

public final class CrazyLanguages {

    private CrazyLanguages() {}

    public static final int COL_GRAY = 0xFFAAAAAA;
    public static final int COL_GOLD = 0xFFFFC53D;
    public static final int COL_CYAN = 0xFF55FFFF;
    public static final int COL_RED  = 0xFFFF30BE;

    private static final int DEFAULT_COLOR    = 0xFFFFFFFF;
    private static final int MD_MARKER_COLOR  = 0xFFFFC800;
    private static final int MD_COMMAND_COLOR = 0xFF00FFC8;
    private static final int MD_INDENT_COLOR  = 0xFF888888;
    private static final int MD_TABLE_COLOR   = 0xFFB8B8B8;
    private static final int MD_CODE_COLOR    = 0xFFFFDD55;
    private static final int LABEL_COLOR      = 0xFF66CCFF;
    private static final int COUNT_COLOR      = 0xFFFFDD55;

    public static final TokenType MD_HEX_CMD = new TokenType("MdHexCommand")
            .setPattern("[&^][cb][0-9A-Fa-f]{6}");

    public static final TokenType MD_TYPED_TOKEN = new TokenType("MdTypedToken")
            .setPattern("&[sid]\\^(?:[a-z0-9_.-]+:)?[a-z0-9_.-]+:[a-z0-9_./-]+(?:%\\d+)?(?:@[a-zA-Z0-9]+)?");

    public static final TokenType MD_STYLE_CMD = new TokenType("MdStyleCommand")
            .setPattern("[&^][sid]\\b");

    public static final TokenType MD_INDENT = new TokenType("MdIndent")
            .setPattern(">>");

    public static final TokenType MD_HEADING = new TokenType("MdHeading")
            .setPattern("(?m)^(#{1,6})(?=\\s)");

    public static final TokenType MD_BLOCKQUOTE = new TokenType("MdBlockquote")
            .setPattern("(?m)^[ \\t]{0,3}(?:>[ \\t]?)+");

    public static final TokenType MD_LIST_MARKER = new TokenType("MdListMarker")
            .setPattern("(?m)^[ \\t]{0,3}(?:[*+-]|\\d+\\.)(?=\\s)");

    public static final TokenType MD_TABLE_PIPE = new TokenType("MdTablePipe")
            .setPattern("\\|");

    public static final TokenType MD_TABLE_SEPARATOR = new TokenType("MdTableSeparator")
            .setPattern("(?m):?-{3,}:?");

    public static final TokenType MD_CODE_FENCE = new TokenType("MdCodeFence")
            .setPattern("(?m)^```[A-Za-z0-9_-]*\\s*$");

    public static final TokenType MD_INLINE_CODE = new TokenType("MdInlineCode")
            .setPattern("`[^`\\r\\n]+`");

    public static final TokenType MD_STRONG = new TokenType("MdStrong")
            .setPattern("\\*\\*|__|~~");

    public static final TokenType MD_EM = new TokenType("MdEm")
            .setPattern("(?<!\\*)\\*(?!\\*)|(?<!_)_(?!_)");

    public static final TokenType MD_WS = new TokenType("MdWhitespace")
            .setPattern("[ \\t]+");

    public static final TokenType MD_NL = new TokenType("MdNewline")
            .setPattern("\\R");

    public static final TokenType MD_TEXT = new TokenType("MdText")
            .setPattern("[^\\r\\n&^`*_~|>#-]+");

    public static final TokenType MD_OTHER = new TokenType("MdOther")
            .setPattern(".");

    public static final ILanguageDefinition MARKDOWN = new LanguageDefinition(
            "crazy_markdown",
            List.of(
                    MD_HEX_CMD,
                    MD_TYPED_TOKEN,
                    MD_STYLE_CMD,
                    MD_INDENT,
                    MD_HEADING,
                    MD_BLOCKQUOTE,
                    MD_LIST_MARKER,
                    MD_CODE_FENCE,
                    MD_INLINE_CODE,
                    MD_TABLE_PIPE,
                    MD_TABLE_SEPARATOR,
                    MD_STRONG,
                    MD_EM,
                    MD_WS,
                    MD_NL,
                    MD_TEXT,
                    MD_OTHER
            ),
            Set.of()
    ).compileTokenPattern();

    public static final StyleManager MARKDOWN_STYLE = createMarkdownStyle();

    public static final TokenType STRING = new TokenType("ProgString")
            .setPattern("\"(?:\\\\.|[^\"\\\\])*\"");

    public static final TokenType COUNT = new TokenType("ProgCount")
            .setPattern("\\b\\d+(?=\\()");

    public static final TokenType COMMAND_WORD = new TokenType("ProgCommandWord")
            .setPattern("(?<![A-Za-z0-9_])(?:AND|OR|XOR|NAND)(?![A-Za-z0-9_])");

    public static final TokenType COMMAND = new TokenType("ProgCommand")
            .setPattern("[PZXFBUDRLHF]");

    public static final TokenType RESOURCE = new TokenType("ProgResource")
            .setPattern("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+");

    public static final TokenType PROP_KEY = new TokenType("ProgPropKey")
            .setPattern("(?<=[\\[,])[a-z_][a-z0-9_]*(?==)");

    public static final TokenType PROP_VALUE = new TokenType("ProgPropValue")
            .setPattern("(?<==)(?:true|false|[a-z_][a-z0-9_]*|-?\\d+(?:\\.\\d+)?)");

    public static final TokenType NUMBER = new TokenType("ProgNumber")
            .setPattern("-?\\d+(?:\\.\\d+)?");

    public static final TokenType OPERATOR = new TokenType("ProgOperator")
            .setPattern("==|!=|<=|>=|[:,=]");

    public static final TokenType PAREN = new TokenType("ProgParen")
            .setPattern("[()]");

    public static final TokenType BRACKET = new TokenType("ProgBracket")
            .setPattern("[\\[\\]]");

    public static final TokenType BRACE = new TokenType("ProgBrace")
            .setPattern("[{}]");

    public static final TokenType PIPE = new TokenType("ProgPipe")
            .setPattern("\\|+");

    public static final TokenType IDENT = new TokenType("ProgIdent")
            .setPattern("\\b[A-Za-z_][A-Za-z0-9_]*\\b");

    public static final TokenType WS = new TokenType("ProgWhitespace")
            .setPattern("\\s+");

    public static final TokenType OTHER = new TokenType("ProgOther")
            .setPattern(".");

    public static final ILanguageDefinition PROGRAM = new LanguageDefinition(
            "crazy_program",
            List.of(
                    STRING,
                    COUNT,
                    COMMAND_WORD,
                    COMMAND,
                    PROP_KEY,
                    PROP_VALUE,
                    RESOURCE,
                    NUMBER,
                    OPERATOR,
                    PAREN,
                    BRACKET,
                    BRACE,
                    PIPE,
                    IDENT,
                    WS,
                    OTHER
            ),
            Set.of("(", "[", "{")
    ).compileTokenPattern();

    public static final StyleManager PROGRAM_STYLE = createProgramStyle();

    private static StyleManager createMarkdownStyle() {
        StyleManager manager = new StyleManager();
        manager.getStyleMap().clear();
        manager.setDefaultStyle(Style.EMPTY.withColor(DEFAULT_COLOR));

        put(manager, MD_HEX_CMD, MD_COMMAND_COLOR);
        put(manager, MD_TYPED_TOKEN, MD_COMMAND_COLOR);
        put(manager, MD_STYLE_CMD, MD_COMMAND_COLOR);

        put(manager, MD_INDENT, MD_INDENT_COLOR);
        put(manager, MD_BLOCKQUOTE, MD_INDENT_COLOR);

        put(manager, MD_HEADING, MD_MARKER_COLOR);
        put(manager, MD_LIST_MARKER, MD_MARKER_COLOR);
        put(manager, MD_STRONG, MD_MARKER_COLOR);
        put(manager, MD_EM, MD_MARKER_COLOR);

        put(manager, MD_TABLE_PIPE, MD_TABLE_COLOR);
        put(manager, MD_TABLE_SEPARATOR, MD_TABLE_COLOR);

        put(manager, MD_CODE_FENCE, MD_CODE_COLOR);
        put(manager, MD_INLINE_CODE, MD_CODE_COLOR);

        return manager;
    }

    private static StyleManager createProgramStyle() {
        StyleManager manager = new StyleManager();
        manager.getStyleMap().clear();
        manager.setDefaultStyle(Style.EMPTY.withColor(DEFAULT_COLOR));

        put(manager, STRING, COL_GOLD);
        put(manager, COUNT, COUNT_COLOR);
        put(manager, COMMAND, COL_RED);
        put(manager, COMMAND_WORD, COL_RED);
        put(manager, RESOURCE, LABEL_COLOR);
        put(manager, PROP_KEY, 0xFF55FFAA);
        put(manager, PROP_VALUE, COL_CYAN);
        put(manager, NUMBER, COL_CYAN);
        put(manager, OPERATOR, COL_GRAY);
        put(manager, PIPE, COL_GRAY);
        put(manager, PAREN, 0xFF5599FF);
        put(manager, BRACKET, 0xFF55FF55);
        put(manager, BRACE, 0xFFFFD166);

        return manager;
    }

    private static void put(StyleManager manager, TokenType type, int color) {
        manager.getStyleMap().put(type.name, Style.EMPTY.withColor(color));
    }
}