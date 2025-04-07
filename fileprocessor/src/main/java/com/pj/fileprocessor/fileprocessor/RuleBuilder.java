package com.pj.fileprocessor.fileprocessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.util.*;

public class RuleBuilder {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode buildRule(String expression) {
        return parseExpression(expression.replace(",", "&"));
    }

    private static JsonNode parseExpression(String expr) {
        expr = expr.trim();
        if (!expr.contains("&") && !expr.contains("|") && !expr.contains("(")) {
            return buildMissingSome(expr);
        }

        if (expr.contains("(")) {
            return buildComplexRule(expr);
        }

        if (expr.contains("|") && expr.contains("&")) {
            List<String> orParts = splitAtTopLevel(expr, '|');
            ArrayNode ifArray = mapper.createArrayNode();
            for (String part : orParts) {
                ifArray.add(parseExpression(part));
            }
            ifArray.add("OK");
            ObjectNode ifObj = mapper.createObjectNode();
            ifObj.set("if", ifArray);
            return ifObj;
        }

        if (expr.contains("&")) {
            return buildMissing(expr.split("&"));
        }

        if (expr.contains("|")) {
            return buildMissingSome(expr);
        }

        return mapper.createObjectNode();
    }

    private static JsonNode buildComplexRule(String expr) {
        expr = expr.trim();

        // Strip outer parentheses if they wrap the whole expression
        if (expr.startsWith("(") && expr.endsWith(")") && isBalanced(expr.substring(1, expr.length() - 1))) {
            expr = expr.substring(1, expr.length() - 1).trim();
            return parseExpression(expr);
        }

        // OR logic at top level
        if (expr.contains("|")) {
            List<String> orParts = splitAtTopLevel(expr, '|');
            ArrayNode ifArray = mapper.createArrayNode();
            for (String part : orParts) {
                ifArray.add(parseExpression(part));
            }
            ifArray.add("OK");
            ObjectNode ifObj = mapper.createObjectNode();
            ifObj.set("if", ifArray);
            return ifObj;
        }

        // AND logic at top level
        if (expr.contains("&")) {
            List<String> andParts = splitAtTopLevel(expr, '&');
            return buildMissing(andParts.toArray(new String[0]));
        }

        // Fallback
        return parseExpression(expr);
    }

    private static boolean isBalanced(String expr) {
        int depth = 0;
        for (char c : expr.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (depth < 0) return false;
        }
        return depth == 0;
    }

    private static JsonNode buildMissing(String[] codes) {
        ObjectNode obj = mapper.createObjectNode();
        ArrayNode arr = mapper.createArrayNode();
        for (String code : codes) {
            arr.add(code.trim());
        }
        obj.set("missing", arr);
        return obj;
    }

    private static JsonNode buildMissingSome(String expr) {
        ObjectNode obj = mapper.createObjectNode();
        ArrayNode inner = mapper.createArrayNode();
        inner.add(1);
        ArrayNode codes = mapper.createArrayNode();
        for (String code : expr.split("\\|")) {
            codes.add(code.trim());
        }
        codes.add("dummy");
        inner.add(codes);
        obj.set("missing_some", inner);
        return obj;
    }

    private static List<String> splitAtTopLevel(String expr, char delimiter) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : expr.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (c == delimiter && depth == 0) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }
}
