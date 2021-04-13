import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Counts the occurences of each node kind.
 *
 * @author JBispo
 *
 */
public class SymbolTableGenerator extends PreorderJmmVisitor<Void, Void> {

    private final SymbolsTable st;

    public SymbolTableGenerator() {

        addVisit("ImportDec", this::dealWithImport);
        addVisit("Class", this::dealWithClass);
        addVisit("VarDecl", this::dealWithVarDecl);
        addVisit("Method", this::dealWithMethod);

        setDefaultVisit(this::defaultVisit);
        this.st = new SymbolsTable();
    }


    /**
     * Deals with the node in case is type is an import
     * @param node
     * @param unused
     * @return void
     */
    public Void dealWithImport(JmmNode node, Void unused) {
        String importName = node.get("class");
        if (importName != null) {
            st.addImport(importName);
        }
        else {
            this.defaultVisit(node, null);
        }
        return null;
    }

    private Void dealWithClass(JmmNode node, Void unused) {
        String className = node.get("class");
        if (className != null) {
            st.setClassName(className);

            if (node.getNumChildren() > 0) {
                JmmNode ext = node.getChildren().get(0);
                JmmNode superIdent = ext.getChildren().get(0);

                st.setSuperclass(superIdent.get("name"));
            }
        }
        else {
            this.defaultVisit(node, null);
        }
        return null;
    }

    private Void dealWithVarDecl(JmmNode node, Void unused) {

        if (node.getParent().getKind().equals("Parameters"))
            return null;

        String varName = node.get("name");

        JmmNode child = node.getChildren().get(0);

        Type type = new Type(child.get("type"), Boolean.parseBoolean(child.get("isArray")));
        Symbol symbol = new Symbol(type, varName);
        if (varName != null) {
            JmmNode firstParent = Utils.findScope(node);

            this.st.addVariable(firstParent, varName, symbol);
        }

        this.defaultVisit(node, null);
        return null;
    }

    private Void dealWithMethod(JmmNode node, Void unused) {

        String methodName = node.get("name");

        // For the type
        if (methodName.equals("main")) {
            this.dealWithMainMethod(node, null);
            return null;
        }
        JmmNode childType = node.getChildren().get(0);

        Type methodType = new Type(childType.get("type"), Boolean.parseBoolean(childType.get("isArray")));

        MethodSymbols methodSymbols = new MethodSymbols(methodName, methodType);

        if (node.getNumChildren() >= 1) {
            JmmNode childParameters = node.getChildren().get(1);
            methodSymbols.addParameters(childParameters);
        }

        this.st.addMethod(methodSymbols);

        return null;
    }

    private Void dealWithMainMethod(JmmNode node, Void unused) {
        String methodName = "main";

        Type methodType = new Type("void", false);

        MethodSymbols methodSymbols = new MethodSymbols(methodName, methodType);

        JmmNode params = node.getChildren().get(0);
        JmmNode args = params.getChildren().get(0);

        Symbol argsSymb = new Symbol(new Type("String", true), args.get("name"));
        methodSymbols.addParameter(args.get("name"), argsSymb);

        this.st.addMethod(methodSymbols);

        return null;
    }


    private Void defaultVisit(JmmNode node, Void unused) {
        return null;
    }

    @Override
    public Void visit(JmmNode jmmNode) {
        SpecsCheck.checkNotNull(jmmNode, () -> "Node should not be null");

        var visit = getVisit(jmmNode.getKind());

        // Preorder: 1st visit the node
        var nodeResult = visit.apply(jmmNode, null);

        // Preorder: then, visit each children
        for (var child : jmmNode.getChildren()) {
            visit(child);
        }

        return null;
    }


    public SymbolsTable getSt() {
        return st;
    }
}
