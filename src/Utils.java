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
        return kind.equals("Plus") || kind.equals("Minus") || kind.equals("Mult") || kind.equals("Div"); //|| kind.equals("Smaller");
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
            default:
                t = type.getName();
                break;
        }
        return t;
    }

    /**
     * Returns a ollir string version of a type (int --> i32)
     * @param type
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

}