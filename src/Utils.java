import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;


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
        String name = symbol.getName();

        String t = getOllirType(type);
        return name + "." + (type.isArray() ? "array." : "") + t; 
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
        String name = symbol.getName();

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
        switch (literalNode.get("type")) {
            case "int":
                t = "i32";
                break;
            case "boolean":
                t = "bool";
                break;
        }
        return literalNode.get("value") + "." + t;
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

        String op = "";
        switch (operator) {
            case "Smaller":
            case "And":
            case "Negation":
                op = ".bool";
                break;
            default:
                op = ".i32";
                break;
        }
        return op;
        
    }


}