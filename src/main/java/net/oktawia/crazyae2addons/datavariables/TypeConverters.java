package net.oktawia.crazyae2addons.datavariables;

import java.util.Optional;

public final class TypeConverters {

    private TypeConverters() {}

    public static boolean canConvert(DataType from, DataType to) {
        if (from == null || to == null) return false;
        if (from == to) return true;

        return switch (from) {
            case STRING -> (to == DataType.INT || to == DataType.BOOL);
            case INT    -> (to == DataType.STRING || to == DataType.BOOL);
            case BOOL   -> (to == DataType.STRING || to == DataType.INT);
        };
    }

    public static Optional<DataValue<?>> convert(DataValue<?> input, DataType target) {
        if (input == null || target == null) return Optional.empty();
        if (input.getType() == target) return Optional.of(input);

        try {
            return switch (target) {
                case STRING -> toStringValue(input);
                case INT    -> toIntValue(input);
                case BOOL   -> toBoolValue(input);
            };
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    private static Optional<DataValue<?>> toStringValue(DataValue<?> v) {
        try {
            String s = String.valueOf(v.getRaw());
            return Optional.of(new StringValue(s));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<DataValue<?>> toIntValue(DataValue<?> v) {
        try {
            return switch (v.getType()) {
                case STRING -> {
                    String s = ((String) v.getRaw()).trim();
                    if (s.isEmpty()) {
                        yield Optional.empty();
                    }
                    s = s.replace('_', '\0').replace(",", ".");
                    try {
                        yield Optional.of(new IntValue(Integer.parseInt(s)));
                    } catch (NumberFormatException ignore) {
                        try {
                            double d = Double.parseDouble(s);
                            if (Double.isFinite(d)) {
                                yield Optional.of(new IntValue((int) d));
                            } else {
                                yield Optional.empty();
                            }
                        } catch (NumberFormatException ignore2) {
                            yield Optional.empty();
                        }
                    }
                }
                case INT -> Optional.of(new IntValue((Integer) v.getRaw()));
                case BOOL -> Optional.of(new IntValue(((Boolean) v.getRaw()) ? 1 : 0));
            };
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    private static Optional<DataValue<?>> toBoolValue(DataValue<?> v) {
        try {
            return switch (v.getType()) {
                case STRING -> {
                    Optional<DataValue<?>> asInt = toIntValue(v);
                    if (asInt.isEmpty()) yield Optional.empty();
                    int i = ((IntValue) asInt.get()).value();
                    yield Optional.of(new BoolValue(i > 0));
                }
                case INT -> {
                    int i = (Integer) v.getRaw();
                    yield Optional.of(new BoolValue(i > 0));
                }
                case BOOL -> Optional.of(new BoolValue((Boolean) v.getRaw()));
            };
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
