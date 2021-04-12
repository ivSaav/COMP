import pt.up.fe.comp.jmm.JmmNode;
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

        if (node.getParent().getKind().equals("Class") || node.getParent().getKind().equals("Method")
                || node.getParent().getKind().equals("MainMethod")) {
            return node.getParent();
        }

        return findScope(node.getParent());
    }

    public static Type determineType(JmmNode node) {

        switch (node.getKind()) {
            case "Ident":
                return new Type(node.get("name"), false);
            case "Int":
                return new Type("int", false);
            case "LiteralBool":
                return new Type("boolean", false);
        }
        return new Type("", false);
    }
	
}