package net.oktawia.crazyae2addonslite.interfaces;

import net.oktawia.crazyae2addonslite.misc.HighlighterState;
import net.oktawia.crazyae2addonslite.misc.SyntaxHighlighter;

import java.util.List;

@FunctionalInterface
public interface IStatefulTokenizer {
    List<SyntaxHighlighter.Tok> tokenize(String line, int[] bracketDepths, HighlighterState state);
}
