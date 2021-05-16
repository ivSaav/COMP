import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.io.*;
import java.util.HashSet;
import java.util.Set;


public class Utils {
	
	public static InputStream toInputStream(String text) {
        try {
            return new ByteArrayInputStream(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * Finds the parent of a given node (class or method)
     * Determines if a given node is inside a class or method
     * @param node
     * @return
     */
    public static JmmNode findScope(JmmNode node) {
        if (node.getParent() == null) {
            return null;
        }

        if (node.getParent().getKind().equals("Class") || node.getParent().getKind().equals("Method")) {
            return node.getParent();
        }

        return findScope(node.getParent());
    }



    public static boolean isOperator(JmmNode node) {
        String kind = node.getKind();
        return kind.equals("Plus") || kind.equals("Minus") || kind.equals("Mult") || kind.equals("Div")
                || kind.equals("Smaller") || kind.equals("Negation") || kind.equals("And");
    }

    public static boolean isConditionalOperator(JmmNode node) {
        String kind = node.getKind();
        return kind.equals("Smaller") || kind.equals("Negation") || kind.equals("And");
    }

    public static JmmNode getChildOfKind(JmmNode node, String kind) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals(kind))
                return child;
        }
        return null;
    }

    /**
     * Returns a string version of a variable (int b --> b.i32)
     * @param symbol
     * @return
     */
    public static String getOllirVar(Symbol symbol) {
        Type type = symbol.getType();

        String name = replaceName(symbol);

        String t = getOllirType(type);
        return name + "." + (type.isArray() ? "array." : "") + t; 
    }

    public static String replaceName(Symbol symbol) {
        String name = symbol.getName();

        name = name.replace("$", "_S_"); // replacing every $ with _S_ because of argument variables

        if (name.equals("ret") || name.equals("array") || name.equals("field"))
            name = "_" + name + "_";

        return name;
    }


    /**
     * Returns a string version of a variable (int b --> b.i32)
     * Takes into consideration if the variable is an array and if it's being accessed
     * @param symbol - variable's symbol
     * @parma arrayAccess - if variable is an array that's being accessed
     * @return name.type (identifiers); name (array access); name.array.int (arrays)
     */
    public static String getOllirVar(Symbol symbol, boolean arrayAccess) {
        Type type = symbol.getType();
        String name = replaceName(symbol);

        String t = getOllirType(type);

        String ret = "";
        if (type.isArray()) { // A[]
            if (arrayAccess) // A[0] --> A
                ret += name;
            else
                ret = name + ".array."+ t; // A[] --> A.array.i32
        }
        else
            ret = name + "." + t;
        return ret;
    }

    /**
     * Returns a ollir string version of a type (int --> i32)
     * @param type
     * @return
     */
    public static String getOllirType(Type type) {
        String t = "";
        switch (type.getName()) {
            case "int":
                t = "i32";
                break;
            case "boolean":
                t = "bool";
                break;
            case "void":
                t = "V";
                break;
            default:
                t = type.getName();
                break;
        }
        return t;
    }

    /**
     * Returns a ollir string version of a type (int --> i32)
     * @param literalNode
     * @return
     */
    public static String getOllirLiteral(JmmNode literalNode) {
        String t = "";
        String value = literalNode.get("value");
        switch (literalNode.get("type")) {
            case "int":
                t = "i32";
                break;
            case "boolean":
                t = "bool";
                if (value.equals("true"))
                    value = "1";
                else
                    value = "0";

                break;
        }
        return value + "." + t;
    }

        /**
     * Returns a ollir string version of an operator (< --> <.i32)
     * @param operator
     * @return
     */
    public static String getOllirOp(String operator) {
        String op = "";
        switch (operator) {
            case "Smaller":
                op = "<.i32";
                break;
            case "And":
                op = "&&.bool";
                break;
            case "Negation":
                op = "!.bool";
                break;
            case "Plus":
                op = "+.i32";
                break;
            case "Minus":
                op = "-.i32";
                break;
            case "Mult":
                op = "*.i32";
                break;
            case "Div":
                op = "/.i32";
                break;
            default:
                op = operator;
                break;
        }
        return op;
    }

    public static String getOllirExpReturnType(String operator) {

        return switch (operator) {
            case "Smaller", "And", "Negation" -> ".bool";
            default -> ".i32";
        };
    }

    public static String reverseOperatorOllit(String op) {
        switch (op) {
            case "Smaller":
                return ">=.i32";
            case "And":
                return "||.bool";
            default:
                return "";
        }
    }

    public static void saveContents(String contents, String filename) {

        File file = new File("generated" + File.separator + filename);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(contents);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}