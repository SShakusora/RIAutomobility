package com.sshakusora.riautomobility.model.bbmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

final class MolangExpression {
    private MolangExpression() {
    }

    static Expression compile(String source) {
        return new Parser(source).parse();
    }

    @FunctionalInterface
    interface Expression {
        double evaluate(ToDoubleFunction<String> variables);
    }

    private static final class Parser {
        private final String source;
        private int position;

        private Parser(String source) {
            this.source = source;
        }

        Expression parse() {
            Expression expression = conditional();
            whitespace();
            if (position != source.length()) error("Unexpected token");
            return expression;
        }

        private Expression conditional() {
            Expression condition = or();
            if (!take("?")) return condition;
            Expression whenTrue = conditional();
            require(":");
            Expression whenFalse = conditional();
            return variables -> truthy(condition.evaluate(variables))
                    ? whenTrue.evaluate(variables) : whenFalse.evaluate(variables);
        }

        private Expression or() {
            Expression result = and();
            while (take("||")) {
                Expression left = result;
                Expression right = and();
                result = variables -> bool(truthy(left.evaluate(variables)) || truthy(right.evaluate(variables)));
            }
            return result;
        }

        private Expression and() {
            Expression result = equality();
            while (take("&&")) {
                Expression left = result;
                Expression right = equality();
                result = variables -> bool(truthy(left.evaluate(variables)) && truthy(right.evaluate(variables)));
            }
            return result;
        }

        private Expression equality() {
            Expression result = comparison();
            while (true) {
                if (take("==")) {
                    Expression left = result;
                    Expression right = comparison();
                    result = variables -> bool(left.evaluate(variables) == right.evaluate(variables));
                } else if (take("!=")) {
                    Expression left = result;
                    Expression right = comparison();
                    result = variables -> bool(left.evaluate(variables) != right.evaluate(variables));
                } else return result;
            }
        }

        private Expression comparison() {
            Expression result = addition();
            while (true) {
                if (take("<=")) {
                    Expression left = result;
                    Expression right = addition();
                    result = variables -> bool(left.evaluate(variables) <= right.evaluate(variables));
                } else if (take(">=")) {
                    Expression left = result;
                    Expression right = addition();
                    result = variables -> bool(left.evaluate(variables) >= right.evaluate(variables));
                } else if (take("<")) {
                    Expression left = result;
                    Expression right = addition();
                    result = variables -> bool(left.evaluate(variables) < right.evaluate(variables));
                } else if (take(">")) {
                    Expression left = result;
                    Expression right = addition();
                    result = variables -> bool(left.evaluate(variables) > right.evaluate(variables));
                } else return result;
            }
        }

        private Expression addition() {
            Expression result = multiplication();
            while (true) {
                if (take("+")) {
                    Expression left = result;
                    Expression right = multiplication();
                    result = variables -> left.evaluate(variables) + right.evaluate(variables);
                } else if (take("-")) {
                    Expression left = result;
                    Expression right = multiplication();
                    result = variables -> left.evaluate(variables) - right.evaluate(variables);
                } else return result;
            }
        }

        private Expression multiplication() {
            Expression result = power();
            while (true) {
                if (take("*")) {
                    Expression left = result;
                    Expression right = power();
                    result = variables -> left.evaluate(variables) * right.evaluate(variables);
                } else if (take("/")) {
                    Expression left = result;
                    Expression right = power();
                    result = variables -> left.evaluate(variables) / right.evaluate(variables);
                } else if (take("%")) {
                    Expression left = result;
                    Expression right = power();
                    result = variables -> left.evaluate(variables) % right.evaluate(variables);
                } else return result;
            }
        }

        private Expression power() {
            Expression base = unary();
            if (!take("^")) return base;
            Expression exponent = power();
            return variables -> Math.pow(base.evaluate(variables), exponent.evaluate(variables));
        }

        private Expression unary() {
            if (take("+")) return unary();
            if (take("-")) {
                Expression value = unary();
                return variables -> -value.evaluate(variables);
            }
            if (take("!")) {
                Expression value = unary();
                return variables -> bool(!truthy(value.evaluate(variables)));
            }
            return primary();
        }

        private Expression primary() {
            if (take("(")) {
                Expression result = conditional();
                require(")");
                return result;
            }
            whitespace();
            if (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
                return number();
            }
            String identifier = identifier();
            if (identifier.isEmpty()) error("Expected a number, variable, or function");
            String normalized = identifier.toLowerCase(Locale.ROOT);
            if (take("(")) {
                List<Expression> arguments = new ArrayList<>();
                if (!take(")")) {
                    do arguments.add(conditional()); while (take(","));
                    require(")");
                }
                validateArity(normalized, arguments.size());
                return variables -> function(normalized, arguments, variables);
            }
            return switch (normalized) {
                case "true" -> variables -> 1.0D;
                case "false" -> variables -> 0.0D;
                case "math.pi" -> variables -> Math.PI;
                case "math.e" -> variables -> Math.E;
                default -> variables -> variables.applyAsDouble(normalized);
            };
        }

        private Expression number() {
            int start = position;
            while (position < source.length()) {
                char value = source.charAt(position);
                if (!Character.isDigit(value) && value != '.' && value != 'e' && value != 'E'
                        && (value != '+' && value != '-' || position == start
                        || source.charAt(position - 1) != 'e' && source.charAt(position - 1) != 'E')) break;
                position++;
            }
            double value;
            try {
                value = Double.parseDouble(source.substring(start, position));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid number at position " + start, exception);
            }
            return variables -> value;
        }

        private String identifier() {
            whitespace();
            int start = position;
            while (position < source.length()) {
                char value = source.charAt(position);
                if (!Character.isLetterOrDigit(value) && value != '_' && value != '.') break;
                position++;
            }
            return source.substring(start, position);
        }

        private boolean take(String token) {
            whitespace();
            if (!source.startsWith(token, position)) return false;
            position += token.length();
            return true;
        }

        private void require(String token) {
            if (!take(token)) error("Expected '" + token + "'");
        }

        private void whitespace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++;
        }

        private void error(String message) {
            throw new IllegalArgumentException(message + " at position " + position + " in '" + source + "'");
        }
    }

    private static void validateArity(String name, int count) {
        int minimum;
        int maximum;
        switch (name) {
            case "math.min", "math.max" -> { minimum = 1; maximum = Integer.MAX_VALUE; }
            case "math.clamp", "math.lerp" -> { minimum = 3; maximum = 3; }
            case "math.pow", "math.mod", "math.atan2" -> { minimum = 2; maximum = 2; }
            case "math.random" -> { minimum = 0; maximum = 2; }
            case "math.abs", "math.sin", "math.cos", "math.tan", "math.asin", "math.acos", "math.atan",
                    "math.sqrt", "math.floor", "math.ceil", "math.round", "math.trunc", "math.exp", "math.ln",
                    "math.sign" -> { minimum = 1; maximum = 1; }
            default -> throw new IllegalArgumentException("Unsupported Molang function '" + name + "'");
        }
        if (count < minimum || count > maximum) {
            throw new IllegalArgumentException("Molang function '" + name + "' received " + count + " arguments");
        }
    }

    private static double function(String name, List<Expression> arguments, ToDoubleFunction<String> variables) {
        return switch (name) {
            case "math.abs" -> Math.abs(argument(arguments, 0, variables));
            case "math.sin" -> Math.sin(Math.toRadians(argument(arguments, 0, variables)));
            case "math.cos" -> Math.cos(Math.toRadians(argument(arguments, 0, variables)));
            case "math.tan" -> Math.tan(Math.toRadians(argument(arguments, 0, variables)));
            case "math.asin" -> Math.toDegrees(Math.asin(argument(arguments, 0, variables)));
            case "math.acos" -> Math.toDegrees(Math.acos(argument(arguments, 0, variables)));
            case "math.atan" -> Math.toDegrees(Math.atan(argument(arguments, 0, variables)));
            case "math.atan2" -> Math.toDegrees(Math.atan2(argument(arguments, 0, variables), argument(arguments, 1, variables)));
            case "math.sqrt" -> Math.sqrt(argument(arguments, 0, variables));
            case "math.floor" -> Math.floor(argument(arguments, 0, variables));
            case "math.ceil" -> Math.ceil(argument(arguments, 0, variables));
            case "math.round" -> Math.round(argument(arguments, 0, variables));
            case "math.trunc" -> argument(arguments, 0, variables) < 0
                    ? Math.ceil(argument(arguments, 0, variables)) : Math.floor(argument(arguments, 0, variables));
            case "math.exp" -> Math.exp(argument(arguments, 0, variables));
            case "math.ln" -> Math.log(argument(arguments, 0, variables));
            case "math.sign" -> Math.signum(argument(arguments, 0, variables));
            case "math.pow" -> Math.pow(argument(arguments, 0, variables), argument(arguments, 1, variables));
            case "math.mod" -> argument(arguments, 0, variables) % argument(arguments, 1, variables);
            case "math.clamp" -> Math.max(argument(arguments, 1, variables),
                    Math.min(argument(arguments, 2, variables), argument(arguments, 0, variables)));
            case "math.lerp" -> {
                double start = argument(arguments, 0, variables);
                yield start + (argument(arguments, 1, variables) - start) * argument(arguments, 2, variables);
            }
            case "math.min" -> arguments.stream().mapToDouble(value -> value.evaluate(variables)).min().orElse(0.0D);
            case "math.max" -> arguments.stream().mapToDouble(value -> value.evaluate(variables)).max().orElse(0.0D);
            case "math.random" -> {
                double minimum = arguments.isEmpty() ? 0.0D : argument(arguments, 0, variables);
                double maximum = arguments.size() < 2 ? 1.0D : argument(arguments, 1, variables);
                yield minimum + ThreadLocalRandom.current().nextDouble() * (maximum - minimum);
            }
            default -> throw new IllegalArgumentException("Unsupported Molang function '" + name + "'");
        };
    }

    private static double argument(List<Expression> arguments, int index, ToDoubleFunction<String> variables) {
        return arguments.get(index).evaluate(variables);
    }

    private static boolean truthy(double value) {
        return value != 0.0D && !Double.isNaN(value);
    }

    private static double bool(boolean value) {
        return value ? 1.0D : 0.0D;
    }
}
