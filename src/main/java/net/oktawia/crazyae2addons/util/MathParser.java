package net.oktawia.crazyae2addons.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MathParser {

    private MathParser() {
    }

    public static double parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return 0.0D;
        }

        double result = new Parser(normalized).parse();
        return round(result, 10);
    }

    public static boolean canParse(String input) {
        if (input == null) {
            return false;
        }

        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return false;
        }

        try {
            new Parser(normalized).parse();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isLiteralNumber(String input) {
        if (input == null) {
            return false;
        }

        String s = normalize(input);
        if (s.isEmpty()) {
            return false;
        }

        int len = s.length();
        int i = 0;

        if (s.charAt(i) == '+' || s.charAt(i) == '-') {
            i++;
            if (i >= len) {
                return false;
            }
        }

        boolean hasDigits = false;

        while (i < len && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            hasDigits = true;
            i++;
        }

        if (i < len && s.charAt(i) == '.') {
            i++;
            while (i < len && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                hasDigits = true;
                i++;
            }
        }

        if (!hasDigits) {
            return false;
        }

        if (i < len && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            i++;

            if (i < len && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                i++;
            }

            int expStart = i;
            while (i < len && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
            }

            if (expStart == i) {
                return false;
            }
        }

        if (i < len) {
            char suffix = s.charAt(i);
            if (suffix == 'k' || suffix == 'K'
                    || suffix == 'm' || suffix == 'M'
                    || suffix == 'g' || suffix == 'G'
                    || suffix == 't' || suffix == 'T') {
                i++;
            }
        }

        return i == len;
    }

    private static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException("places must be >= 0");
        }

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private static String normalize(String input) {
        return input
                .replace("_", "")
                .replace(" ", "")
                .trim();
    }

    private static final class Parser {
        private final String input;
        private int pos = -1;
        private int ch;

        private Parser(String input) {
            this.input = input;
            nextChar();
        }

        private void nextChar() {
            ch = (++pos < input.length()) ? input.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') {
                nextChar();
            }

            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        private double parse() {
            double x = parseExpression();
            if (pos < input.length()) {
                throw error("Unexpected character: '" + (char) ch + "'");
            }
            return x;
        }

        private double parseExpression() {
            double x = parseTerm();

            while (true) {
                if (eat('+')) {
                    x += parseTerm();
                } else if (eat('-')) {
                    x -= parseTerm();
                } else {
                    return x;
                }
            }
        }

        private double parseTerm() {
            double x = parseFactor();

            while (true) {
                if (eat('*')) {
                    x *= parseFactor();
                } else if (eat('/')) {
                    x /= parseFactor();
                } else if (eat('%')) {
                    x %= parseFactor();
                } else {
                    return x;
                }
            }
        }

        private double parseFactor() {
            if (eat('+')) {
                return parseFactor();
            }
            if (eat('-')) {
                return -parseFactor();
            }

            if (eat('(')) {
                double x = parseExpression();
                if (!eat(')')) {
                    throw error("Missing ')'");
                }
                return applySuffix(x);
            }

            double x = parseNumber();
            return applySuffix(x);
        }

        private double parseNumber() {
            int startPos = this.pos;
            boolean hasDigits = false;

            while (ch >= '0' && ch <= '9') {
                hasDigits = true;
                nextChar();
            }

            if (ch == '.') {
                nextChar();
                while (ch >= '0' && ch <= '9') {
                    hasDigits = true;
                    nextChar();
                }
            }

            if (!hasDigits) {
                throw error("Expected number");
            }

            if (ch == 'e' || ch == 'E') {
                int expPos = this.pos;
                nextChar();

                if (ch == '+' || ch == '-') {
                    nextChar();
                }

                boolean hasExpDigits = false;
                while (ch >= '0' && ch <= '9') {
                    hasExpDigits = true;
                    nextChar();
                }

                if (!hasExpDigits) {
                    throw error("Invalid exponent near position " + expPos);
                }
            }

            String number = input.substring(startPos, this.pos);

            try {
                return Double.parseDouble(number);
            } catch (NumberFormatException e) {
                throw error("Invalid number: " + number);
            }
        }

        private double applySuffix(double value) {
            if (ch == 'k' || ch == 'K') {
                nextChar();
                return value * 1_000D;
            }
            if (ch == 'm' || ch == 'M') {
                nextChar();
                return value * 1_000_000D;
            }
            if (ch == 'g' || ch == 'G') {
                nextChar();
                return value * 1_000_000_000D;
            }
            if (ch == 't' || ch == 'T') {
                nextChar();
                return value * 1_000_000_000_000D;
            }
            return value;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " in expression \"" + input + "\" at position " + pos);
        }
    }
}