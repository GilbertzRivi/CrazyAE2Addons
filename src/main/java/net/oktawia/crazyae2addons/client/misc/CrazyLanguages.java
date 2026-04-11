package net.oktawia.crazyae2addons.client.misc;

import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.ILanguageDefinition;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.LanguageDefinition;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.StyleManager;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.TokenType;
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

    public static final TokenType MD_LIST_MARKER = new TokenType("MdListMarker")
            .setPattern("(?:(?<=^)|(?<=\\s))[*-](?=\\s)");

    public static final TokenType MD_STRONG = new TokenType("MdStrong")
            .setPattern("\\*\\*|__|~~");

    public static final TokenType MD_EM = new TokenType("MdEm")
            .setPattern("(?<!\\*)\\*(?!\\*)");

    public static final TokenType MD_TEXT = new TokenType("MdText")
            .setPattern("[^&^*_~>\\-]+|.");

    public static final ILanguageDefinition MARKDOWN = new LanguageDefinition(
            "crazy_markdown",
            List.of(
                    MD_HEX_CMD,
                    MD_TYPED_TOKEN,
                    MD_STYLE_CMD,
                    MD_INDENT,
                    MD_LIST_MARKER,
                    MD_STRONG,
                    MD_EM,
                    MD_TEXT
            ),
            Set.of()
    ).compileTokenPattern();

    public static final StyleManager MARKDOWN_STYLE = new StyleManager() {
        @Override
        public Style getStyleForTokenType(TokenType type) {
            if (type == MD_HEX_CMD || type == MD_TYPED_TOKEN || type == MD_STYLE_CMD) {
                return Style.EMPTY.withColor(MD_COMMAND_COLOR);
            }
            if (type == MD_INDENT) {
                return Style.EMPTY.withColor(MD_INDENT_COLOR);
            }
            if (type == MD_LIST_MARKER || type == MD_STRONG || type == MD_EM) {
                return Style.EMPTY.withColor(MD_MARKER_COLOR);
            }
            return Style.EMPTY.withColor(DEFAULT_COLOR);
        }
    };

    public static final TokenType STRING = new TokenType("ProgString")
            .setPattern("\"(?:\\\\.|[^\"\\\\])*\"");

    public static final TokenType COUNT = new TokenType("ProgCount")
            .setPattern("\\b\\d+(?=\\()");

    public static final TokenType LABEL = new TokenType("ProgLabel")
            .setPattern("\\b[A-Za-z][A-Za-z0-9_]*:[A-Za-z0-9_]*\\b");

    public static final TokenType KEYWORD = new TokenType("ProgKeyword")
            .setPattern("\\b(?:AND|OR|XOR|NAND|P|Z|X|F|B|U|D|R|L|H)\\b");

    public static final TokenType NUMBER = new TokenType("ProgNumber")
            .setPattern("-?\\d+(?:\\.\\d+)?");

    public static final TokenType PUNCT = new TokenType("ProgPunct")
            .setPattern("[:,=]");

    public static final TokenType PAREN = new TokenType("ProgParen")
            .setPattern("[()]");

    public static final TokenType BRACKET = new TokenType("ProgBracket")
            .setPattern("[\\[\\]]");

    public static final TokenType BRACE = new TokenType("ProgBrace")
            .setPattern("[{}]");

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
                    LABEL,
                    KEYWORD,
                    NUMBER,
                    PUNCT,
                    PAREN,
                    BRACKET,
                    BRACE,
                    IDENT,
                    WS,
                    OTHER
            ),
            Set.of("(", "[", "{")
    ).compileTokenPattern();

    public static final StyleManager PROGRAM_STYLE = new StyleManager() {
        @Override
        public Style getStyleForTokenType(TokenType type) {
            if (type == STRING) {
                return Style.EMPTY.withColor(COL_GOLD);
            }
            if (type == COUNT) {
                return Style.EMPTY.withColor(COUNT_COLOR);
            }
            if (type == LABEL) {
                return Style.EMPTY.withColor(LABEL_COLOR);
            }
            if (type == KEYWORD) {
                return Style.EMPTY.withColor(COL_RED);
            }
            if (type == NUMBER) {
                return Style.EMPTY.withColor(COL_CYAN);
            }
            if (type == PUNCT) {
                return Style.EMPTY.withColor(COL_GRAY);
            }
            if (type == PAREN) {
                return Style.EMPTY.withColor(0xFF5599FF);
            }
            if (type == BRACKET) {
                return Style.EMPTY.withColor(0xFF55FF55);
            }
            if (type == BRACE) {
                return Style.EMPTY.withColor(0xFFFFD166);
            }
            return Style.EMPTY.withColor(DEFAULT_COLOR);
        }
    };
}