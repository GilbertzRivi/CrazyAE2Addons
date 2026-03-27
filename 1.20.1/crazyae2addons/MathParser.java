package net.oktawia.crazyae2addons;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathParser {
    public static double parse(String input) {
        input = normalize(input);
        double result = new Parser(input).parse();
        return round(result, 10);
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private static String normalize(String input) {
        input = input.toLowerCase().replaceAll("\\s+", "");

        input = input.replaceAll("([0-9.]+)k", "$1e3");
        input = input.replaceAll("([0-9.]+)m", "$1e6");
        input = input.replaceAll("([0-9.]+)g", "$1e9");
        input = input.replaceAll("([0-9.]+)t", "$1e12");

        return input;
    }

    private static class Parser {
        private final String input;
        private int pos = -1;
        private int ch;

        Parser(String input) {
            this.input = input;
            nextChar();
        }

        void nextChar() {
            ch = (++pos < input.length()) ? input.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        double parse() {
            double x = parseExpression();
            if (pos < input.length()) throw new RuntimeException("Unexpected: " + (char)ch);
            return x;
        }

        double parseExpression() {
            double x = parseTerm();
            while (true) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        double parseTerm() {
            double x = parseFactor();
            while (true) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else if (eat('%')) x %= parseFactor();
                else return x;
            }
        }

        double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = this.pos;

            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) throw new RuntimeException("Missing ')'");
            } else {
                while ((ch >= '0' && ch <= '9') || ch == '.' || ch == 'e') nextChar();
                String number = input.substring(startPos, this.pos);
                if (number.isEmpty()) throw new RuntimeException("Unexpected: " + (char)ch);
                x = Double.parseDouble(number);
            }

            return x;
        }
    }
}
