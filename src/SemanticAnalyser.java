import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class SemanticAnalyser extends AJmmVisitor<List<Report>, List<Report>> {

    private final SymbolsTable st;

    private final Set<String> visitedVariables;

    public SemanticAnalyser(SymbolsTable st) {
        this.st = st;
        this.visitedVariables = new HashSet<>();

        addVisit("VarDecl", this::dealWithVarDecl);
        addVisit("Plus", this::dealWithOperations);
        //addVisit("Equal", this::dealWithAssignment);
        addVisit("MethodCall", this::dealWithMethodCall);
//        addVisit("Method", this::dealWithMethod);
//        addVisit("MainMethod", this::dealWithMainMethod);

        setDefaultVisit(this::defaultVisit);

    }

    private List<Report> dealWithMethodCall(JmmNode node, List<Report> reports) {
        String methodName = node.get("name");

        // checking if it's a defined method in this class
        if (this.st.getMethod(methodName) == null) {
            JmmNode tmp = node.getChildren().get(0);

            // class identifier
            String nodeKind = tmp.getKind();
            if (nodeKind.equals("Ident")) {

                if (!st.getImports().contains(nodeKind))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Couldn't resolve method call " + methodName));
            }
        }

        // TODO verify if return type is valid
        return null;
    }

    private List<Report> dealWithAssignment(JmmNode node, List<Report> reports) {
        JmmNode lhs = node.getChildren().get(0);
        JmmNode rhs = node.getChildren().get(1);

        // Checking if variable as been declared before assignment
        System.out.println(lhs.get("name") + "===============================");
        if (!this.visitedVariables.contains(lhs.get("name"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable " + lhs.get("name")));
        }

//        Symbol lhsSymb = this.getVariableSymbol(lhs);
//        Type lhsType = lhsSymb.getType();
//        Type rhsType = Utils.determineType(rhs);
//        // Checking if lhs and rhs have the same type
//        if (!lhsType.equals(rhsType) && lhsType.isArray() || rhsType.isArray()) {
//            reports.add(
//                    new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Conflicting types in assignment " + lhsType.getName() + " and " + rhsType.getName())
//            );
//        }


        // TODO: operations ???
        return null;
    }

    /**
     *
     * @param node
     * @param reports
     * @return
     */
    private List<Report> dealWithOperations(JmmNode node, List<Report> reports) {
        JmmNode scope = Utils.findScope(node);

        // Verify if operation is made by operator with the same type
        List<JmmNode> opChildren = node.getChildren();

        List<Type> types = new ArrayList<>();

        if (scope.getKind().equals("Class")) {
            Map<String, Symbol> fields = st.getField();
            for (JmmNode children : opChildren) {
                Symbol symbol = fields.get(children.get("name"));
                types.add(symbol.getType());
            }
        }
        else if (scope.getKind().equals("Method")) {
            Map<String, Symbol> getVariables = st.getVariables(scope.get("name"));
            for (JmmNode children : opChildren) {
                Symbol symbol = getVariables.get(children.get("name"));
                types.add(symbol.getType());
            }
        }

        if (!verifySameTypes(types))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Variables with different types"));

        // Verify if it is used arrays directly for arithmetic operations


        return null;
    }

    private List<Report> dealWithVarDecl(JmmNode node, List<Report> reports) {
        String varName = node.get("name");

        // Verify if variable was already declared
        if (visitedVariables.contains(varName))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Variable " + varName + " already declared"));
        else
            visitedVariables.add(varName);


        // Verify if variable type exists
        JmmNode varChild = node.getChildren().get(0);
        String type = varChild.get("type");

        switch (type) {
            case "int":
                break;
            case"boolean":
                break;
            default:
                if (!verifyType(type))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Unrecognized type " + type));
        }

        return null;
    }

    /**
     * Verifies if the type exists
     * @param type name of type
     * @return
     */
    private boolean verifyType(String type) {
        return st.getImports().contains(type) || st.getClassName().equals(type) || st.getSuper().equals(type);
    }

    /**
     * Verifies if the type exists
     * @param types name of type
     * @return
     */
    private boolean verifySameTypes(List<Type> types) {
        Type defaultType = types.get(0);
        for (Type type : types) {
            if (!defaultType.equals(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fetches variable's symbol from local or global variables
     * @param var
     * @return
     */
    private Symbol getVariableSymbol(JmmNode var) {
        JmmNode scope = Utils.findScope(var);

        String name = var.get("name");

        if (scope.getKind().equals("Class")) {
            return st.getGlobalVariable(name);
        }

        return st.getMethod(scope.get("name")).getVariable(var.get("name"));
    }


    private List<Report> defaultVisit(JmmNode node, List<Report> reports) {
        return null;
    }

    @Override
    public List<Report> visit(JmmNode jmmNode, List<Report> reports) {
        SpecsCheck.checkNotNull(jmmNode, () -> "Node should not be null");

        var visit = getVisit(jmmNode.getKind());

        // Postorder: 1st visit each children
        for (var child : jmmNode.getChildren()) {
            visit(child, reports);
        }

        // Postorder: then, visit the node
        var nodeResult = visit.apply(jmmNode, reports);

        return reports;
    }

}
