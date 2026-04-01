package net.oktawia.crazyae2addons.interfaces;

import net.oktawia.crazyae2addons.misc.HighlighterState;
import net.oktawia.crazyae2addons.misc.SyntaxHighlighter;

import java.util.List;

@FunctionalInterface
public interface IStatefulTokenizer {
    List<SyntaxHighlighter.Tok> tokenize(String line, int[] bracketDepths, HighlighterState state);
}
